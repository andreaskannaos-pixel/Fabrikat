package svenskacraft.vocab;

import com.google.gson.reflect.TypeToken;
import java.util.*;
import svenskacraft.lang.LanguagePack;
import svenskacraft.util.Json;

/** Concepts joined with the active language pack. */
public final class Vocabulary {
	public record Entry(Concept concept, LanguagePack.WordText text) {
		public String id() { return concept.id(); }
		public String word() { return text.word(); }
		public String en() { return concept.en(); }
		public String category() { return concept.category(); }
		public String icon() { return concept.icon(); }
		public String example() { return text.example(); }
		public String exampleEn() { return text.exampleEn(); }
		/** Picture shown for numbers is a stack of this size. */
		public int iconCount() { return concept.amount() == null ? 1 : concept.amount(); }
	}

	private final List<Category> categories;
	private final List<Entry> entries = new ArrayList<>();
	private final Map<String, Entry> byId = new HashMap<>();
	private final Map<String, Entry> byItem = new HashMap<>();
	private final Map<String, List<Entry>> byEntity = new HashMap<>();
	private final Map<String, List<Entry>> byEvent = new HashMap<>();
	private final Map<Integer, Entry> byAmount = new HashMap<>();
	private final List<String> missing = new ArrayList<>();

	public Vocabulary(List<Category> categories, List<Concept> concepts, LanguagePack pack) {
		this.categories = List.copyOf(categories);
		Set<String> catIds = new HashSet<>();
		for (Category c : categories) catIds.add(c.id());
		for (Concept c : concepts) {
			LanguagePack.WordText text = pack.word(c.id());
			if (text == null || !catIds.contains(c.category())) {
				missing.add(c.id());
				continue;
			}
			Entry e = new Entry(c, text);
			entries.add(e);
			byId.put(c.id(), e);
			for (String item : c.items()) byItem.put(item.contains(":") ? item : "minecraft:" + item, e);
			for (String ent : c.entities()) byEntity.computeIfAbsent(ent, k -> new ArrayList<>()).add(e);
			for (String ev : c.events()) byEvent.computeIfAbsent(ev, k -> new ArrayList<>()).add(e);
			if (c.amount() != null) byAmount.put(c.amount(), e);
		}
	}

	public static Vocabulary load(LanguagePack pack) {
		List<Category> cats = Json.resource("/assets/svenskacraft/vocab/categories.json", new TypeToken<List<Category>>() {}.getType());
		List<Concept> concepts = Json.resource("/assets/svenskacraft/vocab/concepts.json", new TypeToken<List<Concept>>() {}.getType());
		return new Vocabulary(cats, concepts, pack);
	}

	public List<Category> categories() { return categories; }

	public List<Entry> entries() { return Collections.unmodifiableList(entries); }

	public Entry byId(String id) { return byId.get(id); }

	public Entry byItem(String itemId) { return byItem.get(itemId); }

	public List<Entry> byEntity(String key) { return byEntity.getOrDefault(key, List.of()); }

	public List<Entry> byEvent(String event) { return byEvent.getOrDefault(event, List.of()); }

	public Entry byAmount(int amount) { return byAmount.get(amount); }

	public List<Entry> inCategory(String categoryId) {
		List<Entry> out = new ArrayList<>();
		for (Entry e : entries) if (e.category().equals(categoryId)) out.add(e);
		return out;
	}

	/** Concept ids skipped because the language pack has no text for them (or their category is unknown). */
	public List<String> missing() { return missing; }
}
