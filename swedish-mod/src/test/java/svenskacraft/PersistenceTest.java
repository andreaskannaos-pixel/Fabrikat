package svenskacraft;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import svenskacraft.progress.LegacyMigration;
import svenskacraft.progress.Progress;
import svenskacraft.progress.ProgressStore;
import svenskacraft.settings.Settings;
import svenskacraft.util.Json;

class PersistenceTest {
	@TempDir Path dir;

	@Test
	void progressRoundTrip() {
		ProgressStore store = new ProgressStore(dir);
		Progress p = store.load("sv");
		p.discover("diamond", 1000);
		p.wordOrCreate("diamond").box = 3;
		p.points = 7;
		p.milestones.add("first_word");
		store.markDirty();
		store.flush(p, 0);
		assertFalse(store.isDirty());
		assertFalse(Files.exists(dir.resolve("progress-sv.json.tmp")));

		Progress back = new ProgressStore(dir).load("sv");
		assertTrue(back.isDiscovered("diamond"));
		assertEquals(3, back.word("diamond").box);
		assertEquals(7, back.points);
		assertTrue(back.milestones.contains("first_word"));
	}

	@Test
	void corruptFileFallsBackToBackup() throws Exception {
		ProgressStore store = new ProgressStore(dir);
		Progress p = store.load("sv");
		p.discover("apple", 1);
		store.save(p, 0);
		p.discover("bread", 2);
		store.save(p, 0); // previous version now in .bak
		Files.writeString(store.file("sv"), "{ broken json");
		Progress back = store.load("sv");
		assertTrue(back.isDiscovered("apple"));
		assertFalse(back.isDiscovered("bread")); // only in the corrupted file
	}

	@Test
	void missingFileGivesFreshProgress() {
		Progress p = new ProgressStore(dir).load("sv");
		assertEquals("sv", p.language);
		assertTrue(p.words.isEmpty());
	}

	@Test
	void batchedSaves() {
		ProgressStore store = new ProgressStore(dir);
		Progress p = store.load("sv");
		store.markDirty();
		store.saveIfDue(p, ProgressStore.SAVE_EVERY_MS);
		assertFalse(store.isDirty());
		store.markDirty();
		store.saveIfDue(p, ProgressStore.SAVE_EVERY_MS + 1000); // too soon
		assertTrue(store.isDirty());
	}

	@Test
	void settingsDefaultsAndRepair() throws Exception {
		Settings s = Settings.load(dir);
		assertEquals(Settings.Intensity.BALANCED, s.intensity);
		assertEquals(svenskacraft.learning.Level.BEGINNER, s.level, "existing players start as beginners");
		assertEquals(10, s.quizMinutes());
		s.intervalMinutes = 3;
		assertEquals(3, s.quizMinutes());
		Files.createDirectories(dir);
		Files.writeString(Settings.file(dir), "{\"intensity\":\"SUPER\",\"voiceVolume\":400,\"voice\":\"MANUAL\"}");
		Settings back = Settings.load(dir);
		assertEquals(Settings.Intensity.BALANCED, back.intensity); // unknown value -> default
		assertEquals(100, back.voiceVolume);
		assertEquals(Settings.VoiceMode.MANUAL, back.voice);
	}

	@Test
	void migratesVersionOneConfig() throws Exception {
		Path legacy = dir.resolve("svenskacraft.json");
		Files.writeString(legacy, """
			{"enabled": true, "intervalMinutes": 7, "answerSeconds": 20, "penalty": true,
			 "points": 12, "streak": 3, "learned": ["diamond", "apple"]}""");
		Settings s = new Settings();
		Progress p = Progress.fresh("sv");
		assertTrue(LegacyMigration.run(legacy, s, p, 5000));
		assertEquals(7, s.intervalMinutes);
		assertEquals(20, s.answerSeconds);
		assertEquals(Settings.Consequence.SURVIVAL, s.consequence);
		assertEquals(12, p.points);
		assertTrue(p.isDiscovered("diamond") && p.isDiscovered("apple"));
		assertTrue(p.word("diamond").spoken, "old words must not be spoken again");
		assertFalse(Files.exists(legacy));
		assertTrue(Files.exists(dir.resolve("svenskacraft.json.migrated")));
		assertFalse(LegacyMigration.run(legacy, s, p, 6000)); // runs once
	}

	@Test
	void atomicWriteKeepsBackup() throws Exception {
		Path f = dir.resolve("x.json");
		Json.writeAtomic(f, java.util.Map.of("a", 1));
		Json.writeAtomic(f, java.util.Map.of("a", 2));
		assertTrue(Files.readString(Json.backupOf(f)).contains("1"));
		assertTrue(Files.readString(f).contains("2"));
	}
}
