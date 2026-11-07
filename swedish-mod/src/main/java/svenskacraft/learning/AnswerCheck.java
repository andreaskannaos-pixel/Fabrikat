package svenskacraft.learning;

import java.text.Normalizer;
import java.util.Locale;

/** Checks a typed answer. Forgiving for beginners: small slips count, with a hint. */
public final class AnswerCheck {
	public enum Verdict { CORRECT, ALMOST, WRONG }

	/** Why an answer was only "almost" right, so the UI can phrase the hint in any language. */
	public enum Reason { NONE, ARTICLE_MISSING, ARTICLE_WRONG, LETTERS, TYPO }

	/** @param hint what to remember next time, in English (empty when exactly right) */
	public record Result(Verdict verdict, Reason reason, String hint) {
		public boolean accepted() { return verdict != Verdict.WRONG; }
	}

	private AnswerCheck() {}

	/** Accepts any of several "|"-separated alternatives; the best verdict wins. */
	public static Result check(String typed, String expected) {
		Result best = null;
		for (String alt : expected.split("\\|")) {
			Result r = checkOne(typed, alt.trim());
			if (best == null || r.verdict().ordinal() < best.verdict().ordinal()) best = r;
		}
		return best;
	}

	private static Result checkOne(String typed, String expected) {
		String t = normalize(typed);
		String e = normalize(expected);
		if (t.isEmpty()) return new Result(Verdict.WRONG, Reason.NONE, "");
		if (t.equals(e)) return new Result(Verdict.CORRECT, Reason.NONE, "");

		String article = article(e);
		String noun = article == null ? e : e.substring(article.length() + 1);
		String typedArticle = article(t);
		String typedNoun = typedArticle == null ? t : t.substring(typedArticle.length() + 1);

		if (article != null && typedNoun.equals(noun)) {
			if (typedArticle == null) return new Result(Verdict.ALMOST, Reason.ARTICLE_MISSING, "Remember the article: " + article + " " + noun);
			return new Result(Verdict.ALMOST, Reason.ARTICLE_WRONG, "It's \"" + article + "\", not \"" + typedArticle + "\": " + article + " " + noun);
		}
		if (stripMarks(t).equals(stripMarks(e)) || (article != null && stripMarks(typedNoun).equals(stripMarks(noun)))) {
			return new Result(Verdict.ALMOST, Reason.LETTERS, "Watch the letters å, ä, ö: " + expected);
		}
		if (e.length() >= 5 && distance(t, e) == 1) return new Result(Verdict.ALMOST, Reason.TYPO, "Small typo – it's spelled " + expected);
		return new Result(Verdict.WRONG, Reason.NONE, "");
	}

	public static String normalize(String s) {
		if (s == null) return "";
		String n = s.toLowerCase(Locale.ROOT).replaceAll("[!?.,]", " ").trim().replaceAll("\\s+", " ");
		return Normalizer.normalize(n, Normalizer.Form.NFC);
	}

	/** "en" or "ett" if the (normalized) word starts with an article, else null. */
	public static String article(String normalizedWord) {
		if (normalizedWord.startsWith("en ") && normalizedWord.length() > 3) return "en";
		if (normalizedWord.startsWith("ett ") && normalizedWord.length() > 4) return "ett";
		return null;
	}

	static String stripMarks(String s) {
		return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
	}

	static int distance(String a, String b) {
		int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) prev[j] = j;
		for (int i = 1; i <= a.length(); i++) {
			cur[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
			}
			int[] tmp = prev; prev = cur; cur = tmp;
		}
		return prev[b.length()];
	}
}
