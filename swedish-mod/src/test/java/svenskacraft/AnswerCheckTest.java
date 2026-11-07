package svenskacraft;

import static org.junit.jupiter.api.Assertions.*;
import static svenskacraft.learning.AnswerCheck.Verdict.*;

import org.junit.jupiter.api.Test;
import svenskacraft.learning.AnswerCheck;

class AnswerCheckTest {
	private static AnswerCheck.Verdict v(String typed, String expected) {
		return AnswerCheck.check(typed, expected).verdict();
	}

	@Test
	void exactIgnoringCaseSpacesAndPunctuation() {
		assertEquals(CORRECT, v("en diamant", "en diamant"));
		assertEquals(CORRECT, v("  En   Diamant ", "en diamant"));
		assertEquals(CORRECT, v("hej", "Hej!"));
		assertEquals(CORRECT, v("god morgon", "God morgon!"));
	}

	@Test
	void missingOrWrongArticleIsAlmost() {
		assertEquals(ALMOST, v("diamant", "en diamant"));
		assertEquals(ALMOST, v("ett diamant", "en diamant"));
		assertTrue(AnswerCheck.check("ett diamant", "en diamant").hint().contains("\"en\""));
	}

	@Test
	void missingSwedishLettersIsAlmost() {
		assertEquals(ALMOST, v("ett apple", "ett äpple"));
		assertEquals(ALMOST, v("fron", "frön"));
		assertEquals(ALMOST, v("atta", "åtta"));
		assertEquals(ALMOST, v("apple", "ett äpple"));
	}

	@Test
	void oneTypoInLongerWordsIsAlmost() {
		assertEquals(ALMOST, v("en diamamt", "en diamant"));
		assertEquals(WRONG, v("is", "ko"));      // short words must be exact
		assertEquals(WRONG, v("en dimnt", "en diamant"));
	}

	@Test
	void wrongAndEmpty() {
		assertEquals(WRONG, v("", "en ko"));
		assertEquals(WRONG, v("en gris", "en ko"));
		assertFalse(AnswerCheck.check("en gris", "en ko").accepted());
		assertTrue(AnswerCheck.check("diamant", "en diamant").accepted());
	}

	@Test
	void articleDetection() {
		assertEquals("en", AnswerCheck.article("en ko"));
		assertEquals("ett", AnswerCheck.article("ett äpple"));
		assertNull(AnswerCheck.article("ett"));      // the number one
		assertNull(AnswerCheck.article("enkel"));
	}
}
