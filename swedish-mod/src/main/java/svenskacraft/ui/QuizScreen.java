package svenskacraft.ui;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import svenskacraft.Penalty;
import svenskacraft.SvenskaCraft;
import svenskacraft.audio.AudioPlayer;
import svenskacraft.learning.Mastery;
import svenskacraft.learning.Scheduler;
import svenskacraft.learning.WordState;
import svenskacraft.progress.WordProgress;
import svenskacraft.settings.Settings;
import svenskacraft.vocab.Vocabulary.Entry;

/** A short quiz round (1–3 questions depending on intensity). */
public class QuizScreen extends Screen {
	enum Type {
		TARGET_TO_EN,       // Swedish word shown, pick English
		EN_TO_TARGET,       // picture + English shown, pick Swedish
		PICTURE_TO_TARGET,  // picture only, pick Swedish
		TARGET_TO_PICTURE   // Swedish word shown, pick a picture
	}

	private static final int FEEDBACK_TICKS = 50;

	private final List<Entry> questions;
	private final Random rnd = new Random();
	private int index = -1;
	private int correctCount;

	// current question
	private Entry answer;
	private Type type;
	private List<Entry> options;
	private int correctIndex;
	private int chosen;
	private int ticks;
	private int nextAt;
	private boolean revealed;
	private int limitTicks;
	private Component resultLine = Component.empty();
	private Component detailLine = Component.empty();
	private final Button[] buttons = new Button[4];
	private Button listen;
	private Button reveal;

	public QuizScreen(List<Entry> questions) {
		super(Component.literal("Quiz"));
		this.questions = List.copyOf(questions);
		nextQuestion();
		AudioPlayer.cue(SoundEvents.NOTE_BLOCK_CHIME, 1.2f, 0.4f);
	}

	private Settings settings() { return SvenskaCraft.settings; }

	private void nextQuestion() {
		index++;
		answer = questions.get(index);
		type = chooseType(answer);
		options = buildOptions(answer, type);
		correctIndex = options.indexOf(answer);
		chosen = -2;
		ticks = 0;
		nextAt = -1;
		revealed = false;
		limitTicks = settings().answerSeconds * 20;
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

	private boolean targetShown() { return type == Type.TARGET_TO_EN || type == Type.TARGET_TO_PICTURE; }

	private int top() { return Math.max(4, (height - 180) / 2); }

	@Override
	protected void init() {
		int cx = width / 2;
		int top = top();
		for (int i = 0; i < 4; i++) {
			final int idx = i;
			int x = (i % 2 == 0) ? cx - 152 : cx + 2;
			int y = top + 76 + (i / 2) * 24;
			buttons[i] = addRenderableWidget(Button.builder(buttonText(i), b -> answer(idx)).bounds(x, y, 150, 20).build());
		}
		listen = addRenderableWidget(Button.builder(Component.literal("♪ Lyssna (listen)"),
			b -> SvenskaCraft.audio.play(SvenskaCraft.pack.wordSound(answer.id()), AudioPlayer.Priority.MANUAL)).bounds(cx - 102, top + 126, 100, 20).build());
		reveal = addRenderableWidget(Button.builder(Component.literal("Visa engelska"), b -> {
			revealed = true;
			b.visible = false;
			layoutSmallButtons();
		}).bounds(cx + 2, top + 126, 100, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Show English"))).build());
		reveal.visible = settings().translations == Settings.Translations.ON_DEMAND && type == Type.PICTURE_TO_TARGET && !revealed;
		updateListen();
	}

	private void updateListen() {
		listen.visible = SvenskaCraft.audio.voiceEnabled() && (targetShown() || chosen != -2);
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
		if (type == Type.TARGET_TO_PICTURE) {
			if (chosen == -2) return Component.empty();
			if (i == correctIndex) return Component.literal("✔").withStyle(ChatFormatting.GREEN);
			if (i == chosen) return Component.literal("✘").withStyle(ChatFormatting.RED);
			return Component.empty();
		}
		Entry e = options.get(i);
		String text = type == Type.TARGET_TO_EN ? e.en() : e.word();
		if (chosen == -2) return Component.literal(text);
		if (i == correctIndex) return Component.literal("✔ " + text).withStyle(ChatFormatting.GREEN);
		if (i == chosen) return Component.literal("✘ " + text).withStyle(ChatFormatting.RED);
		return Component.literal(text).withStyle(ChatFormatting.DARK_GRAY);
	}

	private void answer(int idx) {
		if (chosen != -2 || minecraft == null) return;
		chosen = idx;
		boolean correct = idx == correctIndex;
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

		if (correct) {
			correctCount++;
			progress.points++;
			progress.streak++;
			progress.bestStreak = Math.max(progress.bestStreak, progress.streak);
			SvenskaCraft.audio.react("correct");
			AudioPlayer.cue(SoundEvents.NOTE_BLOCK_PLING, 1.6f, 0.35f);
			resultLine = Component.literal(SvenskaCraft.pack.ui("correct") + "  ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD).append(pair);
		} else {
			progress.streak = 0;
			SvenskaCraft.audio.react(idx == -1 ? "timeout" : "wrong");
			AudioPlayer.cue(SoundEvents.NOTE_BLOCK_BASS, 0.7f, 0.5f);
			String head = idx == -1 ? SvenskaCraft.pack.ui("timeout") : SvenskaCraft.pack.ui("wrong");
			resultLine = Component.literal(head + "  ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD).append(pair);
			detailLine = Penalty.apply(minecraft, settings().consequence);
			if (!detailLine.getString().isEmpty()) SvenskaCraft.say(detailLine);
		}
		if (after.ordinal() > before.ordinal() && after != WordState.LEARNING) {
			detailLine = Component.literal("↑ " + answer.word() + ": " + Journal.stateName(after)).withStyle(Journal.stateColor(after));
		}
		SvenskaCraft.progressChanged();
		for (int i = 0; i < 4; i++) buttons[i].setMessage(buttonText(i));
		updateListen();
		nextAt = ticks + FEEDBACK_TICKS;
	}

	@Override
	public void tick() {
		ticks++;
		if (chosen == -2 && ticks >= limitTicks) answer(-1);
		if (nextAt >= 0 && ticks >= nextAt) {
			if (index + 1 < questions.size()) {
				nextQuestion();
				rebuildWidgets();
			} else {
				SvenskaCraft.say(Component.literal("Quiz: " + correctCount + "/" + questions.size() + " rätt (correct)")
					.withStyle(correctCount == questions.size() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
				onClose();
			}
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		int cx = width / 2;
		int top = top();
		String counter = questions.size() > 1 ? "   " + (index + 1) + "/" + questions.size() : "";
		g.centeredText(font, Component.literal("★ " + SvenskaCraft.pack.ui("quizTitle") + " ★" + counter).withStyle(ChatFormatting.BOLD), cx, top, 0xFFFFCC00);

		boolean showEn = settings().translations == Settings.Translations.ALWAYS || revealed;
		switch (type) {
			case TARGET_TO_EN, TARGET_TO_PICTURE -> {
				String q = type == Type.TARGET_TO_EN ? SvenskaCraft.pack.ui("whatDoesItMean") : SvenskaCraft.pack.ui("whichPicture");
				g.centeredText(font, Component.literal(q), cx, top + 16, 0xFFFFFFFF);
				g.pose().pushMatrix();
				g.pose().translate(cx, top + 30);
				g.pose().scale(2.0f, 2.0f);
				g.centeredText(font, Component.literal(answer.word()).withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFAA00);
				g.pose().popMatrix();
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

		int barW = 304, x0 = cx - barW / 2, y0 = top + 66;
		float left = chosen == -2 ? Math.max(0f, 1f - (ticks + partialTick) / limitTicks) : 0f;
		g.fill(x0, y0, x0 + barW, y0 + 4, 0xFF333333);
		g.fill(x0, y0, x0 + (int) (barW * left), y0 + 4, left > 0.5f ? 0xFF55FF55 : left > 0.2f ? 0xFFFFDD33 : 0xFFFF5555);

		if (type == Type.TARGET_TO_PICTURE) {
			for (int i = 0; i < 4; i++) {
				Button b = buttons[i];
				drawIcon(g, options.get(i), b.getX() + b.getWidth() / 2 - 8, b.getY() + 2, 1.0f);
			}
		}

		int infoY = top + 152;
		if (chosen == -2) {
			Settings.Consequence c = settings().consequence;
			if (c != Settings.Consequence.NONE) {
				String what = c == Settings.Consequence.SMALL ? "1 item" : "a whole stack";
				String shield = SvenskaCraft.progress.shields > 0 ? "  [shield: " + SvenskaCraft.progress.shields + "]" : "";
				g.centeredText(font, Component.literal("Wrong answer = you lose " + what + shield), cx, infoY, 0xFFFF7777);
			}
		} else {
			g.centeredText(font, resultLine, cx, infoY, 0xFFFFFFFF);
			g.centeredText(font, detailLine, cx, infoY + 13, 0xFFFFFFFF);
		}
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
	public boolean shouldCloseOnEsc() { return false; }

	@Override
	public boolean isPauseScreen() { return false; }
}
