package svenskacraft;

import static org.junit.jupiter.api.Assertions.*;
import static svenskacraft.learning.Mastery.DAY;
import static svenskacraft.learning.Mastery.MINUTE;

import java.util.*;
import org.junit.jupiter.api.Test;
import svenskacraft.learning.Scheduler;
import svenskacraft.progress.Progress;
import svenskacraft.progress.WordProgress;

class SchedulerTest {
	private static final long NOW = 1_800_000_000_000L;

	private static Progress withWords(int n) {
		Progress p = Progress.fresh("sv");
		for (int i = 0; i < n; i++) p.discover("w" + i, NOW - DAY);
		return p;
	}

	@Test
	void emptyWhenNothingDiscovered() {
		assertTrue(Scheduler.pick(List.of(), Progress.fresh("sv"), 2, NOW, new Random(1)).isEmpty());
	}

	@Test
	void picksDistinctWords() {
		Progress p = withWords(10);
		List<String> out = Scheduler.pick(p.discoveredIds(), p, 3, NOW, new Random(1));
		assertEquals(3, out.size());
		assertEquals(3, new HashSet<>(out).size());
	}

	@Test
	void neverRepeatsRecentQuestionsWhenThereIsChoice() {
		Progress p = withWords(8);
		Random rnd = new Random(7);
		Deque<String> last5 = new ArrayDeque<>();
		long t = NOW;
		for (int round = 0; round < 200; round++) {
			String id = Scheduler.pick(p.discoveredIds(), p, 1, t, rnd).get(0);
			assertFalse(last5.contains(id), "repeated " + id + " in round " + round);
			p.word(id).lastAskedAt = t;
			Scheduler.remember(p, id);
			last5.addLast(id);
			if (last5.size() > 5) last5.removeFirst();
			t += 11 * MINUTE;
		}
	}

	@Test
	void stillWorksWithOneWord() {
		Progress p = withWords(1);
		Scheduler.remember(p, "w0");
		p.word("w0").lastAskedAt = NOW;
		assertEquals(List.of("w0"), Scheduler.pick(p.discoveredIds(), p, 2, NOW + 1000, new Random(1)));
	}

	@Test
	void difficultWordsComeUpMoreOftenThanMasteredOnes() {
		Progress p = Progress.fresh("sv");
		p.discover("hard", NOW - 30 * DAY);
		p.discover("known", NOW - 30 * DAY);
		WordProgress hard = p.word("hard");
		hard.box = 0; hard.correct = 1; hard.wrong = 4; hard.lastAskedAt = NOW - 15 * MINUTE;
		WordProgress known = p.word("known");
		known.box = 5; known.correct = 20; known.correctDays = 5; known.lastAskedAt = NOW - DAY; // mastered, not due
		Random rnd = new Random(3);
		int hardCount = 0;
		for (int i = 0; i < 1000; i++) if (Scheduler.pick(p.discoveredIds(), p, 1, NOW, rnd).get(0).equals("hard")) hardCount++;
		assertTrue(hardCount > 950, "hard picked " + hardCount + "/1000");
	}

	@Test
	void masteredWordsReturnWhenDue() {
		WordProgress m = new WordProgress();
		m.discoveredAt = NOW - 60 * DAY; m.box = 5; m.correct = 20; m.correctDays = 5;
		m.lastAskedAt = NOW - DAY;
		double notDue = Scheduler.weight(m, NOW);
		m.lastAskedAt = NOW - 8 * DAY;
		double due = Scheduler.weight(m, NOW);
		assertTrue(due > notDue * 10, "due " + due + " vs not due " + notDue);
	}
}
