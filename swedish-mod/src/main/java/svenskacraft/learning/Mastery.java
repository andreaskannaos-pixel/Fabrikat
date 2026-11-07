package svenskacraft.learning;

import svenskacraft.progress.WordProgress;

/** Leitner boxes and word states. */
public final class Mastery {
	public static final long MINUTE = 60_000L;
	public static final long DAY = 24 * 60 * MINUTE;
	/** Time until a word in box N is due again. */
	public static final long[] INTERVAL_MS = {2 * MINUTE, 5 * MINUTE, 20 * MINUTE, DAY, 3 * DAY, 7 * DAY};

	private Mastery() {}

	public static WordState state(WordProgress p) {
		if (p == null || !p.discovered()) return WordState.NEW;
		double accuracy = p.asked() == 0 ? 0 : (double) p.correct / p.asked();
		if (p.box >= 4 && p.correctDays >= 2 && accuracy >= 0.8) return WordState.MASTERED;
		if (p.box >= 2 && p.correct >= 3) return WordState.FAMILIAR;
		return WordState.LEARNING;
	}

	public static long interval(WordProgress p) { return INTERVAL_MS[Math.max(0, Math.min(5, p.box))]; }

	public static long dueAt(WordProgress p) {
		long base = p.lastAskedAt > 0 ? p.lastAskedAt : p.discoveredAt;
		return base + interval(p);
	}

	public static void recordAnswer(WordProgress p, boolean correct, long now) {
		if (correct) {
			p.correct++;
			p.streak++;
			// Only climb a box when the word was (nearly) due; cramming in one sitting doesn't count.
			boolean dueEnough = p.lastAskedAt == 0 || now >= dueAt(p) - interval(p) / 2;
			if (dueEnough && p.box < 5) p.box++;
			long day = Math.floorDiv(now, DAY);
			if (day != p.lastCorrectDay) {
				p.correctDays++;
				p.lastCorrectDay = day;
			}
		} else {
			p.wrong++;
			p.streak = 0;
			p.box = p.box >= 2 ? 1 : 0;
		}
		p.lastAskedAt = now;
	}
}
