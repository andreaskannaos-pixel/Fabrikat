package svenskacraft;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import svenskacraft.lang.LanguagePack;
import svenskacraft.learning.AnswerCheck;
import svenskacraft.learning.Forms;
import svenskacraft.learning.Level;
import svenskacraft.vocab.Vocabulary;

class FormsTest {
	private final LanguagePack sv = LanguagePack.load("sv");
	private final Vocabulary vocab = Vocabulary.load(sv);

	private LanguagePack.WordText t(String id) { return sv.word(id); }

	@Test
	void everyWordHasTheGrammarItsKindNeeds() {
		for (Vocabulary.Entry e : vocab.entries()) {
			String c = e.category();
			var w = e.text();
			assertNotNull(w.definition(), e.id() + " has no definition");
			if (c.equals("verbs")) {
				assertNotNull(w.present(), e.id());
				assertNotNull(w.past(), e.id());
				assertNotNull(w.supine(), e.id());
			} else if (c.equals("numbers")) {
				assertNotNull(w.ordinal(), e.id());
			} else if (c.equals("colors")) {
				// rosa, lila, orange never change; the others have an ett-form and a plural
				if (!List.of("orange", "pink", "purple").contains(e.id())) {
					assertNotNull(w.neuter(), e.id());
					assertNotNull(w.plural(), e.id());
				}
			} else if (!c.equals("phrases")) {
				assertNotNull(w.definite(), e.id() + " has no definite form");
			}
		}
	}

	@Test
	void definitionsNeverContainTheAnswer() {
		for (Vocabulary.Entry e : vocab.entries()) {
			String def = e.text().definition().toLowerCase(Locale.ROOT);
			String noun = Forms.noun(e.word()).toLowerCase(Locale.ROOT).replaceAll("[!?.]", "");
			if (noun.length() < 3) continue;
			assertFalse(Pattern.compile("(?<!\\p{L})" + Pattern.quote(noun) + "(?!\\p{L})").matcher(def).find(),
				e.id() + ": \"" + def + "\" gives away \"" + noun + "\"");
		}
	}

	@Test
	void askableFormsDependOnLevel() {
		assertTrue(Forms.askable(t("diamond"), Level.BEGINNER).isEmpty());
		assertEquals(List.of(Forms.Type.DEFINITE, Forms.Type.PLURAL), Forms.askable(t("diamond"), Level.INTERMEDIATE));
		assertEquals(List.of(Forms.Type.PRESENT), Forms.askable(t("verb_eat"), Level.INTERMEDIATE));
		assertEquals(List.of(Forms.Type.PRESENT, Forms.Type.PAST, Forms.Type.SUPINE), Forms.askable(t("verb_eat"), Level.ADVANCED));
		assertEquals(List.of(Forms.Type.ORDINAL), Forms.askable(t("num_3"), Level.INTERMEDIATE));
		assertEquals(List.of(Forms.Type.DEFINITE), Forms.askable(t("dirt"), Level.ADVANCED)); // mass noun: no plural
	}

	@Test
	void distractorsAreWrongButPlausible() {
		List<String> d = Forms.distractors(t("diamond"), Forms.Type.DEFINITE);
		assertFalse(d.contains("diamanten"));
		assertTrue(d.contains("diamanter"), "the plural is a good trap: " + d);
		assertTrue(d.contains("diamantet"), d.toString());
		List<String> past = Forms.distractors(t("verb_eat"), Forms.Type.PAST);
		assertFalse(past.contains("åt"));
		assertTrue(past.contains("ätit") && past.contains("äter"), past.toString());
		assertTrue(past.contains("ätade"), "typical learner mistake: " + past);
		for (Vocabulary.Entry e : vocab.entries()) {
			for (Forms.Type type : Forms.askable(e.text(), Level.ADVANCED)) {
				var correct = Forms.alternatives(Forms.all(e.text()).get(type));
				for (String wrong : Forms.distractors(e.text(), type)) assertFalse(correct.contains(wrong), e.id() + " " + type + " " + wrong);
			}
		}
	}

	@Test
	void noSillyFormsForWordsEndingInAVowel() {
		List<String> plural = Forms.distractors(t("cookie"), Forms.Type.PLURAL);   // en kaka → kakor
		assertFalse(plural.contains("kakaor") || plural.contains("kakaar"), plural.toString());
		assertTrue(plural.contains("kakar"), plural.toString());
		List<String> def = Forms.distractors(t("cookie"), Forms.Type.DEFINITE);    // kakan
		assertFalse(def.contains("kakaen"), def.toString());
		assertTrue(def.contains("kakat"), def.toString());
	}

	@Test
	void gapSentencesFindTheWord() {
		Forms.Cloze c = Forms.cloze(t("diamond"));
		assertEquals("Jag har ", c.before());
		assertEquals("en diamant", c.answer());
		assertEquals("!", c.after());
		Forms.Cloze dirt = Forms.cloze(t("dirt"));
		assertEquals("jorden", dirt.answer());
		assertEquals(Forms.Type.DEFINITE, dirt.type());
		assertEquals("går", Forms.cloze(t("verb_walk")).answer());
		assertEquals("hej", Forms.cloze(t("phrase_hello")).answer());
		assertEquals("bläck", Forms.cloze(t("ink")).answer(), "whole words only, not inside bläckfisken");
		int found = 0;
		for (Vocabulary.Entry e : vocab.entries()) if (Forms.cloze(e.text()) != null) found++;
		assertTrue(found >= vocab.entries().size() * 95 / 100, "gap sentences for " + found + "/" + vocab.entries().size() + " words");
	}

	@Test
	void adjectivesAgreeWithTheNoun() {
		assertEquals(List.of(Forms.Type.NEUTER, Forms.Type.PLURAL), Forms.askable(t("red"), Level.INTERMEDIATE));
		List<String> neuter = Forms.distractors(t("red"), Forms.Type.NEUTER);   // rött
		assertTrue(neuter.contains("rödt"), "classic mistake: " + neuter);
		assertFalse(neuter.contains("rött"));
		List<String> plural = Forms.distractors(t("red"), Forms.Type.PLURAL);   // röda
		assertFalse(plural.contains("röda"));
		assertFalse(plural.contains("rödar") && plural.contains("rödor"), "no noun endings for adjectives: " + plural);
		assertTrue(Forms.askable(t("pink"), Level.ADVANCED).isEmpty(), "rosa never changes");
		Forms.Cloze c = Forms.cloze(t("red"));
		assertEquals("rött", c.answer());
		assertEquals(Forms.Type.NEUTER, c.type());
		assertEquals(AnswerCheck.Verdict.CORRECT, AnswerCheck.check("blåa", Forms.all(t("blue")).get(Forms.Type.PLURAL)).verdict());
	}

	@Test
	void typedAnswersAcceptAlternatives() {
		assertEquals(AnswerCheck.Verdict.CORRECT, AnswerCheck.check("papperet", "pappret|papperet").verdict());
		assertEquals(AnswerCheck.Verdict.CORRECT, AnswerCheck.check("pappret", "pappret|papperet").verdict());
		assertEquals(AnswerCheck.Verdict.WRONG, AnswerCheck.check("papper", "pappret|papperet").verdict());
	}
}
