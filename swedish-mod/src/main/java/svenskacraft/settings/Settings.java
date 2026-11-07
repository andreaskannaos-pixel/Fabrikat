package svenskacraft.settings;

import java.nio.file.Path;
import svenskacraft.util.Json;

/** Player choices. Saved as config/svenskacraft/settings.json. */
public class Settings {
	public enum Intensity {
		RELAXED("Relaxed", 15, 1, -1),
		BALANCED("Balanced", 10, 2, 120),
		IMMERSIVE("Immersive", 5, 3, 30);

		public final String label;
		public final int quizMinutes;
		public final int questions;
		/** Seconds between hotbar reminders for a known word you pick up again; -1 = never. */
		public final int reminderSeconds;

		Intensity(String label, int quizMinutes, int questions, int reminderSeconds) {
			this.label = label;
			this.quizMinutes = quizMinutes;
			this.questions = questions;
			this.reminderSeconds = reminderSeconds;
		}
	}

	public enum Translations {
		ALWAYS("Always"), ON_DEMAND("On demand"), NEVER("Never");
		public final String label;
		Translations(String label) { this.label = label; }
	}

	public enum VoiceMode {
		OFF("Off"), DISCOVERY("Discovery only"), MANUAL("Manual playback");
		public final String label;
		VoiceMode(String label) { this.label = label; }
	}

	public enum Consequence {
		NONE("No penalty", "None"), SMALL("Small penalty", "Small"), SURVIVAL("Survival challenge", "Survival");
		public final String label;
		public final String shortLabel;
		Consequence(String label, String shortLabel) { this.label = label; this.shortLabel = shortLabel; }
	}

	public boolean enabled = true;
	public String language = "sv";
	public Intensity intensity = Intensity.BALANCED;
	public Translations translations = Translations.ALWAYS;
	public VoiceMode voice = VoiceMode.DISCOVERY;
	public int voiceVolume = 80;
	public Consequence consequence = Consequence.SMALL;
	/** 0 = use the intensity's interval. */
	public int intervalMinutes = 0;
	public int answerSeconds = 30;

	public int quizMinutes() { return intervalMinutes > 0 ? intervalMinutes : intensity.quizMinutes; }

	public Settings normalize() {
		if (language == null || language.isBlank()) language = "sv";
		if (intensity == null) intensity = Intensity.BALANCED;
		if (translations == null) translations = Translations.ALWAYS;
		if (voice == null) voice = VoiceMode.DISCOVERY;
		if (consequence == null) consequence = Consequence.SMALL;
		voiceVolume = Math.max(0, Math.min(100, voiceVolume));
		intervalMinutes = Math.max(0, Math.min(120, intervalMinutes));
		answerSeconds = Math.max(5, Math.min(120, answerSeconds));
		return this;
	}

	public static Path file(Path dir) { return dir.resolve("settings.json"); }

	public static Settings load(Path dir) {
		Settings s = Json.readWithBackup(file(dir), Settings.class);
		return (s == null ? new Settings() : s).normalize();
	}

	public void save(Path dir) throws java.io.IOException { Json.writeAtomic(file(dir), this); }

	public static <E extends Enum<E>> E next(E value) {
		E[] all = value.getDeclaringClass().getEnumConstants();
		return all[(value.ordinal() + 1) % all.length];
	}
}
