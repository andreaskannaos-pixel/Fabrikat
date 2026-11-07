package svenskacraft.learning;

import java.util.Random;
import svenskacraft.progress.WordProgress;

/**
 * Picks the kind of question for a word. New words get easy multiple choice;
 * as a word becomes familiar, harder kinds appear: en/ett, listening, typing.
 */
public final class QuestionPlanner {
	public enum Kind { CHOICE, GENDER, LISTEN, TYPE }

	public record Options(boolean voice, boolean typing) {}

	private QuestionPlanner() {}

	public static Kind plan(WordProgress w, String word, Options opt, Random rnd) {
		boolean noun = AnswerCheck.article(AnswerCheck.normalize(word)) != null;
		WordState s = Mastery.state(w);
		if (w == null || w.asked() == 0) return Kind.CHOICE;
		// weights: choice, gender, listen, type
		double[] wt = switch (s) {
			case NEW, LEARNING -> new double[] {80, 20, 0, 0};
			case FAMILIAR -> new double[] {35, 15, 20, 30};
			case MASTERED -> new double[] {20, 10, 25, 45};
		};
		if (!noun) wt[1] = 0;
		if (!opt.voice()) wt[2] = 0;
		if (!opt.typing()) wt[3] = 0;
		double total = wt[0] + wt[1] + wt[2] + wt[3];
		double r = rnd.nextDouble() * total;
		for (int i = 0; i < 4; i++) {
			r -= wt[i];
			if (r < 0) return Kind.values()[i];
		}
		return Kind.CHOICE;
	}
}
