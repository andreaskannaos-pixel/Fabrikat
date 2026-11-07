package svenskacraft;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import svenskacraft.lang.LanguagePack;
import svenskacraft.learning.Level;
import svenskacraft.learning.Quests;
import svenskacraft.learning.Quests.Type;
import svenskacraft.learning.Statistics;
import svenskacraft.progress.Progress;
import svenskacraft.progress.WordProgress;
import svenskacraft.vocab.Vocabulary;

class QuestsTest {
	private final Vocabulary vocab = Vocabulary.load(LanguagePack.load("sv"));
	private final Quests.Context ctx = new Quests.Context(Level.INTERMEDIATE, true, true);

	private Progress withKnownWords(int n) {
		Progress p = Progress.fresh("sv");
		for (Vocabulary.Entry e : vocab.entries().subList(0, n)) {
			p.discover(e.id(), 1);
			WordProgress w = p.word(e.id());
			w.box = 2; w.correct = 3;
		}
		return p;
	}

	@Test
	void threeDifferentQuestsPerDayAndStableWithinTheDay() {
		Progress p = withKnownWords(20);
		Quests.refresh(p, vocab, 100, ctx);
		assertEquals(3, p.quests.size());
		assertEquals(3, p.quests.stream().map(q -> q.type + ":" + q.category).distinct().count());
		List<Type> first = p.quests.stream().map(q -> q.type).toList();
		p.quests.get(0).progress = 1;
		Quests.refresh(p, vocab, 100, ctx);
		assertEquals(1, p.quests.get(0).progress, "same day keeps progress");
		assertEquals(first, p.quests.stream().map(q -> q.type).toList());
	}

	@Test
	void newPlayerGetsAchievableQuests() {
		Progress p = Progress.fresh("sv");
		Quests.refresh(p, vocab, 5, new Quests.Context(Level.BEGINNER, false, false));
		assertEquals(3, p.quests.size());
		for (Quests.Quest q : p.quests) {
			assertNotEquals(Type.LISTEN, q.type, "voice is off");
			assertNotEquals(Type.TYPED, q.type, "typing is off");
			assertNotEquals(Type.MASTER, q.type, "nothing to master yet");
			assertNotEquals(Type.GRAMMAR, q.type, "beginners get no grammar questions");
		}
	}

	@Test
	void onlyFreshCorrectAnswersCount() {
		Progress p = withKnownWords(20);
		p.quests.clear();
		p.questDay = 1;
		Quests.Quest review = new Quests.Quest();
		review.type = Type.REVIEW; review.target = 2;
		p.quests.add(review);
		assertTrue(Quests.answer(p, "CHOICE", false, true, false).isEmpty(), "repeat within 10 min doesn't count");
		assertTrue(Quests.answer(p, "CHOICE", false, false, true).isEmpty(), "wrong answers don't count");
		assertEquals(0, review.progress);
		Quests.answer(p, "CHOICE", false, true, true);
		List<Quests.Quest> done = Quests.answer(p, "LISTEN", false, true, true);
		assertEquals(List.of(review), done);
		assertTrue(review.done);
		assertTrue(Quests.answer(p, "CHOICE", false, true, true).isEmpty(), "a done quest completes only once");
	}

	@Test
	void categoryQuestNeedsThatCategory() {
		Progress p = Progress.fresh("sv");
		Quests.Quest q = new Quests.Quest();
		q.type = Type.NEW_IN_CATEGORY; q.category = "animals"; q.target = 1;
		p.quests.add(q);
		assertTrue(Quests.newWord(p, "food").isEmpty());
		assertEquals(1, Quests.newWord(p, "animals").size());
	}

	@Test
	void statisticsPerKindAndDay() {
		Progress p = Progress.fresh("sv");
		Statistics.answer(p, "LISTEN", false, true, true, 10);
		Statistics.answer(p, "LISTEN", false, false, true, 10);
		Statistics.answer(p, "FORM", true, true, false, 11);
		assertEquals(1, p.kinds.get("LISTEN").correct);
		assertEquals(1, p.kinds.get("LISTEN").wrong);
		assertEquals(1, Statistics.freshCorrect(p, "LISTEN"));
		assertEquals(0, Statistics.freshCorrect(p, "FORM"), "not fresh");
		assertEquals(1, p.kinds.get("TYPED").correct);
		assertEquals(3, Statistics.lastDays(p, 11, 7).answered);
		assertEquals(1, Statistics.lastDays(p, 11, 1).answered);
		Statistics.answer(p, "CHOICE", false, true, true, 10 + Statistics.KEEP_DAYS);
		assertNull(p.days.get("10"), "old days are trimmed");
	}
}
