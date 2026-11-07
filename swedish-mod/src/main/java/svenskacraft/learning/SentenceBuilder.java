package svenskacraft.learning;

import java.util.*;
import svenskacraft.lang.LanguagePack.WordText;

/**
 * Turns a word's example sentence into a word-order puzzle: "Jag / äter / ett äpple".
 *
 * - The final punctuation is kept aside and added back; the first word is lower-cased on its tile
 *   and capitalised again in the finished sentence.
 * - Beginner/Intermediate keep "en/ett + noun" and "att + verb" together; Advanced splits every word
 *   and adds one trap tile (a wrong form of the word, e.g. "ett" instead of "en").
 * - Sentences that are hard to judge as a puzzle are skipped: more than one sentence, or a
 *   time word that may stand in several places (i dag, nu, i morgon ...).
 */
public final class SentenceBuilder {
	/** @param tiles shuffled tiles (may include one trap); @param answer the tiles in the right order */
	public record Puzzle(List<String> tiles, List<String> answer, String punctuation, String original) {}

	private static final Set<String> MOVABLE = Set.of("i dag", "idag", "nu", "i morgon", "igår", "i går", "ofta", "alltid");
	private static final Set<String> PROPER = Set.of("Nether", "Minecraft");

	private SentenceBuilder() {}

	public static int maxTiles(Level level) {
		return switch (level) {
			case BEGINNER -> 4;
			case INTERMEDIATE -> 6;
			case ADVANCED -> 8;
		};
	}

	/** The sentence split into tiles for this level, or null if it isn't suitable as a puzzle. */
	public static List<String> tiles(String sentence, Level level) {
		String s = sentence.trim();
		String body = s.replaceAll("[.!?]+$", "");
		if (body.matches(".*[.!?].*")) return null;                     // two sentences
		String lower = " " + body.toLowerCase(Locale.ROOT) + " ";
		for (String m : MOVABLE) if (lower.contains(" " + m + " ")) return null;
		List<String> words = new ArrayList<>(Arrays.asList(body.split("\\s+")));
		if (!words.isEmpty() && !PROPER.contains(words.get(0))) words.set(0, words.get(0).toLowerCase(Locale.ROOT));
		List<String> out = new ArrayList<>();
		boolean chunk = level != Level.ADVANCED;
		for (int i = 0; i < words.size(); i++) {
			String w = words.get(i);
			boolean joiner = w.equals("en") || w.equals("ett") || w.equals("att");
			if (chunk && joiner && i + 1 < words.size() && !words.get(i).endsWith(",")) {
				out.add(w + " " + words.get(++i));
			} else {
				out.add(w);
			}
		}
		if (out.size() < 3 || out.size() > maxTiles(level)) return null;
		return out;
	}

	public static Puzzle make(WordText t, Level level, Random rnd) {
		List<String> answer = tiles(t.example(), level);
		if (answer == null) return null;
		String punct = t.example().trim().replaceAll("^.*?([.!?]*)$", "$1");
		List<String> tiles = new ArrayList<>(answer);
		if (level == Level.ADVANCED) {
			String trap = trap(t, answer);
			if (trap != null) tiles.add(trap);
		}
		// shuffle until the order differs from the answer
		for (int i = 0; i < 20; i++) {
			Collections.shuffle(tiles, rnd);
			if (!tiles.subList(0, Math.min(tiles.size(), answer.size())).equals(answer)) break;
		}
		return new Puzzle(List.copyOf(tiles), List.copyOf(answer), punct, t.example());
	}

	/** One wrong form of the target word that could tempt a learner, or null. */
	static String trap(WordText t, List<String> answer) {
		Forms.Cloze c = Forms.cloze(t);
		if (c == null) return null;
		Set<String> present = new HashSet<>();
		for (String a : answer) present.add(a.toLowerCase(Locale.ROOT));
		if (c.type() == Forms.Type.BASE) {
			String art = AnswerCheck.article(AnswerCheck.normalize(c.answer()));
			if (art != null) {
				String other = art.equals("en") ? "ett" : "en";
				if (!present.contains(other)) return other;
			}
		}
		for (String d : Forms.distractors(t, c.type())) {
			String lower = d.toLowerCase(Locale.ROOT);
			if (!lower.contains(" ") && !present.contains(lower)) return lower;
		}
		return null;
	}

	/** Correct when the chosen tiles spell the answer (tiles with equal text are interchangeable). */
	public static boolean check(List<String> chosen, Puzzle p) {
		if (chosen.size() != p.answer().size()) return false;
		for (int i = 0; i < chosen.size(); i++) {
			if (!chosen.get(i).equalsIgnoreCase(p.answer().get(i))) return false;
		}
		return true;
	}

	/** The tiles as a sentence: capital first letter, punctuation at the end. */
	public static String render(List<String> tiles, String punctuation) {
		if (tiles.isEmpty()) return "";
		String s = String.join(" ", tiles);
		return Character.toUpperCase(s.charAt(0)) + s.substring(1) + punctuation;
	}
}
