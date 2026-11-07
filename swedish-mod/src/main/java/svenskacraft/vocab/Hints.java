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
		Map.entry("low_health", "Get low on health"),
		Map.entry("fly", "Fly (elytra or creative)"),
		Map.entry("ride", "Ride a horse or a pig"),
		Map.entry("vehicle", "Take a boat or a minecart"),
		Map.entry("fall", "Fall from somewhere high"),
		Map.entry("climb", "Climb a ladder or vines"),
		Map.entry("fish", "Go fishing"),
		Map.entry("sneak", "Sneak around"),
		Map.entry("drink", "Drink something"),
		Map.entry("hit_friendly", "Hit a friendly animal (oops)"),
		Map.entry("leave", "Open the pause menu after playing a while"),
		Map.entry("milestone", "Reach a milestone"),
		Map.entry("rain", "Stand outside in the rain"),
		Map.entry("thunder", "Be outside in a thunderstorm"),
		Map.entry("sunny", "Be outside on a sunny day"),
		Map.entry("moon", "Look at the night sky"),
		Map.entry("cave", "Go into a cave"),
		Map.entry("biome_forest", "Visit a forest"),
		Map.entry("biome_desert", "Visit a desert"),
		Map.entry("biome_ocean", "Visit the ocean"),
		Map.entry("biome_mountain", "Climb a mountain"),
		Map.entry("biome_swamp", "Visit a swamp"),
		Map.entry("biome_jungle", "Visit a jungle"),
		Map.entry("biome_beach", "Visit a beach"));

	private Hints() {}

	public static String hint(Concept c) {
		if (c.amount() != null) return "Pick up " + c.amount() + " items at once";
		if (!c.entities().isEmpty()) return c.entities().get(0).endsWith(":tame") ? "Look at a tamed animal" : "Look at an animal";
		if (!c.events().isEmpty()) return EVENTS.getOrDefault(c.events().get(0), "Keep playing");
		if (!c.items().isEmpty()) return "Pick it up or hold it";
		return "Keep playing";
	}

	/** True when the hint can safely show the item's picture (the picture is the thing to find). */
	public static boolean showIcon(Concept c) {
		return !c.items().isEmpty() || !c.entities().isEmpty();
	}

	public static boolean knownEvent(String event) { return EVENTS.containsKey(event); }
}
