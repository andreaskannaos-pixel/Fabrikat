package svenskacraft.ui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import svenskacraft.Penalty;
import svenskacraft.SvenskaCraft;
import svenskacraft.audio.AudioPlayer;
import svenskacraft.learning.AnswerCheck;
import svenskacraft.learning.Mastery;
import svenskacraft.learning.QuestionPlanner;
import svenskacraft.learning.QuestionPlanner.Kind;
import svenskacraft.learning.Scheduler;
import svenskacraft.learning.WordState;
import svenskacraft.progress.WordProgress;
import svenskacraft.settings.Settings;
import svenskacraft.vocab.Vocabulary.Entry;

/**
 * A quiz round (1–3 questions by intensity), or a practice round from the journal (no timer, no penalty).
 * Question kinds get harder as a word improves: multiple choice → en/ett → listening → typing.
 */
public class QuizScreen extends Screen {
	/** How a multiple-choice question is shown. */
	enum Type {
		TARGET_TO_EN,       // Swedish word shown, pick English
		EN_TO_TARGET,       // picture + English shown, pick Swedish
		PICTURE_TO_TARGET,  // picture only, pick Swedish
		TARGET_TO_PICTURE   // Swedish word shown, pick a picture
	}

	private static final int FEEDBACK_TICKS = 50;
	private static final int UNANSWERED = -2, TIMED_OUT = -1;

	private final List<Entry> questions;
	private final boolean practice;
	private final Screen parent;
	private final Random rnd = new Random();
	private int index = -1;
	private int correctCount;

	// current question
	private Entry answer;
	private Kind kind;
	private Type type;
	private List<Entry> options = List.of();
	private int correctIndex;
	private int chosen;
	private int ticks;
	private int nextAt;
	private boolean revealed;
	private int limitTicks;
	private String typed = "";
	private Component resultLine = Component.empty();
	private Component detailLine = Component.empty();
	private final Button[] buttons = new Button[4];
	private Button listen;
	private Button reveal;
	private EditBox input;

	public QuizScreen(List<Entry> questions) {
		this(questions, false, null);
	}

	public QuizScreen(List<Entry> questions, boolean practice, Screen parent) {
		super(Component.literal("Quiz"));
		this.questions = List.copyOf(questions);
		this.practice = practice;
		this.parent = parent;
		nextQuestion();
		AudioPlayer.cue(SoundEvents.NOTE_BLOCK_CHIME, 1.2f, 0.4f);
	}

	private Settings settings() { return SvenskaCraft.settings; }

	private void nextQuestion() {
		index++;
		answer = questions.get(index);
		WordProgress w = SvenskaCraft.progress.word(answer.id());
		kind = QuestionPlanner.plan(w, answer.word(), new QuestionPlanner.Options(SvenskaCraft.audio.voiceEnabled(), settings().typing), rnd);
		boolean never = settings().translations == Settings.Translations.NEVER;
		if (kind == Kind.LISTEN && never && answer.concept().abstractMeaning()) kind = Kind.CHOICE;
		type = switch (kind) {
			case LISTEN -> never ? Type.TARGET_TO_PICTURE : Type.TARGET_TO_EN;
			case CHOICE -> chooseType(answer);
			default -> null;
		};
		options = type == null ? List.of() : buildOptions(answer, type);
		correctIndex = switch (kind) {
			case GENDER -> AnswerCheck.article(AnswerCheck.normalize(answer.word())).equals("en") ? 0 : 1;
			case TYPE -> 0;
			default -> options.indexOf(answer);
		};
		chosen = UNANSWERED;
		ticks = 0;
		nextAt = -1;
		revealed = false;
		typed = "";
		limitTicks = practice ? Integer.MAX_VALUE : settings().answerSeconds * 20 * (kind == Kind.TYPE ? 2 : 1);
		resultLine = Component.empty();
		detailLine = Component.empty();
	}

	private Type chooseType(Entry e) {
		boolean coin = rnd.nextBoolean();
		return switch (settings().translations) {
			case ALWAYS -> coin ? Type.TARGET_TO_EN : Type.EN_TO_TARGET;
			case ON_DEMAND -> coin ? Type.TARGET_TO_EN : Type.PICTURE_TO_TARGET;
			case NEVER -> e.concept().abstractMeaning() || !coin ? Type.PICTURE_TO_TARGET : Type.TARGET_TO_PICTURE;
		};
	}

	private String key(Entry e, Type t) {
		return switch (t) {
			case TARGET_TO_EN -> e.en();
			case TARGET_TO_PICTURE -> e.icon() + "x" + e.iconCount();
			default -> e.word();
		};
	}

	private List<Entry> buildOptions(Entry ans, Type t) {
		List<Entry> out = new ArrayList<>(List.of(ans));
		List<Entry> same = new ArrayList<>(SvenskaCraft.vocab.inCategory(ans.category()));
		List<Entry> all = new ArrayList<>(SvenskaCraft.vocab.entries());
		Collections.shuffle(same, rnd);
		Collections.shuffle(all, rnd);
		for (List<Entry> pool : List.of(same, all)) {
			for (Entry e : pool) {
				if (out.size() == 4) break;
				if (t == Type.TARGET_TO_PICTURE && e.concept().abstractMeaning()) continue;
				String k = key(e, t);
				if (out.stream().noneMatch(o -> key(o, t).equals(k))) out.add(e);
			}
		}
		Collections.shuffle(out, rnd);
		return out;
	}

	/** The Swedish word is visible before answering (so the ♪ button may play it). */
	private boolean targetShown() {
		return (kind == Kind.CHOICE && (type == Type.TARGET_TO_EN || type == Type.TARGET_TO_PICTURE)) || kind == Kind.LISTEN;
	}

	private int top() { return Math.max(4, (height - 180) / 2); }

	@Override
	protected void init() {
		int cx = width / 2;
		int top = top();
		Arrays.fill(buttons, null);
		input = null;
		switch (kind) {
			case CHOICE, LISTEN -> {
				for (int i = 0; i < 4; i++) {
					final int idx = i;
					int x = (i % 2 == 0) ? cx - 152 : cx + 2;
					int y = top + 76 + (i / 2) * 24;
					buttons[i] = addRenderableWidget(Button.builder(buttonText(i), b -> choose(idx)).bounds(x, y, 150, 20).build());
				}
			}
			case GENDER -> {
				for (int i = 0; i < 2; i++) {
					final int idx = i;
					buttons[i] = addRenderableWidget(Button.builder(buttonText(i), b -> choose(idx))
						.bounds(i == 0 ? cx - 152 : cx + 2, top + 82, 150, 20).build());
				}
			}
			case TYPE -> {
				input = addRenderableWidget(new EditBox(font, cx - 100, top + 76, 200, 20, Component.literal("answer")));
				input.setMaxLength(40);
				input.setValue(typed);
				input.setResponder(s -> typed = s);
				input.setEditable(chosen == UNANSWERED);
				int x = cx - 100;
				for (String letter : new String[] {"å", "ä", "ö"}) {
					addRenderableWidget(Button.builder(Component.literal(letter), b -> {
						if (input != null && chosen == UNANSWERED) { input.insertText(letter); setFocused(input); }
					}).bounds(x, top + 100, 20, 20).build());
					x += 22;
				}
				buttons[0] = addRenderableWidget(Button.builder(Component.literal("Svara (answer)"), b -> submitTyped())
					.bounds(cx + 20, top + 100, 80, 20).build());
				if (chosen == UNANSWERED) setInitialFocus(input);
			}
		}
		listen = addRenderableWidget(Button.builder(Component.literal(kind == Kind.LISTEN && chosen == UNANSWERED ? "♪ Spela (play)" : "♪ Lyssna (listen)"),
			b -> SvenskaCraft.audio.play(SvenskaCraft.pack.wordSound(answer.id()), AudioPlayer.Priority.MANUAL)).bounds(cx - 102, top + 126, 100, 20).build());
		reveal = addRenderableWidget(Button.builder(Component.literal("Visa engelska"), b -> {
			revealed = true;
			b.visible = false;
			layoutSmallButtons();
		}).bounds(cx + 2, top + 126, 100, 20).tooltip(Tooltip.create(Component.literal("Show English"))).build());
		boolean pictureOnly = (kind == Kind.CHOICE && type == Type.PICTURE_TO_TARGET) || kind == Kind.TYPE || kind == Kind.GENDER;
		reveal.visible = settings().translations == Settings.Translations.ON_DEMAND && pictureOnly && !revealed && chosen == UNANSWERED;
		updateListen();
	}

	private void updateListen() {
		listen.visible = SvenskaCraft.audio.voiceEnabled() && (targetShown() || chosen != UNANSWERED);
		layoutSmallButtons();
	}

	/** Listen / show-English sit side by side, or centred when only one is visible. */
	private void layoutSmallButtons() {
		int cx = width / 2;
		if (listen.visible && reveal.visible) {
			listen.setX(cx - 102);
			reveal.setX(cx + 2);
		} else {
			listen.setX(cx - 50);
			reveal.setX(cx - 50);
		}
	}

	private Component buttonText(int i) {
		if (kind == Kind.GENDER) {
			String a = i == 0 ? "en" : "ett";
			if (chosen == UNANSWERED) return Component.literal(a);
			if (i == correctIndex) return Component.literal("✔ " + a).withStyle(ChatFormatting.GREEN);
			return Component.literal((i == chosen ? "✘ " : "") + a).withStyle(i == chosen ? ChatFormatting.RED : ChatFormatting.DARK_GRAY);
		}
		if (type == Type.TARGET_TO_PICTURE) {
			if (chosen == UNANSWERED) return Component.empty();
			if (i == correctIndex) return Component.literal("✔").withStyle(ChatFormatting.GREEN);
			if (i == chosen) return Component.literal("✘").withStyle(ChatFormatting.RED);
			return Component.empty();
		}
		Entry e = options.get(i);
		String text = type == Type.TARGET_TO_EN ? e.en() : e.word();
		if (chosen == UNANSWERED) return Component.literal(text);
		if (i == correctIndex) return Component.literal("✔ " + text).withStyle(ChatFormatting.GREEN);
		if (i == chosen) return Component.literal("✘ " + text).withStyle(ChatFormatting.RED);
		return Component.literal(text).withStyle(ChatFormatting.DARK_GRAY);
	}

	private void choose(int idx) {
		if (chosen != UNANSWERED) return;
		chosen = idx;
		finish(idx == correctIndex, Component.empty());
	}

	private void submitTyped() {
		if (chosen != UNANSWERED || kind != Kind.TYPE) return;
		if (AnswerCheck.normalize(typed).isEmpty()) return;
		chosen = 0;
		AnswerCheck.Result r = AnswerCheck.check(typed, answer.word());
		Component hint = r.verdict() == AnswerCheck.Verdict.ALMOST
			? Component.literal(r.hint()).withStyle(ChatFormatting.YELLOW)
			: Component.empty();
		if (!r.accepted()) hint = Component.literal("You wrote: " + typed).withStyle(ChatFormatting.GRAY);
		finish(r.accepted(), hint);
	}

	private void finish(boolean correct, Component hint) {
		if (minecraft == null) return;
		long now = System.currentTimeMillis();
		var progress = SvenskaCraft.progress;
		WordProgress w = progress.wordOrCreate(answer.id());
		WordState before = Mastery.state(w);
		Mastery.recordAnswer(w, correct, now);
		Scheduler.remember(progress, answer.id());
		WordState after = Mastery.state(w);

		boolean showEn = settings().translations != Settings.Translations.NEVER;
		MutableComponent pair = Component.literal(answer.word()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		if (showEn) pair.append(Component.literal(" = " + answer.en()).withStyle(ChatFormatting.GRAY));

		detailLine = hint;
		if (correct) {
			correctCount++;
			progress.points += kind == Kind.TYPE ? 2 : 1;
			progress.streak++;
			progress.bestStreak = Math.max(progress.bestStreak, progress.streak);
			SvenskaCraft.audio.react("correct");
			AudioPlayer.cue(SoundEvents.NOTE_BLOCK_PLING, 1.6f, 0.35f);
			resultLine = Component.literal(SvenskaCraft.pack.ui("correct") + "  ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD).append(pair);
		} else {
			progress.streak = 0;
			SvenskaCraft.audio.react(chosen == TIMED_OUT ? "timeout" : "wrong");
			AudioPlayer.cue(SoundEvents.NOTE_BLOCK_BASS, 0.7f, 0.5f);
			String head = chosen == TIMED_OUT ? SvenskaCraft.pack.ui("timeout") : SvenskaCraft.pack.ui("wrong");
			resultLine = Component.literal(head + "  ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD).append(pair);
			if (!practice) {
				Component penalty = Penalty.apply(minecraft, settings().consequence);
				if (!penalty.getString().isEmpty()) {
					SvenskaCraft.say(penalty);
					detailLine = penalty;
				}
			}
		}
		if (after.ordinal() > before.ordinal() && after != WordState.LEARNING) {
			detailLine = Component.literal("↑ " + answer.word() + ": " + Journal.stateName(after)).withStyle(Journal.stateColor(after));
		}
		SvenskaCraft.answered();
		SvenskaCraft.progressChanged();
		for (int i = 0; i < 4; i++) if (buttons[i] != null && kind != Kind.TYPE) buttons[i].setMessage(buttonText(i));
		if (input != null) input.setEditable(false);
		reveal.visible = false;
		listen.setMessage(Component.literal("♪ Lyssna (listen)"));
		updateListen();
		nextAt = ticks + FEEDBACK_TICKS + (detailLine.getString().length() > 30 ? 20 : 0);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (kind == Kind.TYPE && chosen == UNANSWERED
			&& (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER)) {
			submitTyped();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void tick() {
		ticks++;
		if (kind == Kind.LISTEN && ticks == 10 && chosen == UNANSWERED && settings().voice == Settings.VoiceMode.DISCOVERY) {
			SvenskaCraft.audio.play(SvenskaCraft.pack.wordSound(answer.id()), AudioPlayer.Priority.MANUAL);
		}
		if (chosen == UNANSWERED && ticks >= limitTicks) {
			chosen = TIMED_OUT;
			finish(false, Component.empty());
		}
		if (nextAt >= 0 && ticks >= nextAt) {
			if (index + 1 < questions.size()) {
				nextQuestion();
				rebuildWidgets();
			} else {
				SvenskaCraft.say(Component.literal((practice ? "Practice: " : "Quiz: ") + correctCount + "/" + questions.size() + " rätt (correct)")
					.withStyle(correctCount == questions.size() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
				onClose();
			}
		}
	}

	@Override
	public void onClose() {
		// Practice returns to the journal (Esc is allowed there); a real quiz just closes.
		if (practice) minecraft.gui.setScreen(parent);
		else super.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		int cx = width / 2;
		int top = top();
		String counter = questions.size() > 1 ? "   " + (index + 1) + "/" + questions.size() : "";
		String title = practice ? "ÖVNING · Practice" : SvenskaCraft.pack.ui("quizTitle");
		g.centeredText(font, Component.literal("★ " + title + " ★" + counter).withStyle(ChatFormatting.BOLD), cx, top, 0xFFFFCC00);

		boolean showEn = settings().translations == Settings.Translations.ALWAYS || revealed;
		boolean answered = chosen != UNANSWERED;
		switch (kind) {
			case CHOICE -> drawChoicePrompt(g, cx, top, showEn);
			case LISTEN -> {
				g.centeredText(font, Component.literal(SvenskaCraft.pack.ui("listenAndChoose")), cx, top + 16, 0xFFFFFFFF);
				bigText(g, answered ? answer.word() : "♪ ? ♪", cx, top + 30, answered ? 0xFFFFAA00 : 0xFF88CCFF);
			}
			case GENDER -> {
				drawIcon(g, answer, cx - 16, top + 12, 2.0f);
				String noun = answer.word().substring(answer.word().indexOf(' ') + 1);
				bigText(g, (answered ? answer.word().substring(0, answer.word().indexOf(' ')) : "___") + " " + noun, cx, top + 46, 0xFFFFAA00);
				g.centeredText(font, Component.literal(SvenskaCraft.pack.ui("enOrEtt") + (showEn ? "  (" + answer.en() + ")" : "")), cx, top + 66, 0xFFCCCCCC);
			}
			case TYPE -> {
				drawIcon(g, answer, cx - 16, top + 12, 2.0f);
				String q = SvenskaCraft.pack.ui("typeInSwedish") + (showEn ? "  \"" + answer.en() + "\"" : "");
				g.centeredText(font, Component.literal(q), cx, top + 50, 0xFFFFFFFF);
				if (answered) g.centeredText(font, Component.literal("→ " + answer.word()).withStyle(ChatFormatting.BOLD), cx, top + 62, 0xFFFFAA00);
			}
		}

		if (!practice) {
			int barW = 304, x0 = cx - barW / 2;
			int y0 = switch (kind) { case GENDER -> top + 76; case TYPE -> top + 71; default -> top + 66; };
			float left = chosen == UNANSWERED ? Math.max(0f, 1f - (ticks + partialTick) / limitTicks) : 0f;
			g.fill(x0, y0, x0 + barW, y0 + 4, 0xFF333333);
			g.fill(x0, y0, x0 + (int) (barW * left), y0 + 4, left > 0.5f ? 0xFF55FF55 : left > 0.2f ? 0xFFFFDD33 : 0xFFFF5555);
		}

		if (type == Type.TARGET_TO_PICTURE && (kind == Kind.CHOICE || kind == Kind.LISTEN)) {
			for (int i = 0; i < 4; i++) {
				Button b = buttons[i];
				drawIcon(g, options.get(i), b.getX() + b.getWidth() / 2 - 8, b.getY() + 2, 1.0f);
			}
		}

		int infoY = top + 152;
		if (!answered) {
			Settings.Consequence c = settings().consequence;
			if (practice) {
				g.centeredText(font, Component.literal("Practice – no timer, no penalty. Esc to stop."), cx, infoY, 0xFF88CC88);
			} else if (c != Settings.Consequence.NONE) {
				String what = c == Settings.Consequence.SMALL ? "1 item" : "a whole stack";
				String shield = SvenskaCraft.progress.shields > 0 ? "  [shield: " + SvenskaCraft.progress.shields + "]" : "";
				g.centeredText(font, Component.literal("Wrong answer = you lose " + what + shield), cx, infoY, 0xFFFF7777);
			}
		} else {
			g.centeredText(font, resultLine, cx, infoY, 0xFFFFFFFF);
			g.centeredText(font, detailLine, cx, infoY + 13, 0xFFFFFFFF);
		}
	}

	private void drawChoicePrompt(GuiGraphicsExtractor g, int cx, int top, boolean showEn) {
		switch (type) {
			case TARGET_TO_EN, TARGET_TO_PICTURE -> {
				String q = type == Type.TARGET_TO_EN ? SvenskaCraft.pack.ui("whatDoesItMean") : SvenskaCraft.pack.ui("whichPicture");
				g.centeredText(font, Component.literal(q), cx, top + 16, 0xFFFFFFFF);
				bigText(g, answer.word(), cx, top + 30, 0xFFFFAA00);
			}
			case EN_TO_TARGET, PICTURE_TO_TARGET -> {
				drawIcon(g, answer, cx - 16, top + 14, 2.0f);
				String q = type == Type.EN_TO_TARGET
					? String.format(SvenskaCraft.pack.ui("howDoYouSay"), answer.en())
					: SvenskaCraft.pack.ui("whatIsThis");
				g.centeredText(font, Component.literal(q), cx, top + 50, 0xFFFFFFFF);
				if (type == Type.PICTURE_TO_TARGET && showEn) g.centeredText(font, Component.literal("(" + answer.en() + ")"), cx, top + 60, 0xFFAAAAAA);
			}
		}
	}

	private void bigText(GuiGraphicsExtractor g, String text, int cx, int y, int color) {
		g.pose().pushMatrix();
		g.pose().translate(cx, y);
		g.pose().scale(2.0f, 2.0f);
		g.centeredText(font, Component.literal(text).withStyle(ChatFormatting.BOLD), 0, 0, color);
		g.pose().popMatrix();
	}

	static void drawIcon(GuiGraphicsExtractor g, Entry e, int x, int y, float scale) {
		ItemStack stack = Journal.icon(e);
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.item(stack, 0, 0);
		if (stack.getCount() > 1) g.itemDecorations(Journal.font(), stack, 0, 0);
		g.pose().popMatrix();
	}

	@Override
	public boolean shouldCloseOnEsc() { return practice; }

	@Override
	public boolean isPauseScreen() { return false; }
}
