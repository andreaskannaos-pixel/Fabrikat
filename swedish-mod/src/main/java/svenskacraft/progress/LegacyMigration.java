package svenskacraft.progress;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import svenskacraft.settings.Settings;

/** Moves v1.0 data (config/svenskacraft.json) into the new settings + progress files. */
public final class LegacyMigration {
	private LegacyMigration() {}

	/** Returns true if a legacy file was found and migrated. The old file is renamed to *.migrated. */
	public static boolean run(Path legacyFile, Settings settings, Progress progress, long now) {
		if (!Files.exists(legacyFile)) return false;
		try {
			JsonObject o = JsonParser.parseString(Files.readString(legacyFile, StandardCharsets.UTF_8)).getAsJsonObject();
			if (o.has("enabled")) settings.enabled = o.get("enabled").getAsBoolean();
			if (o.has("intervalMinutes")) {
				int m = o.get("intervalMinutes").getAsInt();
				settings.intervalMinutes = m == 10 ? 0 : m; // 10 was the old default
			}
			if (o.has("answerSeconds")) settings.answerSeconds = o.get("answerSeconds").getAsInt();
			if (o.has("penalty")) {
				settings.consequence = o.get("penalty").getAsBoolean() ? Settings.Consequence.SURVIVAL : Settings.Consequence.NONE;
			}
			settings.normalize();
			if (o.has("points")) progress.points += o.get("points").getAsInt();
			if (o.has("streak")) progress.streak = o.get("streak").getAsInt();
			progress.bestStreak = Math.max(progress.bestStreak, progress.streak);
			if (o.has("learned") && o.get("learned").isJsonArray()) {
				JsonArray arr = o.getAsJsonArray("learned");
				for (JsonElement e : arr) {
					WordProgress w = progress.wordOrCreate(e.getAsString());
					if (!w.discovered()) w.discoveredAt = now;
					w.spoken = true; // the old version already said these words out loud
				}
			}
			Files.move(legacyFile, legacyFile.resolveSibling(legacyFile.getFileName() + ".migrated"), StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
