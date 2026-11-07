package svenskacraft.vocab;

import java.util.List;

/**
 * A language-neutral meaning ("a cow"), how it is discovered in game and its icon.
 * Triggers: items (picked up), entities (looked at; "wolf:tame" = tamed only),
 * events (see discovery.Discovery) or amount (number of items picked up at once).
 */
public record Concept(String id, String category, String en, String icon,
					  List<String> items, List<String> entities, List<String> events, Integer amount) {
	public Concept {
		items = items == null ? List.of() : List.copyOf(items);
		entities = entities == null ? List.of() : List.copyOf(entities);
		events = events == null ? List.of() : List.copyOf(events);
	}

	/** Verbs and phrases have no picture that clearly means them. */
	public boolean abstractMeaning() {
		return category.equals("verbs") || category.equals("phrases");
	}
}
