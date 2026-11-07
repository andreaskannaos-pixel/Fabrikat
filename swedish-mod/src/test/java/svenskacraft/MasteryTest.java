package svenskacraft;

import static org.junit.jupiter.api.Assertions.*;
import static svenskacraft.learning.Mastery.DAY;
import static svenskacraft.learning.Mastery.MINUTE;

import org.junit.jupiter.api.Test;
import svenskacraft.learning.Mastery;
import svenskacraft.learning.WordState;
import svenskacraft.progress.WordProgress;

class MasteryTest {
	private static final long T0 = 1_800_000_000_000L;

	private static WordProgress discovered() {
		WordProgress w = new WordProgress();
		w.discoveredAt = T0;
		return w;
	}

	@Test
	void newThenLearning() {
		assertEquals(WordState.NEW, Mastery.state(null));
		assertEquals(WordState.NEW, Mastery.state(new WordProgress()));
		assertEquals(WordState.LEARNING, Mastery.state(discovered()));
	}

	@Test
	void crammingInOneSittingDoesNotClimbBoxes() {
		WordProgress w = discovered();
		long t = T0 + 3 * MINUTE;
		Mastery.recordAnswer(w, true, t);          // first answer: box 1
		for (int i = 1; i <= 5; i++) Mastery.recordAnswer(w, true, t + i * 1000);
		assertEquals(1, w.box);
		assertEquals(WordState.LEARNING, Mastery.state(w));
	}

	@Test
	void spacedCorrectAnswersReachFamiliarThenMastered() {
		WordProgress w = discovered();
		long t = T0;
		t += 3 * MINUTE; Mastery.recordAnswer(w, true, t);   // box 1
		t += 6 * MINUTE; Mastery.recordAnswer(w, true, t);   // box 2
		t += 21 * MINUTE; Mastery.recordAnswer(w, true, t);  // box 3, 3 correct
		assertEquals(WordState.FAMILIAR, Mastery.state(w));
		t += DAY + MINUTE; Mastery.recordAnswer(w, true, t); // box 4, second day
		assertEquals(4, w.box);
		assertTrue(w.correctDays >= 2);
		assertEquals(WordState.MASTERED, Mastery.state(w));
	}

	@Test
	void masteredNeedsTwoDifferentDays() {
		WordProgress w = discovered();
		w.box = 4;
		w.correct = 10;
		w.correctDays = 1;
		assertEquals(WordState.FAMILIAR, Mastery.state(w));
		w.correctDays = 2;
		assertEquals(WordState.MASTERED, Mastery.state(w));
		w.wrong = 5; // accuracy 10/15 < 80%
		assertEquals(WordState.FAMILIAR, Mastery.state(w));
	}

	@Test
	void wrongAnswerDropsBack() {
		WordProgress w = discovered();
		w.box = 4;
		Mastery.recordAnswer(w, false, T0 + DAY);
		assertEquals(1, w.box);
		assertEquals(0, w.streak);
		Mastery.recordAnswer(w, false, T0 + DAY + MINUTE);
		assertEquals(0, w.box);
	}
}
