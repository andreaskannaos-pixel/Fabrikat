package svenskacraft;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import svenskacraft.learning.DailyGoal;
import svenskacraft.progress.Progress;

class DailyGoalTest {
	@Test
	void goalCompletesOncePerDay() {
		Progress p = Progress.fresh("sv");
		assertFalse(DailyGoal.countAnswer(p, 100, 3));
		assertFalse(DailyGoal.countAnswer(p, 100, 3));
		assertTrue(DailyGoal.countAnswer(p, 100, 3));
		assertFalse(DailyGoal.countAnswer(p, 100, 3));
		assertEquals(1, p.dayStreak);
		assertEquals(4, p.answeredToday);
	}

	@Test
	void consecutiveDaysBuildAStreakAndAGapBreaksIt() {
		Progress p = Progress.fresh("sv");
		for (long day = 10; day <= 12; day++) DailyGoal.countAnswer(p, day, 1);
		assertEquals(3, p.dayStreak);
		DailyGoal.rollOver(p, 14); // missed day 13
		assertEquals(0, p.dayStreak);
		assertEquals(0, p.answeredToday);
		DailyGoal.countAnswer(p, 14, 1);
		assertEquals(1, p.dayStreak);
		assertEquals(3, p.bestDayStreak);
	}

	@Test
	void noGoalMeansNoCompletion() {
		Progress p = Progress.fresh("sv");
		assertFalse(DailyGoal.countAnswer(p, 1, 0));
		assertEquals(1, p.answeredToday);
	}
}
