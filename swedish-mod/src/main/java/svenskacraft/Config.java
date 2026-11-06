package svenskacraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

public class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("svenskacraft.json");

	public boolean enabled = true;
	public int intervalMinutes = 10;
	public int answerSeconds = 30;
	public boolean penalty = true;
	public int points = 0;
	public int streak = 0;
	public Set<String> learned = new LinkedHashSet<>();

	public static Config load() {
		try {
			if (Files.exists(FILE)) {
				Config c = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), Config.class);
				if (c != null) {
					if (c.learned == null) c.learned = new LinkedHashSet<>();
					return c;
				}
			}
		} catch (Exception e) {
			SvenskaCraft.LOG.warn("Could not read config, using defaults", e);
		}
		return new Config();
	}

	public void save() {
		try {
			Files.writeString(FILE, GSON.toJson(this), StandardCharsets.UTF_8);
		} catch (Exception e) {
			SvenskaCraft.LOG.warn("Could not save config", e);
		}
	}
}
