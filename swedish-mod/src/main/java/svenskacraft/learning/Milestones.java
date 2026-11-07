package svenskacraft.learning;

import java.util.*;
import java.util.function.Predicate;
import svenskacraft.progress.Progress;
import svenskacraft.progress.WordProgress;
import svenskacraft.vocab.Category;
import svenskacraft.vocab.Vocabulary;

/** Learning milestones. Big ones grant a penalty shield. */
public final class Milestones {
	public record Stats(int discovered, int familiarOrBetter, int mastered, int bestStreak, int bestDayStreak, Set<String> completeCategories) {}

	public record Milestone(String id, String title, String description, boolean shield, Predicate<Stats> reached) {}

	private static final List<Milestone> FIXED = List.of(
		m("first_word", "First word!", "Discover your first Swedish word", false, s -> s.discovered() >= 1),
		m("words_10", "Explorer", "Discover 10 words", false, s -> s.discovered() >= 10),
		m("words_25", "Collector", "Discover 25 words", true, s -> s.discovered() >= 25),
		m("words_50", "Word hunter", "Discover 50 words", true, s -> s.discovered() >= 50),
		m("words_100", "Walking dictionary", "Discover 100 words", true, s -> s.discovered() >= 100),
		m("first_familiar", "Getting there", "Make a word familiar", false, s -> s.familiarOrBetter() >= 1),
		m("first_mastered", "First mastery", "Master your first word", true, s -> s.mastered() >= 1),
		m("mastered_10", "Ten for ten", "Master 10 words", true, s -> s.mastered() >= 10),
		m("mastered_25", "Fluent-ish", "Master 25 words", true, s -> s.mastered() >= 25),
		m("mastered_50", "Svensk mästare", "Master 50 words", true, s -> s.mastered() >= 50),
		m("streak_5", "On a roll", "5 correct quiz answers in a row", false, s -> s.bestStreak() >= 5),
		m("streak_10", "Unstoppable", "10 correct quiz answers in a row", true, s -> s.bestStreak() >= 10),
		m("goal_1", "Daily learner", "Reach your daily goal", false, s -> s.bestDayStreak() >= 1),
		m("days_3", "Three days in a row", "Reach your daily goal 3 days in a row", false, s -> s.bestDayStreak() >= 3),
		m("days_7", "A whole week", "Reach your daily goal 7 days in a row", true, s -> s.bestDayStreak() >= 7));

	private Milestones() {}

	private static Milestone m(String id, String title, String desc, boolean shield, Predicate<Stats> p) {
		return new Milestone(id, title, desc, shield, p);
	}

	public static List<Milestone> all(Vocabulary vocab) {
		List<Milestone> out = new ArrayList<>(FIXED);
		for (Category c : vocab.categories()) {
			String id = "category:" + c.id();
			out.add(m(id, c.en() + " complete", "Discover every word in " + c.en(), true, s -> s.completeCategories().contains(c.id())));
		}
		return out;
	}

	public static Stats stats(Progress p, Vocabulary vocab) {
		int discovered = 0, familiar = 0, mastered = 0;
		Set<String> complete = new HashSet<>();
		Map<String, int[]> perCat = new HashMap<>(); // [total, discovered]
		for (Vocabulary.Entry e : vocab.entries()) {
			int[] c = perCat.computeIfAbsent(e.category(), k -> new int[2]);
			c[0]++;
			WordProgress w = p.word(e.id());
			WordState s = Mastery.state(w);
			if (s != WordState.NEW) { discovered++; c[1]++; }
			if (s == WordState.FAMILIAR || s == WordState.MASTERED) familiar++;
			if (s == WordState.MASTERED) mastered++;
		}
		perCat.forEach((cat, c) -> { if (c[0] > 0 && c[0] == c[1]) complete.add(cat); });
		return new Stats(discovered, familiar, mastered, p.bestStreak, p.bestDayStreak, complete);
	}

	/** Returns milestones reached since the last check, records them and hands out shields. */
	public static List<Milestone> check(Progress p, Vocabulary vocab) {
		return check(p, vocab, true);
	}

	/** @param rewards false records reached milestones without shields (used when unlocking all words) */
	public static List<Milestone> check(Progress p, Vocabulary vocab, boolean rewards) {
		Stats s = stats(p, vocab);
		List<Milestone> reached = new ArrayList<>();
		for (Milestone m : all(vocab)) {
			if (!p.milestones.contains(m.id()) && m.reached().test(s)) {
				p.milestones.add(m.id());
				if (m.shield() && rewards) p.addShield();
				reached.add(m);
			}
		}
		return reached;
	}
}
