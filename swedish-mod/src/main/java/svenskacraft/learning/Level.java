package svenskacraft.learning;

/** How much of the language the player already knows. */
public enum Level {
	BEGINNER("Beginner (A1)", "Words, articles and English support."),
	INTERMEDIATE("Intermediate (A2)", "Grammar: definite form, plural, verbs, gap sentences."),
	ADVANCED("Advanced (B1+)", "Swedish only: definitions, past tense, mostly typed answers.");

	public final String label;
	public final String description;

	Level(String label, String description) {
		this.label = label;
		this.description = description;
	}

	public boolean atLeast(Level other) { return ordinal() >= other.ordinal(); }
}
