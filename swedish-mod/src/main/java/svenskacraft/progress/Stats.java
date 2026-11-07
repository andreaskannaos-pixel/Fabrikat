package svenskacraft.progress;

/** Saved learning statistics (added in 2.4; older saves start with empty stats). */
public final class Stats {
	private Stats() {}

	/** Answers of one question kind. */
	public static class KindStats {
		public int correct;
		public int wrong;
		/** Correct answers to words not asked in the last 10 minutes (used for achievements). */
		public int freshCorrect;

		public int total() { return correct + wrong; }
	}

	/** What happened on one day. */
	public static class DayStats {
		public int answered;
		public int correct;
		public int newWords;
		public int mastered;
	}
}
