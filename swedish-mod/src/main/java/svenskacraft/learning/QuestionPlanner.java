package svenskacraft.learning;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import svenskacraft.progress.WordProgress;

/**
 * Picks the kind of question for a word, from the player's level and how well they know the word.
 * Beginners: multiple choice → en/ett → listening → typing.
 * Intermediate adds grammar forms and gap sentences; advanced adds Swedish definitions and mostly typing.
 */
public final class QuestionPlanner {
	public enum Kind { CHOICE, GENDER, LISTEN, TYPE, FORM, CLOZE, DEFINITION }

	/** @param typed answer by typing instead of choosing (for FORM, CLOZE and DEFINITION) */
	public record Plan(Kind kind, boolean typed) {}

	private QuestionPlanner() {}

	/**
	 * @param available kinds this word supports right now (e.g. GENDER only for nouns, LISTEN only with voice on)
	 * @param typing    whether typed answers are allowed at all
	 */
	public static Plan plan(WordProgress w, Level level, Set<Kind> available, boolean typing, Random rnd) {
		boolean first = w == null || w.asked() == 0;
		WordState s = Mastery.state(w);
		Map<Kind, Double> wt = weights(level, s, first);
		if (!typing) wt.remove(Kind.TYPE);
		wt.keySet().retainAll(available);
		Kind kind = pick(wt, rnd);
		double typedChance = switch (level) {
			case BEGINNER -> 0;
			case INTERMEDIATE -> s == WordState.MASTERED ? 0.7 : s == WordState.FAMILIAR ? 0.4 : 0;
			case ADVANCED -> s == WordState.MASTERED ? 0.9 : s == WordState.FAMILIAR ? 0.75 : 0.5;
		};
		boolean typedKind = kind == Kind.FORM || kind == Kind.CLOZE || kind == Kind.DEFINITION;
		boolean typed = typing && typedKind && !first && rnd.nextDouble() < typedChance;
		return new Plan(kind, typed);
	}

	static Map<Kind, Double> weights(Level level, WordState s, boolean first) {
		Map<Kind, Double> m = new EnumMap<>(Kind.class);
		switch (level) {
			case BEGINNER -> {
				if (first) { m.put(Kind.CHOICE, 1.0); break; }
				switch (s) {
					case NEW, LEARNING -> put(m, 80, 20, 0, 0, 0, 0, 0);
					case FAMILIAR -> put(m, 35, 15, 20, 30, 0, 0, 0);
					case MASTERED -> put(m, 20, 10, 25, 45, 0, 0, 0);
				}
			}
			case INTERMEDIATE -> {
				if (first) { put(m, 50, 20, 0, 0, 0, 30, 0); break; }
				switch (s) {
					case NEW, LEARNING -> put(m, 30, 15, 0, 0, 25, 30, 0);
					case FAMILIAR -> put(m, 10, 10, 15, 20, 25, 20, 0);
					case MASTERED -> put(m, 5, 5, 15, 25, 25, 25, 0);
				}
			}
			case ADVANCED -> {
				if (first) { put(m, 0, 0, 0, 0, 0, 50, 50); break; }
				switch (s) {
					case NEW, LEARNING -> put(m, 0, 15, 0, 0, 25, 30, 30);
					case FAMILIAR -> put(m, 0, 0, 10, 20, 25, 25, 20);
					case MASTERED -> put(m, 0, 0, 10, 10, 30, 30, 20);
				}
			}
		}
		m.values().removeIf(v -> v <= 0);
		return m;
	}

	private static void put(Map<Kind, Double> m, double choice, double gender, double listen, double type, double form, double cloze, double def) {
		double[] v = {choice, gender, listen, type, form, cloze, def};
		for (int i = 0; i < v.length; i++) m.put(Kind.values()[i], v[i]);
	}

	private static Kind pick(Map<Kind, Double> wt, Random rnd) {
		double total = 0;
		for (double v : wt.values()) total += v;
		if (total <= 0) return Kind.CHOICE;
		double r = rnd.nextDouble() * total;
		for (Map.Entry<Kind, Double> e : wt.entrySet()) {
			r -= e.getValue();
			if (r < 0) return e.getKey();
		}
		return Kind.CHOICE;
	}
}
