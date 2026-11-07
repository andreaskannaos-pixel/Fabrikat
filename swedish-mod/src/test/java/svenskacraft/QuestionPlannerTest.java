package svenskacraft;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import svenskacraft.learning.QuestionPlanner;
import svenskacraft.learning.QuestionPlanner.Kind;
import svenskacraft.progress.WordProgress;

class QuestionPlannerTest {
	private static final QuestionPlanner.Options ALL = new QuestionPlanner.Options(true, true);

	private static WordProgress word(int box, int correct, int days) {
		WordProgress w = new WordProgress();
		w.discoveredAt = 1;
		w.box = box;
		w.correct = correct;
		w.correctDays = days;
		return w;
	}

	private static Map<Kind, Integer> sample(WordProgress w, String word, QuestionPlanner.Options o) {
		Map<Kind, Integer> n = new EnumMap<>(Kind.class);
		Random rnd = new Random(5);
		for (int i = 0; i < 2000; i++) n.merge(QuestionPlanner.plan(w, word, o, rnd), 1, Integer::sum);
		return n;
	}

	@Test
	void firstTimeIsAlwaysMultipleChoice() {
		assertEquals(Kind.CHOICE, QuestionPlanner.plan(word(0, 0, 0), "en ko", ALL, new Random(1)));
	}

	@Test
	void learningWordsNeverTypeOrListen() {
		WordProgress w = word(1, 1, 1);
		w.wrong = 2;
		Map<Kind, Integer> n = sample(w, "en ko", ALL);
		assertNull(n.get(Kind.TYPE));
		assertNull(n.get(Kind.LISTEN));
		assertTrue(n.getOrDefault(Kind.GENDER, 0) > 0);
	}

	@Test
	void masteredWordsAreMostlyHarder() {
		Map<Kind, Integer> n = sample(word(5, 20, 5), "en ko", ALL);
		assertTrue(n.get(Kind.TYPE) > n.get(Kind.CHOICE));
		assertTrue(n.get(Kind.LISTEN) > 0);
	}

	@Test
	void respectsVoiceAndTypingSwitchesAndNounsOnly() {
		Map<Kind, Integer> n = sample(word(5, 20, 5), "hoppa", new QuestionPlanner.Options(false, false));
		assertEquals(Map.of(Kind.CHOICE, 2000), n);
	}
}
