package svenskacraft.learning;

import svenskacraft.progress.Progress;

/** Daily goal: answer N quiz questions a day. Reaching it on consecutive days builds a day streak. */
public final class DailyGoal {
	private DailyGoal() {}

	/** Resets the daily counter when the date changed, and breaks the streak if a day was missed. */
	public static void rollOver(Progress p, long today) {
		if (p.today != today) {
			p.today = today;
			p.answeredToday = 0;
		}
		if (p.lastGoalDay >= 0 && today - p.lastGoalDay > 1) p.dayStreak = 0;
	}

	/** Counts one answered question. Returns true when this answer completed today's goal. */
	public static boolean countAnswer(Progress p, long today, int goal) {
		rollOver(p, today);
		p.answeredToday++;
		if (goal <= 0 || p.answeredToday != goal || p.lastGoalDay == today) return false;
		p.dayStreak = p.lastGoalDay == today - 1 ? p.dayStreak + 1 : 1;
		p.bestDayStreak = Math.max(p.bestDayStreak, p.dayStreak);
		p.lastGoalDay = today;
		return true;
	}
}
