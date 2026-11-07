package svenskacraft;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import svenskacraft.lang.LanguagePack;
import svenskacraft.vocab.Category;
import svenskacraft.vocab.Vocabulary;

class VocabularyTest {
	private final LanguagePack sv = LanguagePack.load("sv");
	private final Vocabulary vocab = Vocabulary.load(sv);

	@Test
	void everyConceptHasSwedishText() {
		assertTrue(vocab.missing().isEmpty(), "missing: " + vocab.missing());
		assertEquals(116, vocab.entries().size());
	}

	@Test
	void entriesAreComplete() {
		Set<String> ids = new HashSet<>();
		for (Vocabulary.Entry e : vocab.entries()) {
			assertTrue(ids.add(e.id()), "duplicate id " + e.id());
			assertFalse(e.word().isBlank(), e.id());
			assertFalse(e.en().isBlank(), e.id());
			assertFalse(e.example().isBlank(), e.id());
			assertFalse(e.exampleEn().isBlank(), e.id());
			assertNotNull(e.icon(), e.id());
			boolean discoverable = !e.concept().items().isEmpty() || !e.concept().entities().isEmpty()
				|| !e.concept().events().isEmpty() || e.concept().amount() != null;
			assertTrue(discoverable, e.id() + " can never be discovered");
		}
	}

	@Test
	void everyCategoryCanFillAFourOptionQuiz() {
		assertEquals(9, vocab.categories().size());
		for (Category c : vocab.categories()) {
			assertTrue(vocab.inCategory(c.id()).size() >= 4, c.id());
			assertNotEquals(c.id(), sv.category(c.id()), "no Swedish name for " + c.id());
		}
	}

	@Test
	void triggersResolve() {
		assertEquals("en diamant", vocab.byItem("minecraft:diamond").word());
		assertEquals("ett svärd", vocab.byItem("minecraft:netherite_sword").word());
		assertEquals("tre", vocab.byAmount(3).word());
		assertEquals("en hund", vocab.byEntity("wolf:tame").get(0).word());
		assertEquals("en varg", vocab.byEntity("wolf").get(0).word());
		assertEquals("hoppa", vocab.byEvent("jump").get(0).word());
	}

	@Test
	void everyEventInDataIsSupportedByDiscovery() {
		Set<String> supported = Set.of("join", "walk", "sprint", "jump", "swim", "eat", "dig", "build", "sleep",
			"night", "morning", "trade", "hurt", "low_health");
		for (Vocabulary.Entry e : vocab.entries()) {
			for (String ev : e.concept().events()) assertTrue(supported.contains(ev), e.id() + " uses unknown event " + ev);
		}
	}

	@Test
	void reactionsAreShortAndVaried() {
		assertTrue(sv.reactions("correct").size() >= 3);
		assertTrue(sv.reactions("wrong").size() >= 3);
		for (String kind : new String[] {"correct", "wrong", "timeout"}) {
			for (String line : sv.reactions(kind)) assertTrue(line.split(" ").length <= 3, "too long: " + line);
		}
	}

	@Test
	void soundNamesFollowLanguageLayout() {
		assertEquals("sv.word.diamond", sv.wordSound("diamond"));
		assertEquals("sv.example.diamond", sv.exampleSound("diamond"));
		assertEquals("sv.react.correct_1", sv.reactionSound("correct", 0));
	}
}
