package svenskacraft.progress;

import java.util.*;

/** All progress for one language. Saved as config/svenskacraft/progress-&lt;lang&gt;.json. */
public class Progress {
	public static final int VERSION = 1;
	public static final int MAX_SHIELDS = 3;

	public int version = VERSION;
	public String language;
	public int points;
	public int streak;
	public int bestStreak;
	public int shields;
	public Set<String> milestones = new LinkedHashSet<>();
	public Map<String, WordProgress> words = new LinkedHashMap<>();
	/** Most recent quiz questions, newest last. */
	public List<String> recent = new ArrayList<>();

	public static Progress fresh(String language) {
		Progress p = new Progress();
		p.language = language;
		return p;
	}

	/** Repairs fields that may be missing in older or hand-edited files. */
	public Progress normalize(String language) {
		if (this.language == null) this.language = language;
		if (milestones == null) milestones = new LinkedHashSet<>();
		if (words == null) words = new LinkedHashMap<>();
		if (recent == null) recent = new ArrayList<>();
		words.values().removeIf(Objects::isNull);
		for (WordProgress w : words.values()) w.box = Math.max(0, Math.min(5, w.box));
		shields = Math.max(0, Math.min(MAX_SHIELDS, shields));
		version = VERSION;
		return this;
	}

	public WordProgress word(String id) { return words.get(id); }

	public WordProgress wordOrCreate(String id) { return words.computeIfAbsent(id, k -> new WordProgress()); }

	public boolean isDiscovered(String id) {
		WordProgress w = words.get(id);
		return w != null && w.discovered();
	}

	/** Marks a word as discovered. Returns true only the first time. */
	public boolean discover(String id, long now) {
		WordProgress w = wordOrCreate(id);
		if (w.discovered()) return false;
		w.discoveredAt = now;
		return true;
	}

	public List<String> discoveredIds() {
		List<String> out = new ArrayList<>();
		for (Map.Entry<String, WordProgress> e : words.entrySet()) if (e.getValue().discovered()) out.add(e.getKey());
		return out;
	}

	public void addShield() { shields = Math.min(MAX_SHIELDS, shields + 1); }
}
