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
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import svenskacraft.Penalty;
import svenskacraft.SvenskaCraft;
import svenskacraft.audio.AudioPlayer;
import svenskacraft.learning.AnswerCheck;
import svenskacraft.learning.Forms;
import svenskacraft.learning.Level;
import svenskacraft.learning.Mastery;
import svenskacraft.learning.QuestionPlanner;
import svenskacraft.learning.QuestionPlanner.Kind;
import svenskacraft.learning.Scheduler;
import svenskacraft.learning.SentenceBuilder;
import svenskacraft.learning.WordState;
import svenskacraft.progress.WordProgress;
import svenskacraft.settings.Settings;
import svenskacraft.vocab.Vocabulary.Entry;

/**
 * A quiz round (1–3 questions by intensity), or a practice round from the journal (no timer, no penalty).
 * Which kinds of questions appear depends on the player's level and how well they know each word.
 */
public class QuizScreen extends Screen {
	/** How a word-choice question is shown. */
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
	/** Practice can ask one kind only (e.g. sentence practice); null = let the planner choose. */
	private final Kind forcedKind;
	private SentenceBuilder.Puzzle puzzle;
	private final List<Integer> picked = new ArrayList<>();
	private final Random rnd = new Random();
	private int index = -1;
	private int correctCount;

	// current question
	private Entry answer;
	private Kind kind;
	private boolean typedMode;
	private Type type;                          // CHOICE / LISTEN
	private List<Entry> options = List.of();    // CHOICE / LISTEN
	private List<String> textOptions;           // GENDER / FORM / CLOZE / DEFINITION as choice
	private String expected;                    // typed answer ("|" alternatives allowed)
	private Forms.Type formType;
	private Forms.Cloze cloze;
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
		this(questions, practice, parent, null);
	}

	public QuizScreen(List<Entry> questions, boolean practice, Screen parent, Kind forcedKind) {
		super(Component.literal("Quiz"));
		this.questions = List.copyOf(questions);
		this.practice = practice;
		this.parent = parent;
		this.forcedKind = forcedKind;
		nextQuestion();
		AudioPlayer.cue(SoundEvents.NOTE_BLOCK_CHIME, 1.2f, 0.4f);
	}

	private Settings settings() { return SvenskaCraft.settings; }

	private boolean advanced() { return settings().level == Level.ADVANCED; }

	/** Interface text: English with a Swedish word at lower levels, Swedish only at Advanced. */
	private String t(String english, String swedish) { return advanced() ? swedish : english; }

	private String ui(String key) { return SvenskaCraft.pack.ui(key); }

	// ---------------------------------------------------------------- question setup

	private void nextQuestion() {
		index++;
		answer = questions.get(index);
		chosen = UNANSWERED;
		ticks = 0;
		nextAt = -1;
		revealed = false;
		typed = "";
		resultLine = Component.empty();
		detailLine = Component.empty();
		type = null;
		options = List.of();
		textOptions = null;
		expected = answer.word();
		formType = null;
		cloze = Forms.cloze(answer.text());

		boolean never = settings().translations == Settings.Translations.NEVER;
		boolean noun = AnswerCheck.article(AnswerCheck.normalize(answer.word())) != null;
		List<Forms.Type> askable = Forms.askable(answer.text(), settings().level);
		Set<Kind> avail = EnumSet.of(Kind.CHOICE, Kind.TYPE);
		if (noun) avail.add(Kind.GENDER);
		if (SvenskaCraft.audio.voiceEnabled() && !(never && answer.concept().abstractMeaning())) avail.add(Kind.LISTEN);
		if (!askable.isEmpty()) avail.add(Kind.FORM);
		if (cloze != null) avail.add(Kind.CLOZE);
		if (answer.text().definition() != null) avail.add(Kind.DEFINITION);
		picked.clear();
		puzzle = SentenceBuilder.make(answer.text(), settings().level, rnd);
		if (puzzle != null) avail.add(Kind.SENTENCE);

		WordProgress w = SvenskaCraft.progress.word(answer.id());
		QuestionPlanner.Plan plan = forcedKind != null && avail.contains(forcedKind)
			? new QuestionPlanner.Plan(forcedKind, false)
			: QuestionPlanner.plan(w, settings().level, avail, settings().typing, rnd);
		kind = plan.kind();
		typedMode = kind == Kind.TYPE || plan.typed();

		switch (kind) {
			case CHOICE -> { type = chooseType(answer); options = buildOptions(answer, type); }
			case LISTEN -> { type = never ? Type.TARGET_TO_PICTURE : Type.TARGET_TO_EN; options = buildOptions(answer, type); }
			case GENDER -> textOptions = List.of("en", "ett");
			case FORM -> {
				formType = askable.get(rnd.nextInt(askable.size()));
				expected = Forms.all(answer.text()).get(formType);
				if (!typedMode) textOptions = formOptions();
			}
			case CLOZE -> {
				expected = cloze.answer();
				if (!typedMode) textOptions = clozeOptions();
			}
			case DEFINITION -> {
				if (!typedMode) textOptions = shuffledWithAnswer(Forms.first(expected), sameCategory(e -> e.word()));
			}
			case SENTENCE -> expected = SentenceBuilder.render(puzzle.answer(), puzzle.punctuation());
			case TYPE -> { }
		}
		if (textOptions != null && textOptions.size() < (kind == Kind.GENDER ? 2 : 4)) {
			// not enough sensible wrong answers: ask by typing if allowed, else fall back to plain choice
			textOptions = null;
			if (settings().typing && kind != Kind.GENDER) typedMode = true;
			else { kind = Kind.CHOICE; type = chooseType(answer); options = buildOptions(answer, type); expected = answer.word(); }
		}
		correctIndex = switch (kind) {
			case GENDER -> AnswerCheck.article(AnswerCheck.normalize(answer.word())).equals("en") ? 0 : 1;
			case CHOICE, LISTEN -> options.indexOf(answer);
			default -> textOptions == null ? 0 : textOptions.indexOf(Forms.first(expected).toLowerCase(Locale.ROOT));
		};
		limitTicks = practice ? Integer.MAX_VALUE : settings().answerSeconds * 20 * (typedMode || kind == Kind.SENTENCE ? 2 : 1);
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

	/** Strings from other words in the same category (then any category), e.g. their definite forms. */
	private List<String> sameCategory(java.util.function.Function<Entry, String> f) {
		List<String> out = new ArrayList<>();
		for (List<Entry> pool : List.of(SvenskaCraft.vocab.inCategory(answer.category()), SvenskaCraft.vocab.entries())) {
			List<Entry> shuffled = new ArrayList<>(pool);
			Collections.shuffle(shuffled, rnd);
			for (Entry e : shuffled) {
				if (e == answer) continue;
				String s = f.apply(e);
				if (s != null && !s.isBlank()) out.add(Forms.first(s).toLowerCase(Locale.ROOT));
			}
		}
		return out;
	}

	private List<String> formOptions() {
		List<String> wrong = new ArrayList<>(Forms.distractors(answer.text(), formType));
		Collections.shuffle(wrong, rnd);
		wrong.addAll(sameCategory(e -> Forms.all(e.text()).get(formType)));
		return shuffledWithAnswer(Forms.first(expected), wrong);
	}

	private List<String> clozeOptions() {
		List<String> others = new ArrayList<>();
		for (String s : sameCategory(e -> Forms.all(e.text()).get(cloze.type()))) others.add(s.replaceAll("[!?.]+$", ""));
		return shuffledWithAnswer(cloze.answer(), others);
	}

	private List<String> shuffledWithAnswer(String correct, List<String> candidates) {
		String c = correct.toLowerCase(Locale.ROOT);
		Set<String> correctAlts = new HashSet<>();
		for (String a : Forms.alternatives(expected)) correctAlts.add(a.toLowerCase(Locale.ROOT));
		List<String> out = new ArrayList<>(List.of(c));
		for (String s : candidates) {
			if (out.size() == 4) break;
			if (!out.contains(s) && !correctAlts.contains(s)) out.add(s);
		}
		Collections.shuffle(out, rnd);
		return out;
	}

	/** The Swedish word is visible before answering (so the ♪ button may play it). */
	private boolean targetShown() {
		return (kind == Kind.CHOICE && (type == Type.TARGET_TO_EN || type == Type.TARGET_TO_PICTURE))
			|| kind == Kind.LISTEN || kind == Kind.FORM;
	}

	private int top() { return Math.max(4, (height - 180) / 2); }

	// ---------------------------------------------------------------- widgets

	@Override
	protected void init() {
		int cx = width / 2;
		int top = top();
		Arrays.fill(buttons, null);
		input = null;
		if (kind == Kind.SENTENCE) {
			initSentence(cx, top);
		} else if (typedMode) {
			input = addRenderableWidget(new EditBox(font, cx - 100, top + 76, 200, 20, Component.literal("answer")));
			input.setMaxLength(60);
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
			buttons[0] = addRenderableWidget(Button.builder(Component.literal(t("Svara (answer)", "Svara")), b -> submitTyped())
				.bounds(cx + 20, top + 100, 80, 20).build());
			if (chosen == UNANSWERED) setInitialFocus(input);
		} else {
			int n = kind == Kind.GENDER ? 2 : 4;
			int rowY = kind == Kind.GENDER ? top + 82 : top + 76;
			for (int i = 0; i < n; i++) {
				final int idx = i;
				int x = (i % 2 == 0) ? cx - 152 : cx + 2;
				buttons[i] = addRenderableWidget(Button.builder(buttonText(i), b -> choose(idx)).bounds(x, rowY + (i / 2) * 24, 150, 20).build());
			}
		}
		String listenLabel = kind == Kind.LISTEN && chosen == UNANSWERED ? t("♪ Spela (play)", "♪ Spela") : t("♪ Lyssna (listen)", "♪ Lyssna");
		boolean sentence = kind == Kind.SENTENCE;
		listen = addRenderableWidget(Button.builder(Component.literal(listenLabel),
			b -> SvenskaCraft.audio.play(sentence ? SvenskaCraft.pack.exampleSound(answer.id()) : SvenskaCraft.pack.wordSound(answer.id()),
				AudioPlayer.Priority.MANUAL)).bounds(sentence ? cx - 50 : cx - 102, top + (sentence ? 128 : 126), 100, 20).build());
		reveal = addRenderableWidget(Button.builder(Component.literal("Visa engelska"), b -> {
			revealed = true;
			b.visible = false;
			layoutSmallButtons();
		}).bounds(sentence ? cx + 52 : cx + 2, top + (sentence ? 128 : 126), 100, 20).tooltip(Tooltip.create(Component.literal("Show English"))).build());
		boolean englishHidden = (kind == Kind.CHOICE && type == Type.PICTURE_TO_TARGET) || kind == Kind.TYPE || kind == Kind.GENDER
			|| kind == Kind.FORM || kind == Kind.CLOZE || kind == Kind.SENTENCE;
		reveal.visible = settings().translations == Settings.Translations.ON_DEMAND && englishHidden && !revealed && chosen == UNANSWERED;
		updateListen();
	}

	private void updateListen() {
		listen.visible = SvenskaCraft.audio.voiceEnabled() && (targetShown() || chosen != UNANSWERED);
		layoutSmallButtons();
	}

	/** Listen / show-English sit side by side, or centred when only one is visible. */
	private void layoutSmallButtons() {
		int cx = width / 2;
		if (kind == Kind.SENTENCE) return; // fixed positions next to Rensa / Svara
		if (listen.visible && reveal.visible) {
			listen.setX(cx - 102);
			reveal.setX(cx + 2);
		} else {
			listen.setX(cx - 50);
			reveal.setX(cx - 50);
		}
	}

	private Component buttonText(int i) {
		if (textOptions != null) {
			String a = textOptions.get(i);
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

	// ---------------------------------------------------------------- sentence builder

	private void initSentence(int cx, int top) {
		boolean open = chosen == UNANSWERED;
		// built sentence (click a tile to take it back)
		List<String> built = new ArrayList<>();
		for (int i : picked) built.add(puzzle.tiles().get(i));
		placeTiles(built, top + 40, cx, open, j -> { picked.remove((int) j); rebuildWidgets(); });
		// tiles still to place
		List<Integer> pool = new ArrayList<>();
		for (int i = 0; i < puzzle.tiles().size(); i++) if (!picked.contains(i)) pool.add(i);
		List<String> poolText = new ArrayList<>();
		for (int i : pool) poolText.add(puzzle.tiles().get(i));
		placeTiles(poolText, top + 90, cx, open, j -> { picked.add(pool.get(j)); rebuildWidgets(); });
		if (open) {
			// with "Visa engelska" there are three buttons in the row, otherwise two, centred
			boolean three = settings().translations == Settings.Translations.ON_DEMAND && !revealed;
			int rx = three ? cx - 152 : cx - 100;
			addRenderableWidget(Button.builder(Component.literal(t("Rensa (clear)", "Rensa")), b -> { picked.clear(); rebuildWidgets(); })
				.bounds(rx, top + 128, 96, 20).build()).active = !picked.isEmpty();
			buttons[0] = addRenderableWidget(Button.builder(Component.literal(t("Svara (answer)", "Svara")), b -> submitSentence())
				.bounds(rx + 100, top + 128, 98, 20).build());
			buttons[0].active = picked.size() == puzzle.answer().size();
		}
	}

	/** Lays tiles out in centred rows (wrapping at 304 px). */
	private void placeTiles(List<String> labels, int y, int cx, boolean active, java.util.function.IntConsumer onClick) {
		List<List<Integer>> rows = new ArrayList<>();
		List<Integer> row = new ArrayList<>();
		int w = 0;
		for (int i = 0; i < labels.size(); i++) {
			int tw = font.width(labels.get(i)) + 12;
			if (!row.isEmpty() && w + tw > 304) { rows.add(row); row = new ArrayList<>(); w = 0; }
			row.add(i);
			w += tw + 4;
		}
		if (!row.isEmpty()) rows.add(row);
		for (List<Integer> r : rows) {
			int total = 0;
			for (int i : r) total += font.width(labels.get(i)) + 16;
			int x = cx - total / 2;
			for (int i : r) {
				int tw = font.width(labels.get(i)) + 12;
				final int idx = i;
				Button b = addRenderableWidget(Button.builder(Component.literal(labels.get(i)), btn -> onClick.accept(idx)).bounds(x, y, tw, 18).build());
				b.active = active;
				x += tw + 4;
			}
			y += 22;
		}
	}

	private void submitSentence() {
		if (chosen != UNANSWERED || kind != Kind.SENTENCE || picked.size() != puzzle.answer().size()) return;
		chosen = 0;
		List<String> chosenTiles = new ArrayList<>();
		for (int i : picked) chosenTiles.add(puzzle.tiles().get(i));
		boolean correct = SentenceBuilder.check(chosenTiles, puzzle);
		Component hint = correct ? Component.empty()
			: Component.literal(t("You built: ", "Du byggde: ") + SentenceBuilder.render(chosenTiles, puzzle.punctuation())).withStyle(ChatFormatting.GRAY);
		finish(correct, hint);
		rebuildWidgets();
	}

	// ---------------------------------------------------------------- answering

	private void choose(int idx) {
		if (chosen != UNANSWERED) return;
		chosen = idx;
		finish(idx == correctIndex, Component.empty());
	}

	private void submitTyped() {
		if (chosen != UNANSWERED || !typedMode) return;
		if (AnswerCheck.normalize(typed).isEmpty()) return;
		chosen = 0;
		AnswerCheck.Result r = AnswerCheck.check(typed, expected);
		Component hint = Component.empty();
		if (r.verdict() == AnswerCheck.Verdict.ALMOST) hint = Component.literal(hintText(r)).withStyle(ChatFormatting.YELLOW);
		if (!r.accepted()) hint = Component.literal(t("You wrote: ", "Du skrev: ") + typed).withStyle(ChatFormatting.GRAY);
		finish(r.accepted(), hint);
	}

	private String hintText(AnswerCheck.Result r) {
		if (!advanced()) return r.hint();
		String exp = Forms.first(expected);
		return switch (r.reason()) {
			case ARTICLE_MISSING -> "Glöm inte artikeln: " + exp;
			case ARTICLE_WRONG -> "Det heter " + exp;
			case LETTERS -> "Tänk på å, ä och ö: " + exp;
			case TYPO -> "Litet stavfel – det stavas " + exp;
			case NONE -> "";
		};
	}

	private void finish(boolean correct, Component hint) {
		if (minecraft == null) return;
		long now = System.currentTimeMillis();
		var progress = SvenskaCraft.progress;
		WordProgress w = progress.wordOrCreate(answer.id());
		WordState before = Mastery.state(w);
		boolean fresh = svenskacraft.learning.Quests.fresh(w, now);
		Mastery.recordAnswer(w, correct, now);
		Scheduler.remember(progress, answer.id());
		WordState after = Mastery.state(w);

		boolean showEn = settings().translations != Settings.Translations.NEVER;
		String shown = kind == Kind.FORM || kind == Kind.CLOZE || kind == Kind.SENTENCE ? Forms.first(expected) : answer.word();
		MutableComponent pair = Component.literal(shown).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		if (showEn && kind != Kind.SENTENCE) pair.append(Component.literal(" = " + answer.en()).withStyle(ChatFormatting.GRAY));

		detailLine = hint;
		if (correct) {
			correctCount++;
			progress.points += typedMode || kind == Kind.SENTENCE ? 2 : 1;
			progress.streak++;
			progress.bestStreak = Math.max(progress.bestStreak, progress.streak);
			SvenskaCraft.audio.react("correct");
			AudioPlayer.cue(SoundEvents.NOTE_BLOCK_PLING, 1.6f, 0.35f);
			resultLine = Component.literal(ui("correct") + "  ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD).append(pair);
		} else {
			progress.streak = 0;
			SvenskaCraft.audio.react(chosen == TIMED_OUT ? "timeout" : "wrong");
			AudioPlayer.cue(SoundEvents.NOTE_BLOCK_BASS, 0.7f, 0.5f);
			String head = chosen == TIMED_OUT ? ui("timeout") : ui("wrong");
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
		SvenskaCraft.onAnswer(kind.name(), typedMode && kind != Kind.SENTENCE, correct, fresh,
			after == WordState.MASTERED && before != WordState.MASTERED);
		SvenskaCraft.progressChanged();
		if (!typedMode && kind != Kind.SENTENCE) for (int i = 0; i < 4; i++) if (buttons[i] != null) buttons[i].setMessage(buttonText(i));
		if (input != null) input.setEditable(false);
		reveal.visible = false;
		listen.setMessage(Component.literal(t("♪ Lyssna (listen)", "♪ Lyssna")));
		updateListen();
		nextAt = ticks + FEEDBACK_TICKS + (detailLine.getString().length() > 30 ? 20 : 0);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (typedMode && chosen == UNANSWERED
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
				String label = practice ? t("Practice: ", "Övning: ") : "Quiz: ";
				SvenskaCraft.say(Component.literal(label + correctCount + "/" + questions.size() + " rätt" + (advanced() ? "" : " (correct)"))
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

	// ---------------------------------------------------------------- drawing

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		int cx = width / 2;
		int top = top();
		String counter = questions.size() > 1 ? "   " + (index + 1) + "/" + questions.size() : "";
		String title = practice ? t("ÖVNING · Practice", "ÖVNING") : ui("quizTitle");
		g.centeredText(font, Component.literal("★ " + title + " ★" + counter).withStyle(ChatFormatting.BOLD), cx, top, 0xFFFFCC00);

		boolean showEn = settings().translations == Settings.Translations.ALWAYS || revealed;
		boolean answered = chosen != UNANSWERED;
		switch (kind) {
			case CHOICE -> drawChoicePrompt(g, cx, top, showEn);
			case LISTEN -> {
				g.centeredText(font, Component.literal(ui("listenAndChoose")), cx, top + 16, 0xFFFFFFFF);
				bigText(g, answered ? answer.word() : "♪ ? ♪", cx, top + 30, answered ? 0xFFFFAA00 : 0xFF88CCFF);
			}
			case GENDER -> {
				drawIcon(g, answer, cx - 16, top + 12, 2.0f);
				String noun = Forms.noun(answer.word());
				String art = answer.word().substring(0, answer.word().length() - noun.length()).trim();
				bigText(g, (answered ? art : "___") + " " + noun, cx, top + 46, 0xFFFFAA00);
				g.centeredText(font, Component.literal(ui("enOrEtt") + (showEn ? "  (" + answer.en() + ")" : "")), cx, top + 66, 0xFFCCCCCC);
			}
			case TYPE -> {
				drawIcon(g, answer, cx - 16, top + 12, 2.0f);
				String q = ui("typeInSwedish") + (showEn ? "  \"" + answer.en() + "\"" : "");
				g.centeredText(font, Component.literal(q), cx, top + 50, 0xFFFFFFFF);
				if (answered) g.centeredText(font, Component.literal("→ " + answer.word()).withStyle(ChatFormatting.BOLD), cx, top + 62, 0xFFFFAA00);
			}
			case FORM -> {
				boolean adj = Forms.isAdjective(answer.text());
				String key = adj && formType == Forms.Type.PLURAL ? "form.adj_plural" : "form." + formType.name().toLowerCase(Locale.ROOT);
				String head = ui(key);
				if (!advanced()) head += "  (" + (adj && formType == Forms.Type.PLURAL ? "with plural words" : formEnglish(formType)) + ")";
				g.centeredText(font, Component.literal(head), cx, top + 16, 0xFFFFFFFF);
				String base = answer.concept().category().equals("verbs") ? "att " + answer.word() : answer.word();
				bigText(g, base, cx, top + 30, 0xFFFFAA00);
				String line = answered ? "→ " + Forms.first(expected) : (showEn ? "(" + answer.en() + ")" : "");
				g.centeredText(font, Component.literal(line).withStyle(answered ? ChatFormatting.BOLD : ChatFormatting.RESET), cx, top + 52,
					answered ? 0xFFFFAA00 : 0xFFAAAAAA);
			}
			case CLOZE -> {
				// the picture makes the gap unambiguous ("Jag äter ___" could be any food)
				Component head = Component.literal(ui("cloze"));
				int hx = cx - (font.width(head) + 20) / 2;
				drawIcon(g, answer, hx, top + 11, 1.0f);
				g.text(font, head, hx + 20, top + 16, 0xFFFFFFFF);
				String gap = answered ? Forms.first(expected).toUpperCase(Locale.ROOT) : "_____";
				int y = top + 30;
				for (FormattedCharSequence line : font.split(Component.literal(cloze.before() + gap + cloze.after()), 300)) {
					g.centeredText(font, line, cx, y, 0xFFFFDD88);
					y += 11;
				}
				if (showEn) g.centeredText(font, Component.literal(answer.exampleEn()).withStyle(ChatFormatting.ITALIC), cx, y + 2, 0xFF999999);
			}
			case SENTENCE -> {
				Component head = Component.literal(ui("buildSentence"));
				int hx = cx - (font.width(head) + 20) / 2;
				drawIcon(g, answer, hx, top + 9, 1.0f);
				g.text(font, head, hx + 20, top + 14, 0xFFFFFFFF);
				String sub = showEn ? answer.exampleEn() : advanced() || settings().translations == Settings.Translations.NEVER
					? "(" + answer.word() + ")" : "";
				g.centeredText(font, Component.literal(sub).withStyle(ChatFormatting.ITALIC), cx, top + 27, 0xFFAAAAAA);
				if (picked.isEmpty() && !answered) g.centeredText(font, Component.literal("_  _  _  _"), cx, top + 45, 0xFF777777);
				if (answered) {
					g.centeredText(font, Component.literal(Forms.first(expected)).withStyle(ChatFormatting.BOLD), cx, top + 70,
						chosen != UNANSWERED && resultLine.getString().startsWith(ui("correct")) ? 0xFF55FF55 : 0xFFFFAA00);
				}
			}
			case DEFINITION -> {
				g.centeredText(font, Component.literal(ui("whatIsIt")), cx, top + 16, 0xFFFFFFFF);
				int y = top + 30;
				for (FormattedCharSequence line : font.split(Component.literal("“" + answer.text().definition() + "”"), 300)) {
					g.centeredText(font, line, cx, y, 0xFFFFDD88);
					y += 11;
				}
				if (answered) g.centeredText(font, Component.literal("→ " + answer.word()).withStyle(ChatFormatting.BOLD), cx, top + 56, 0xFFFFAA00);
			}
		}

		if (!practice) {
			int barW = 304, x0 = cx - barW / 2;
			int y0 = kind == Kind.SENTENCE ? top + 83 : kind == Kind.GENDER ? top + 76 : typedMode ? top + 69 : top + 66;
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
				g.centeredText(font, Component.literal(t("Practice – no timer, no penalty. Esc to stop.", "Övning – ingen tid, inget straff. Esc för att sluta.")),
					cx, infoY, 0xFF88CC88);
			} else if (c != Settings.Consequence.NONE) {
				String what = c == Settings.Consequence.SMALL ? t("1 item", "1 föremål") : t("a whole stack", "en hel stack");
				String shield = SvenskaCraft.progress.shields > 0 ? "  [" + t("shield", "sköld") + ": " + SvenskaCraft.progress.shields + "]" : "";
				g.centeredText(font, Component.literal(t("Wrong answer = you lose ", "Fel svar = du förlorar ") + what + shield), cx, infoY, 0xFFFF7777);
			}
		} else {
			g.centeredText(font, resultLine, cx, infoY, 0xFFFFFFFF);
			g.centeredText(font, detailLine, cx, infoY + 13, 0xFFFFFFFF);
		}
	}

	private static String formEnglish(Forms.Type t) {
		return switch (t) {
			case DEFINITE -> "the …";
			case PLURAL -> "more than one";
			case NEUTER -> "with an ett-word";
			case PRESENT -> "present tense";
			case PAST -> "past tense";
			case SUPINE -> "have + verb";
			case ORDINAL -> "1st, 2nd, 3rd …";
			default -> "";
		};
	}

	private void drawChoicePrompt(GuiGraphicsExtractor g, int cx, int top, boolean showEn) {
		switch (type) {
			case TARGET_TO_EN, TARGET_TO_PICTURE -> {
				String q = type == Type.TARGET_TO_EN ? ui("whatDoesItMean") : ui("whichPicture");
				g.centeredText(font, Component.literal(q), cx, top + 16, 0xFFFFFFFF);
				bigText(g, answer.word(), cx, top + 30, 0xFFFFAA00);
			}
			case EN_TO_TARGET, PICTURE_TO_TARGET -> {
				drawIcon(g, answer, cx - 16, top + 14, 2.0f);
				String q = type == Type.EN_TO_TARGET ? String.format(ui("howDoYouSay"), answer.en()) : ui("whatIsThis");
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
	public boolean isPauseScreen() { return true; } // singleplayer: the world waits while you read or answer
}
