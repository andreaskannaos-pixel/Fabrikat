package svenskacraft.learning;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import svenskacraft.lang.LanguagePack.WordText;

/** Grammatical forms of a word, the forms worth asking at each level, and gap sentences. */
public final class Forms {
	public enum Type { BASE, NOUN, DEFINITE, PLURAL, PRESENT, PAST, SUPINE, ORDINAL }

	/** A sentence with one word blanked out. */
	public record Cloze(String before, String answer, String after, Type type) {}

	private Forms() {}

	/** All known forms, each possibly with "|" alternatives. */
	public static Map<Type, String> all(WordText t) {
		Map<Type, String> m = new EnumMap<>(Type.class);
		m.put(Type.BASE, t.word());
		String noun = noun(t.word());
		if (!noun.equals(t.word())) m.put(Type.NOUN, noun);
		put(m, Type.DEFINITE, t.definite());
		put(m, Type.PLURAL, t.plural());
		put(m, Type.PRESENT, t.present());
		put(m, Type.PAST, t.past());
		put(m, Type.SUPINE, t.supine());
		put(m, Type.ORDINAL, t.ordinal());
		return m;
	}

	private static void put(Map<Type, String> m, Type type, String v) {
		if (v != null && !v.isBlank()) m.put(type, v);
	}

	/** Forms a quiz may ask for at this level. */
	public static List<Type> askable(WordText t, Level level) {
		List<Type> out = new ArrayList<>();
		if (!level.atLeast(Level.INTERMEDIATE)) return out;
		Map<Type, String> m = all(t);
		for (Type type : new Type[] {Type.DEFINITE, Type.PLURAL, Type.PRESENT, Type.ORDINAL}) if (m.containsKey(type)) out.add(type);
		if (level.atLeast(Level.ADVANCED)) for (Type type : new Type[] {Type.PAST, Type.SUPINE}) if (m.containsKey(type)) out.add(type);
		return out;
	}

	/** Wrong-but-plausible answers for a form question: the word's other forms plus typical learner mistakes. */
	public static List<String> distractors(WordText t, Type type) {
		Map<Type, String> m = all(t);
		Set<String> correct = new HashSet<>(alternatives(m.get(type)));
		LinkedHashSet<String> out = new LinkedHashSet<>();
		for (Map.Entry<Type, String> e : m.entrySet()) {
			if (e.getKey() != type && e.getKey() != Type.BASE) out.add(first(e.getValue()));
		}
		String noun = m.getOrDefault(Type.NOUN, t.word()).toLowerCase(Locale.ROOT);
		String inf = t.word().toLowerCase(Locale.ROOT);
		String stem = inf.endsWith("a") ? inf.substring(0, inf.length() - 1) : inf;
		boolean vowelEnd = !noun.isEmpty() && "aeiouyåäö".indexOf(noun.charAt(noun.length() - 1)) >= 0;
		String nounStem = vowelEnd && (noun.endsWith("a") || noun.endsWith("e")) ? noun.substring(0, noun.length() - 1) : noun;
		switch (type) {
			case DEFINITE -> out.addAll(vowelEnd ? List.of(noun + "n", noun + "t", nounStem + "en")
				: List.of(noun + "en", noun + "et", noun + "n", noun + "t"));
			case PLURAL -> out.addAll(List.of(nounStem + "ar", nounStem + "or", nounStem + "er", noun + (vowelEnd ? "n" : "en")));
			case PRESENT -> out.addAll(List.of(inf + "r", stem + "er", inf));
			case PAST -> out.addAll(List.of(stem + "ade", stem + "de", stem + "te"));
			case SUPINE -> out.addAll(List.of(stem + "at", stem + "t", stem + "it"));
			default -> { }
		}
		out.removeIf(s -> correct.contains(s) || s.isBlank() || s.equalsIgnoreCase(t.word()) && type != Type.PRESENT);
		return new ArrayList<>(out);
	}

	/** Blanks the first form of the word found in its example sentence, or null if none is found. */
	public static Cloze cloze(WordText t) {
		Map<Type, String> m = all(t);
		Type[] order = {Type.BASE, Type.DEFINITE, Type.PLURAL, Type.PRESENT, Type.PAST, Type.SUPINE, Type.NOUN};
		for (Type type : order) {
			String forms = m.get(type);
			if (forms == null) continue;
			for (String f : alternatives(forms)) {
				String bare = f.replaceAll("[!?.]+$", "");
				if (bare.isBlank()) continue;
				Matcher mt = Pattern.compile("(?<!\\p{L})" + Pattern.quote(bare) + "(?!\\p{L})",
					Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(t.example());
				if (mt.find()) {
					return new Cloze(t.example().substring(0, mt.start()), mt.group().toLowerCase(Locale.ROOT),
						t.example().substring(mt.end()), type);
				}
			}
		}
		return null;
	}

	public static String noun(String word) {
		String lower = word.toLowerCase(Locale.ROOT);
		if (lower.startsWith("en ")) return word.substring(3);
		if (lower.startsWith("ett ")) return word.substring(4);
		return word;
	}

	public static List<String> alternatives(String forms) {
		if (forms == null) return List.of();
		List<String> out = new ArrayList<>();
		for (String s : forms.split("\\|")) if (!s.isBlank()) out.add(s.trim());
		return out;
	}

	public static String first(String forms) {
		List<String> a = alternatives(forms);
		return a.isEmpty() ? "" : a.get(0);
	}
}
