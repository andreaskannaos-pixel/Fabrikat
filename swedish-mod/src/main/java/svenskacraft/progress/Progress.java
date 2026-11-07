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
	// daily goal (days are local calendar days, as epoch-day numbers)
	public long today = -1;
	public int answeredToday;
	public long lastGoalDay = -1;
	public int dayStreak;
	public int bestDayStreak;
	// statistics and quests (2.4) – missing in older saves, filled in by normalize()
	/** Answers per question kind ("CHOICE", "LISTEN", "SENTENCE", ... and "TYPED" for any typed answer). */
	public Map<String, Stats.KindStats> kinds = new LinkedHashMap<>();
	/** Per local day (epoch-day number as text). */
	public Map<String, Stats.DayStats> days = new LinkedHashMap<>();
	public List<svenskacraft.learning.Quests.Quest> quests = new ArrayList<>();
	public long questDay = -1;
	public int questsCompleted;
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
		if (kinds == null) kinds = new LinkedHashMap<>();
		if (days == null) days = new LinkedHashMap<>();
		if (quests == null) quests = new ArrayList<>();
		kinds.values().removeIf(Objects::isNull);
		days.values().removeIf(Objects::isNull);
		quests.removeIf(q -> q == null || q.type == null);
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
