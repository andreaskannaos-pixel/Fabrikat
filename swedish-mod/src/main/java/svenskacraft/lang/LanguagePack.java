package svenskacraft.lang;

import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.util.Map;
import svenskacraft.util.Json;

/**
 * Everything specific to the language being learned: words, example sentences,
 * quiz prompts, category names and voice reactions. One folder per language under
 * assets/svenskacraft/languages/&lt;code&gt;/ (pack.json + words.json + sounds/&lt;code&gt;/...).
 */
public final class LanguagePack {
	/**
	 * Text for one word. Grammar fields are optional (null when they don't apply);
	 * a field may list accepted alternatives separated by "|" (first one is shown).
	 */
	public record WordText(String word, String example, String exampleEn,
						   String definite, String plural,               // nouns (plural also for adjectives)
						   String neuter,                                // adjectives: ett-form (rött)
						   String present, String past, String supine,   // verbs
						   String ordinal,                               // numbers
						   String definition) {}                        // short explanation in the language itself

	private record PackFile(String code, String name, String nativeName, Map<String, String> categories,
							Map<String, String> ui, Map<String, List<String>> reactions) {}

	private final PackFile pack;
	private final Map<String, WordText> words;

	private LanguagePack(PackFile pack, Map<String, WordText> words) {
		this.pack = pack;
		this.words = words;
	}

	public static LanguagePack load(String code) {
		String base = "/assets/svenskacraft/languages/" + code + "/";
		PackFile pack = Json.resource(base + "pack.json", PackFile.class);
		Map<String, WordText> words = Json.resource(base + "words.json", new TypeToken<Map<String, WordText>>() {}.getType());
		return new LanguagePack(pack, Map.copyOf(words));
	}

	public String code() { return pack.code(); }

	public String name() { return pack.name(); }

	public String nativeName() { return pack.nativeName(); }

	public WordText word(String conceptId) { return words.get(conceptId); }

	public String ui(String key) { return pack.ui().getOrDefault(key, key); }

	public String category(String categoryId) { return pack.categories().getOrDefault(categoryId, categoryId); }

	public List<String> reactions(String kind) { return pack.reactions().getOrDefault(kind, List.of()); }

	// Sound event names (see tools/generate_voices.py)
	public String wordSound(String conceptId) { return code() + ".word." + conceptId; }

	public String exampleSound(String conceptId) { return code() + ".example." + conceptId; }

	public String reactionSound(String kind, int index) { return code() + ".react." + kind + "_" + (index + 1); }
}
