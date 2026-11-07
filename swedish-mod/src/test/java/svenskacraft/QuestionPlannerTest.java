package svenskacraft;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import svenskacraft.learning.Level;
import svenskacraft.learning.QuestionPlanner;
import svenskacraft.learning.QuestionPlanner.Kind;
import svenskacraft.progress.WordProgress;

class QuestionPlannerTest {
	private static final Set<Kind> ALL = EnumSet.allOf(Kind.class);

	private static WordProgress word(int box, int correct, int days) {
		WordProgress w = new WordProgress();
		w.discoveredAt = 1;
		w.box = box;
		w.correct = correct;
		w.correctDays = days;
		return w;
	}

	private static Map<Kind, Integer> sample(WordProgress w, Level level, Set<Kind> avail, boolean typing) {
		Map<Kind, Integer> n = new EnumMap<>(Kind.class);
		Random rnd = new Random(5);
		for (int i = 0; i < 2000; i++) n.merge(QuestionPlanner.plan(w, level, avail, typing, rnd).kind(), 1, Integer::sum);
		return n;
	}

	@Test
	void beginnerFirstTimeIsAlwaysMultipleChoice() {
		assertEquals(Kind.CHOICE, QuestionPlanner.plan(word(0, 0, 0), Level.BEGINNER, ALL, true, new Random(1)).kind());
	}

	@Test
	void beginnersNeverGetGrammarOrDefinitions() {
		Map<Kind, Integer> n = sample(word(5, 20, 5), Level.BEGINNER, ALL, true);
		assertNull(n.get(Kind.FORM));
		assertNull(n.get(Kind.CLOZE));
		assertNull(n.get(Kind.DEFINITION));
	}

	@Test
	void beginnerLearningWordsNeverTypeOrListen() {
		WordProgress w = word(1, 1, 1);
		w.wrong = 2;
		Map<Kind, Integer> n = sample(w, Level.BEGINNER, ALL, true);
		assertNull(n.get(Kind.TYPE));
		assertNull(n.get(Kind.LISTEN));
		assertTrue(n.getOrDefault(Kind.GENDER, 0) > 0);
	}

	@Test
	void intermediateAddsGrammarAndGapSentences() {
		WordProgress w = word(1, 1, 1);
		w.wrong = 1;
		Map<Kind, Integer> n = sample(w, Level.INTERMEDIATE, ALL, true);
		assertTrue(n.getOrDefault(Kind.FORM, 0) > 300, n.toString());
		assertTrue(n.getOrDefault(Kind.CLOZE, 0) > 300, n.toString());
		assertNull(n.get(Kind.DEFINITION));
	}

	@Test
	void advancedIsSwedishOnlyNoTranslationChoices() {
		Map<Kind, Integer> first = sample(word(0, 0, 0), Level.ADVANCED, ALL, true);
		assertEquals(Set.of(Kind.CLOZE, Kind.DEFINITION), first.keySet());
		Map<Kind, Integer> later = sample(word(3, 4, 1), Level.ADVANCED, ALL, true);
		assertNull(later.get(Kind.CHOICE), "advanced never asks for English translations");
	}

	@Test
	void advancedTypesMostAnswers() {
		Random rnd = new Random(2);
		int typed = 0, typable = 0;
		for (int i = 0; i < 2000; i++) {
			QuestionPlanner.Plan p = QuestionPlanner.plan(word(5, 20, 5), Level.ADVANCED, ALL, true, rnd);
			if (p.kind() == Kind.FORM || p.kind() == Kind.CLOZE || p.kind() == Kind.DEFINITION) {
				typable++;
				if (p.typed()) typed++;
			}
		}
		assertTrue(typed > typable * 0.8, typed + "/" + typable);
	}

	@Test
	void sentencesOnlyForWordsYouKnowAndNeverFirst() {
		assertNotEquals(Kind.SENTENCE, QuestionPlanner.plan(word(0, 0, 0), Level.ADVANCED, ALL, true, new Random(1)).kind());
		WordProgress learning = word(1, 1, 1);
		learning.wrong = 2;
		assertNull(sample(learning, Level.BEGINNER, ALL, true).get(Kind.SENTENCE), "beginners build sentences only with familiar words");
		assertTrue(sample(word(3, 4, 1), Level.BEGINNER, ALL, true).getOrDefault(Kind.SENTENCE, 0) > 0);
		assertTrue(sample(learning, Level.INTERMEDIATE, ALL, true).getOrDefault(Kind.SENTENCE, 0) > 100);
	}

	@Test
	void typingSwitchOffMeansNoTyping() {
		Random rnd = new Random(3);
		for (int i = 0; i < 1000; i++) {
			QuestionPlanner.Plan p = QuestionPlanner.plan(word(5, 20, 5), Level.ADVANCED, ALL, false, rnd);
			assertFalse(p.typed());
			assertNotEquals(Kind.TYPE, p.kind());
		}
	}

	@Test
	void unavailableKindsAreNeverPicked() {
		Map<Kind, Integer> n = sample(word(5, 20, 5), Level.INTERMEDIATE, EnumSet.of(Kind.CHOICE, Kind.CLOZE), true);
		assertEquals(EnumSet.of(Kind.CHOICE, Kind.CLOZE), n.keySet());
		assertEquals(Kind.CHOICE, QuestionPlanner.plan(word(0, 0, 0), Level.ADVANCED, EnumSet.of(Kind.CHOICE), true, new Random(1)).kind(),
			"falls back to choice when nothing else fits");
	}
}
