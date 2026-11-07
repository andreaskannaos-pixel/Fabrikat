package svenskacraft;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import svenskacraft.lang.LanguagePack;
import svenskacraft.learning.Level;
import svenskacraft.learning.SentenceBuilder;
import svenskacraft.vocab.Vocabulary;

class SentenceBuilderTest {
	private final LanguagePack sv = LanguagePack.load("sv");
	private final Vocabulary vocab = Vocabulary.load(sv);

	@Test
	void beginnersKeepArticleAndNounTogether() {
		assertEquals(List.of("jag", "äter", "ett äpple"), SentenceBuilder.tiles("Jag äter ett äpple.", Level.BEGINNER));
		assertEquals(List.of("det", "är", "dags", "att äta"), SentenceBuilder.tiles("Det är dags att äta.", Level.INTERMEDIATE));
	}

	@Test
	void advancedSplitsEveryWord() {
		assertEquals(List.of("jag", "äter", "ett", "äpple"), SentenceBuilder.tiles("Jag äter ett äpple.", Level.ADVANCED));
	}

	@Test
	void skipsSentencesThatAreHardToJudge() {
		assertNull(SentenceBuilder.tiles("Hej! Hur mår du?", Level.ADVANCED), "two sentences");
		assertNull(SentenceBuilder.tiles("Vi äter tårta i dag.", Level.ADVANCED), "'i dag' can move");
		assertNull(SentenceBuilder.tiles("Guld glänser.", Level.ADVANCED), "too short");
		assertNull(SentenceBuilder.tiles("Jag lägger en sadel på hästen.", Level.BEGINNER), "too long for beginners");
	}

	@Test
	void keepsProperNounsCapitalised() {
		assertEquals(List.of("kvarts", "finns", "i", "Nether"), SentenceBuilder.tiles("Kvarts finns i Nether.", Level.ADVANCED));
	}

	@Test
	void puzzleIsShuffledAndCheckable() {
		Random rnd = new Random(4);
		SentenceBuilder.Puzzle p = SentenceBuilder.make(sv.word("apple"), Level.INTERMEDIATE, rnd);
		assertNotNull(p);
		assertEquals("Jag äter ett äpple.", SentenceBuilder.render(p.answer(), p.punctuation()));
		assertNotEquals(p.answer(), p.tiles(), "tiles must start shuffled");
		assertTrue(SentenceBuilder.check(p.answer(), p));
		List<String> wrong = new ArrayList<>(p.answer());
		Collections.swap(wrong, 0, 1);
		assertFalse(SentenceBuilder.check(wrong, p));
		assertEquals("Är jag ett äpple.", SentenceBuilder.render(List.of("är", "jag", "ett äpple"), "."));
	}

	@Test
	void advancedAddsOneTrapTile() {
		SentenceBuilder.Puzzle p = SentenceBuilder.make(sv.word("diamond"), Level.ADVANCED, new Random(1));
		assertEquals(p.answer().size() + 1, p.tiles().size());
		List<String> extra = new ArrayList<>(p.tiles());
		for (String a : p.answer()) extra.remove(a);
		assertEquals(List.of("ett"), extra, "en diamant → trap 'ett'");
	}

	@Test
	void enoughSentencesAtEveryLevel() {
		for (Level level : Level.values()) {
			int n = 0;
			for (Vocabulary.Entry e : vocab.entries()) if (SentenceBuilder.tiles(e.example(), level) != null) n++;
			assertTrue(n >= 60, level + ": only " + n + " sentence puzzles");
		}
	}
}
