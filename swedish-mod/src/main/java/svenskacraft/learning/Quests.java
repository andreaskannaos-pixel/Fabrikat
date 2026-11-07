package svenskacraft.learning;

import java.util.*;
import svenskacraft.progress.Progress;
import svenskacraft.progress.WordProgress;
import svenskacraft.vocab.Category;
import svenskacraft.vocab.Vocabulary;

/**
 * Three daily quests, built from things the player does anyway (discovering, answering, mastering).
 * Only "fresh" answers count – a word not asked in the last 10 minutes – so repeating easy
 * questions or looping practice doesn't fill quests.
 */
public final class Quests {
	public enum Type {
		NEW_WORDS("Learn %d new words"),
		NEW_IN_CATEGORY("Learn %d new words: %s"),
		LISTEN("Answer %d listening questions correctly"),
		SENTENCE("Build %d sentences correctly"),
		GRAMMAR("Get %d grammar questions right (en/ett, forms, gaps)"),
		TYPED("Type %d answers correctly"),
		REVIEW("Get %d review questions right"),
		MASTER("Master %d words");

		public final String template;

		Type(String template) { this.template = template; }
	}

	public static class Quest {
		public Type type;
		public String category;   // NEW_IN_CATEGORY only
		public int target;
		public int progress;
		public boolean done;

		public Quest() {}

		Quest(Type type, String category, int target) {
			this.type = type;
			this.category = category;
			this.target = target;
		}

		public String describe(String categoryName) {
			return type == Type.NEW_IN_CATEGORY ? String.format(type.template, target, categoryName) : String.format(type.template, target);
		}
	}

	/** What the player can currently do – quests are only offered when they're achievable. */
	public record Context(Level level, boolean voice, boolean typing) {}

	public static final int PER_DAY = 3;
	public static final int POINTS_PER_QUEST = 5;

	private Quests() {}

	/** Makes sure today's quests exist; picks a new set when the day changes. Same day → same quests. */
	public static void refresh(Progress p, Vocabulary vocab, long today, Context ctx) {
		if (p.questDay == today && p.quests.size() == PER_DAY) return;
		p.questDay = today;
		p.quests.clear();
		p.quests.addAll(generate(p, vocab, today, ctx));
	}

	static List<Quest> generate(Progress p, Vocabulary vocab, long today, Context ctx) {
		Random rnd = new Random(today * 7919L + 17);
		List<Quest> options = new ArrayList<>();
		int undiscovered = 0;
		Map<String, Integer> undiscoveredPerCat = new HashMap<>();
		int familiar = 0, learning = 0;
		for (Vocabulary.Entry e : vocab.entries()) {
			WordState s = Mastery.state(p.word(e.id()));
			if (s == WordState.NEW) {
				undiscovered++;
				undiscoveredPerCat.merge(e.category(), 1, Integer::sum);
			}
			if (s == WordState.FAMILIAR) familiar++;
			if (s == WordState.LEARNING) learning++;
		}
		int known = vocab.entries().size() - undiscovered;
		if (undiscovered >= 3) options.add(new Quest(Type.NEW_WORDS, null, 3));
		List<String> cats = new ArrayList<>();
		for (Category c : vocab.categories()) if (undiscoveredPerCat.getOrDefault(c.id(), 0) >= 2) cats.add(c.id());
		if (!cats.isEmpty()) options.add(new Quest(Type.NEW_IN_CATEGORY, cats.get(rnd.nextInt(cats.size())), 2));
		if (ctx.voice() && known >= 4) options.add(new Quest(Type.LISTEN, null, 3));
		if (known >= 3) options.add(new Quest(Type.SENTENCE, null, 2));
		if (ctx.level().atLeast(Level.INTERMEDIATE) && known >= 4) options.add(new Quest(Type.GRAMMAR, null, 4));
		if (ctx.typing() && known >= 4) options.add(new Quest(Type.TYPED, null, 3));
		if (learning + familiar >= 5) options.add(new Quest(Type.REVIEW, null, 5));
		if (familiar >= 2) options.add(new Quest(Type.MASTER, null, familiar >= 6 ? 2 : 1));
		if (options.size() < PER_DAY && known >= 1) options.add(new Quest(Type.REVIEW, null, 3));
		if (options.size() < PER_DAY) options.add(new Quest(Type.NEW_WORDS, null, Math.max(1, Math.min(3, undiscovered))));
		Collections.shuffle(options, rnd);
		// never two quests of the same type
		List<Quest> out = new ArrayList<>();
		Set<Type> used = EnumSet.noneOf(Type.class);
		for (Quest q : options) {
			if (out.size() == PER_DAY) break;
			if (used.add(q.type)) out.add(q);
		}
		return out;
	}

	// ---------------------------------------------------------------- events (return quests completed by this event)

	public static List<Quest> newWord(Progress p, String category) {
		List<Quest> done = new ArrayList<>();
		for (Quest q : p.quests) {
			if (q.type == Type.NEW_WORDS || (q.type == Type.NEW_IN_CATEGORY && Objects.equals(q.category, category))) step(q, done);
		}
		return done;
	}

	/**
	 * @param kind  question kind (QuestionPlanner.Kind name, or "SENTENCE")
	 * @param fresh the word had not been asked in the last 10 minutes
	 */
	public static List<Quest> answer(Progress p, String kind, boolean typed, boolean correct, boolean fresh) {
		List<Quest> done = new ArrayList<>();
		if (!correct || !fresh) return done;
		boolean grammar = kind.equals("GENDER") || kind.equals("FORM") || kind.equals("CLOZE");
		for (Quest q : p.quests) {
			switch (q.type) {
				case LISTEN -> { if (kind.equals("LISTEN")) step(q, done); }
				case SENTENCE -> { if (kind.equals("SENTENCE")) step(q, done); }
				case GRAMMAR -> { if (grammar) step(q, done); }
				case TYPED -> { if (typed) step(q, done); }
				case REVIEW -> step(q, done);
				default -> { }
			}
		}
		return done;
	}

	public static List<Quest> mastered(Progress p) {
		List<Quest> done = new ArrayList<>();
		for (Quest q : p.quests) if (q.type == Type.MASTER) step(q, done);
		return done;
	}

	public static boolean allDone(Progress p) {
		return !p.quests.isEmpty() && p.quests.stream().allMatch(q -> q.done);
	}

	private static void step(Quest q, List<Quest> done) {
		if (q.done) return;
		q.progress = Math.min(q.target, q.progress + 1);
		if (q.progress >= q.target) {
			q.done = true;
			done.add(q);
		}
	}

	/** True when the word wasn't asked in the last 10 minutes (call before recording the new answer). */
	public static boolean fresh(WordProgress w, long now) {
		return w == null || w.lastAskedAt == 0 || now - w.lastAskedAt >= Scheduler.RECENT_MS;
	}
}
