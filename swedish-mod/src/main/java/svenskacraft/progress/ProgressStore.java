package svenskacraft.progress;

import java.io.IOException;
import java.nio.file.Path;
import svenskacraft.util.Json;

/** Loads/saves Progress with a backup copy; saves are batched (at most every 30 s) plus on leave/exit. */
public final class ProgressStore {
	public static final long SAVE_EVERY_MS = 30_000;

	private final Path dir;
	private boolean dirty;
	private long lastSave;

	public ProgressStore(Path dir) { this.dir = dir; }

	public Path file(String language) { return dir.resolve("progress-" + language + ".json"); }

	public Progress load(String language) {
		Progress p = Json.readWithBackup(file(language), Progress.class);
		return (p == null ? Progress.fresh(language) : p).normalize(language);
	}

	public void markDirty() { dirty = true; }

	public boolean isDirty() { return dirty; }

	/** Saves if there are changes and the last save was long enough ago. */
	public void saveIfDue(Progress p, long now) {
		if (dirty && now - lastSave >= SAVE_EVERY_MS) save(p, now);
	}

	/** Like saveIfDue, but never throws (for the game loop). */
	public void saveIfDueQuietly(Progress p) {
		try {
			saveIfDue(p, System.currentTimeMillis());
		} catch (RuntimeException ignored) {
			// retried on the next interval
		}
	}

	public void flush(Progress p, long now) {
		if (dirty) save(p, now);
	}

	public void save(Progress p, long now) {
		try {
			Json.writeAtomic(file(p.language), p);
			dirty = false;
			lastSave = now;
		} catch (IOException e) {
			// keep dirty; next attempt retries
			lastSave = now;
			throw new RuntimeException("Could not save progress to " + file(p.language), e);
		}
	}
}
