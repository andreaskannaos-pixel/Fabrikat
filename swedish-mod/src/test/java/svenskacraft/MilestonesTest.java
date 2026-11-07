package svenskacraft;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import svenskacraft.lang.LanguagePack;
import svenskacraft.learning.Milestones;
import svenskacraft.progress.Progress;
import svenskacraft.vocab.Vocabulary;

class MilestonesTest {
	private final Vocabulary vocab = Vocabulary.load(LanguagePack.load("sv"));

	@Test
	void firstWordOnceOnly() {
		Progress p = Progress.fresh("sv");
		p.discover("diamond", 1);
		List<Milestones.Milestone> got = Milestones.check(p, vocab);
		assertEquals(List.of("first_word"), got.stream().map(Milestones.Milestone::id).toList());
		assertTrue(Milestones.check(p, vocab).isEmpty());
	}

	@Test
	void completingACategoryGivesAShieldAndShieldsAreCapped() {
		Progress p = Progress.fresh("sv");
		for (Vocabulary.Entry e : vocab.inCategory("numbers")) p.discover(e.id(), 1);
		var ids = Milestones.check(p, vocab).stream().map(Milestones.Milestone::id).toList();
		assertTrue(ids.contains("category:numbers"), ids.toString());
		assertEquals(1, p.shields);
		for (Vocabulary.Entry e : vocab.entries()) p.discover(e.id(), 1);
		Milestones.check(p, vocab);
		assertEquals(Progress.MAX_SHIELDS, p.shields);
	}

	@Test
	void statsCountStates() {
		Progress p = Progress.fresh("sv");
		p.discover("apple", 1);
		var w = p.wordOrCreate("apple");
		w.box = 4; w.correct = 5; w.correctDays = 2;
		Milestones.Stats s = Milestones.stats(p, vocab);
		assertEquals(1, s.discovered());
		assertEquals(1, s.mastered());
		assertEquals(1, s.familiarOrBetter());
	}
}
