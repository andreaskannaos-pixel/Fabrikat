package svenskacraft.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** JSON helpers: classpath resources and crash-safe file writes. */
public final class Json {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private Json() {}

	public static <T> T resource(String path, Type type) {
		try (InputStream in = Json.class.getResourceAsStream(path)) {
			if (in == null) throw new IllegalStateException("Missing resource " + path);
			return GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), type);
		} catch (IOException e) {
			throw new IllegalStateException("Could not read " + path, e);
		}
	}

	/** Writes to a temp file, keeps the previous version as .bak, then swaps the new file in. */
	public static void writeAtomic(Path file, Object value) throws IOException {
		Files.createDirectories(file.getParent());
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		Files.writeString(tmp, GSON.toJson(value), StandardCharsets.UTF_8);
		if (Files.exists(file)) Files.copy(file, backupOf(file), StandardCopyOption.REPLACE_EXISTING);
		try {
			Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/** Reads file, falling back to its .bak copy if the main file is missing or broken. Returns null if neither works. */
	public static <T> T readWithBackup(Path file, Class<T> type) {
		for (Path p : new Path[] {file, backupOf(file)}) {
			try {
				if (!Files.exists(p)) continue;
				T value = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), type);
				if (value != null) return value;
			} catch (Exception ignored) {
				// try the backup next
			}
		}
		return null;
	}

	public static Path backupOf(Path file) {
		return file.resolveSibling(file.getFileName() + ".bak");
	}
}
