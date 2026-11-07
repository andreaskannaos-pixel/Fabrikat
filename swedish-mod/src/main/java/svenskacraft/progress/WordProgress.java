package svenskacraft.progress;

/** Learning history of one word. Times are epoch milliseconds. */
public class WordProgress {
	public long discoveredAt;   // 0 = never encountered
	public boolean spoken;      // discovery voice already played
	public int box;             // Leitner box 0..5
	public int correct;
	public int wrong;
	public int streak;
	public long lastAskedAt;
	public long lastCorrectDay = -1;
	public int correctDays;     // distinct days with a correct answer

	public boolean discovered() { return discoveredAt > 0; }

	public int asked() { return correct + wrong; }
}
