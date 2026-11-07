package svenskacraft.vocab;

import java.util.Map;

/** Short English hints telling the player how to discover a word they haven't found yet. */
public final class Hints {
	private static final Map<String, String> EVENTS = Map.ofEntries(
		Map.entry("join", "Join a world"),
		Map.entry("walk", "Go for a walk"),
		Map.entry("sprint", "Sprint"),
		Map.entry("jump", "Jump"),
		Map.entry("swim", "Go for a swim"),
		Map.entry("eat", "Eat something"),
		Map.entry("dig", "Dig or mine a block"),
		Map.entry("build", "Place some blocks"),
		Map.entry("sleep", "Sleep in a bed"),
		Map.entry("night", "Stay up until nightfall"),
		Map.entry("morning", "See the sunrise"),
		Map.entry("trade", "Trade with a villager"),
		Map.entry("hurt", "Get hurt"),
		Map.entry("low_health", "Get low on health"));

	private Hints() {}

	public static String hint(Concept c) {
		if (c.amount() != null) return "Pick up " + c.amount() + " items at once";
		if (!c.entities().isEmpty()) return c.entities().get(0).endsWith(":tame") ? "Look at a tamed animal" : "Look at an animal";
		if (!c.events().isEmpty()) return EVENTS.getOrDefault(c.events().get(0), "Keep playing");
		if (!c.items().isEmpty()) return "Pick it up";
		return "Keep playing";
	}

	/** True when the hint can safely show the item's picture (the picture is the thing to find). */
	public static boolean showIcon(Concept c) {
		return !c.items().isEmpty() || !c.entities().isEmpty();
	}

	public static boolean knownEvent(String event) { return EVENTS.containsKey(event); }
}
