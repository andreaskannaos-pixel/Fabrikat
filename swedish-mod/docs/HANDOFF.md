# SvenskaCraft – developer handoff

A summary for another developer or AI assistant continuing work on this mod. It describes what exists, how it fits together, and the rules to follow when changing it.

## What it is

A **client-side Fabric mod** that teaches Swedish while you play Minecraft Java. Only the player installs it; it works in singleplayer and on any server.

- **Version:** 2.3.0
- **Minecraft:** 26.3 (default build) and 26.2
- **Loader:** Fabric Loader ≥ 0.16.0
- **Dependency:** Fabric API
- **Java:** 25

**Main features**
- **Discovering words while playing:** items you pick up or hold, numbers (how many items you pick up at once), animals and creatures you look at, actions (verbs), weather, the sun and moon, biomes, and moments (phrases).
- **Voice:** each new word is spoken once.
- **Quizzes:** every N minutes, with spaced repetition and word states New / Learning / Familiar / Mastered.
- **Question kinds:**
  - multiple choice
  - en/ett (gender)
  - listening
  - typing (forgiving answer checking)
  - grammar forms
  - gap sentences
  - Swedish definitions
- **Swedish levels:** Beginner (A1), Intermediate (A2: grammar), Advanced (B1+: Swedish only).
- **Journal (J key):** categories, ♪ replay, example sentences, forms, definitions, progress, milestones, practice mode, hints for undiscovered words.
- **Rewards:** milestones with shields, which block one quiz penalty, plus a daily goal and day streak.
- **Item tooltips:** show the Swedish word for discovered items.
- **Quiz safety:** quizzes never start during danger, and singleplayer pauses while the quiz, journal or settings are open.
- **Penalties:** quiz penalties are None / Small (1 item) / Survival (stack). On servers the item is thrown instead of deleted, because the client can't delete items there.

## Repository layout (`swedish-mod/`)

```
build.gradle, gradle.properties        Loom 1.19 (net.fabricmc.fabric-loom), MC 26.3, Fabric API 0.165.2+26.3
src/main/java/svenskacraft/
  SvenskaCraft.java        entrypoint: config load, tick loop, quiz timer, commands (/svenska ...), J key,
                           tooltips, daily goal, milestones → toasts, unlockAllWords()
  Safety.java              "is the player out of danger" (quiz gate)
  Penalty.java             wrong-answer consequences (+ shields)
  mixin/ClientPacketListenerMixin.java   item pickup hook (handleTakeItemEntity) → onPickup(stack, amount)
  lang/LanguagePack.java   per-language text: words, examples, forms, definitions, UI strings, reactions, sound names
  vocab/                   Concept (language-neutral word), Category, Vocabulary (joins concepts + pack), Hints
  learning/                PURE JAVA, unit tested:
    Level, WordState, Mastery (Leitner boxes), Scheduler (which words to ask),
    QuestionPlanner (which question kind), Forms (grammar forms, distractors, gap sentences),
    AnswerCheck (typed answers), DailyGoal, Milestones
  progress/                Progress + WordProgress (saved data), ProgressStore (atomic save + .bak), LegacyMigration (v1.0 config)
  settings/Settings.java   all player options (enums), saved as JSON
  discovery/Discovery.java all discovery triggers + new-word / reminder display
  audio/AudioPlayer.java   single voice channel (no overlap), reactions, subtle cues
  ui/                      QuizScreen, JournalScreen, SettingsScreen, Journal (shared helpers)
src/main/resources/assets/svenskacraft/
  vocab/categories.json    [{id, icon, en}]
  vocab/concepts.json      language-neutral words (see below)
  languages/sv/pack.json   Swedish UI strings, category names, voice reactions
  languages/sv/words.json  Swedish text per concept id (see below)
  sounds/sv/{word,example,react}/*.ogg + sounds.json   generated voice clips
  lang/en_us.json          Minecraft translation keys (key binding name)
src/test/java/svenskacraft/  JUnit 5 tests (test scope only)
tools/generate_voices.py     Piper TTS → ogg clips + sounds.json
tools/make_curseforge_pack.py  CurseForge import zip (MC + Fabric + Fabric API + mod)
dist/                        release jars / zips
```

## Data formats

**`vocab/concepts.json`:** one entry per word, independent of language.

```json
{"id": "cow", "category": "animals", "en": "a cow", "icon": "cow_spawn_egg", "entities": ["cow"]}
```

Discovery triggers (use one or more):
- `items`: ids picked up or held.
- `entities`: entity ids looked at; `"wolf:tame"` matches tamed only.
- `events`: one of the names in "Discovery events" below.
- `amount`: number of items picked up at once.

Icons must be real item ids.

**`languages/sv/words.json`:** `{ "<concept id>": { ... } }` with these fields:
- **Required:**
  - `word`: Swedish with article for nouns ("en ko"), infinitive for verbs, en-form for adjectives.
  - `example`, `exampleEn`
  - `definition`: Swedish; must not contain the answer.
- **Optional:**
  - `definite`, `plural` (nouns, and adjective plural)
  - `neuter` (adjective ett-form)
  - `present`, `past`, `supine` (verbs)
  - `ordinal` (numbers)
  - Any field can hold alternatives separated by `|`, e.g. `"pappret|papperet"`.

**`languages/sv/pack.json`:** category names in Swedish, `ui` strings (quiz prompts, form labels), and `reactions` (short voice lines: correct / wrong / timeout).

### Discovery events (`Discovery.java`; hints in `vocab/Hints.java`)

- **Actions:** join, walk, sprint, jump, swim, eat, dig, build, fly, ride, vehicle, fall, climb, fish, sneak, drink, hit_friendly
- **Moments:** sleep, night, morning, trade, hurt, low_health, leave (pause menu after 2 min), milestone
- **Sky and weather:** rain, thunder, sunny, moon
- **Places:** cave, biome_forest, biome_desert, biome_ocean, biome_mountain, biome_swamp, biome_jungle, biome_beach

## How to add words (checklist)

1. **Concept:** add it to `concepts.json` with a trigger. An item id may belong to only one concept.
2. **Swedish text:** add it to `languages/sv/words.json`: word, example + English, definition, and the grammar forms for its type.
3. **New event:** if it needs one, detect it in `Discovery.tick()`, call `event("name")`, and add a hint in `Hints.java`.
4. **New category:** add it to `categories.json` and give it a Swedish name in `pack.json` → `categories`. A category needs at least 4 words so quiz options can be filled.
5. **Voice:**

```bash
python3 tools/generate_voices.py sv /path/sv-se-nst-medium.onnx
```

The voice model is Piper `sv-se-nst-medium`, CC0, from the rhasspy/piper GitHub release v0.0.2.

6. **Build and test:**

```bash
./gradlew build
```

The tests check, among other things:
- every concept has Swedish text and a reachable trigger
- definitions don't give away the answer
- distractors are never the correct answer
- ≥ 95% of words get a gap sentence
- every event has a hint

7. **Exact counts:** `VocabularyTest` asserts the word and category totals, so update them.

## Learning system (all in `learning/`, no Minecraft code)

- **Mastery:**
  - Leitner box 0–5. Review gaps: 2 min, 5 min, 20 min, 1 day, 3 days, 7 days.
  - A correct answer moves the word up a box only if it was nearly due, so cramming doesn't count.
  - A wrong answer drops it to box 1, or 0.
  - States: Familiar = box ≥ 2 and ≥ 3 correct. Mastered = box ≥ 4, correct on ≥ 2 different days, ≥ 80% accuracy.
- **Scheduler:** weighted random pick of discovered words.
  - Learning and overdue words count more; mastered words come back when due.
  - No word repeats within 5 questions or 10 minutes.
- **QuestionPlanner:** chooses the question kind from a weight table per level × word state, then decides "typed or choice". A word's first question is always easy for its level.
- **Forms:**
  - Grammar forms for a word, which forms to ask at each level, and plausible wrong answers (the word's other forms plus typical learner mistakes).
  - Gap sentences come from each word's example sentence, matching whole words only.
- **AnswerCheck:**
  - normalizes case and punctuation
  - accepts `|` alternatives
  - counts as "almost" (accepted with a hint): a missing or wrong article, missing å/ä/ö, or one typo in words ≥ 5 letters

## Persistence

- **Location:**
  - `.minecraft/config/svenskacraft/settings.json`
  - `.minecraft/config/svenskacraft/progress-<lang>.json`
- **Safe writes:** temp file + atomic move, with a `.bak` copy that's read if the main file is broken.
- **When:** saves at most every 30 s, plus on leaving a world and on game exit.
- **Shared progress:** one progress file is shared by all worlds and servers.
- **Migration:** `LegacyMigration` moves the v1.0 `config/svenskacraft.json` on first start.

## Building

```bash
export JAVA_HOME=/path/to/jdk-25
./gradlew build                                   # → build/libs/svenskacraft-<version>+26.3.jar (runs tests)
./gradlew build -Pminecraft_version=26.2 -Pfabric_api_version=0.161.0+26.2   # 26.2 jar
./gradlew runClient                               # dev client
python3 tools/make_curseforge_pack.py <fabric-api-0.165.2+26.3.jar>   # CurseForge zip
```

## Gotchas (Minecraft 26.x specifics)

- **Names:** 26.x is unobfuscated (Mojang names), so there's no Yarn and no remapping. Use `javap` on `~/.gradle/caches/fabric-loom/26.3/minecraft-merged.jar` to confirm signatures; many APIs changed from 1.21.
- **GUI:**
  - Screens override `extractRenderState(GuiGraphicsExtractor, ...)`, not `render`.
  - Text uses `g.text(...)` / `g.centeredText(...)`, items `g.item(...)`.
  - Colors must be ARGB with alpha, e.g. `0xFFFFFFFF`, or the text is invisible.
  - The game's screen is `mc.gui.screen()` / `mc.gui.setScreen()`; the HUD is `mc.gui.hud` (titles, action bar, chat).
- **Chat closes itself right after running a command.** Commands that open screens must set `pendingScreen` and let the tick loop open them.
- **Sound while paused:** a singleplayer pause stops every sound except music and UI. `AudioPlayer` plays the voice on the UI channel while a pausing screen is open, otherwise on VOICE.
- **Pickup mixin:** `handleTakeItemEntity` runs twice, once on the network thread and once on the main thread; only the main-thread call is used.
- **Font:** Minecraft's font can't draw emoji such as 🔊 or 🇸🇪; use ♪ ★ ✔ ✘.
- **Data:** Gson 2.14 supports records; missing JSON fields become null, so normalize them.
- **Limits of a client mod:** it can't change server state. Items can't be deleted on servers (they're thrown), and rewards are cosmetic or shields, not items.

## Testing

- **Unit tests:** 56 JUnit tests cover data integrity, mastery, scheduler, planner, forms and gap sentences, answer checking, daily goal, saving and migration, and milestones.
- **In-game:** verified in a headless dev client (Xvfb + software Vulkan) on 26.3 and 26.2: discovery, all question kinds, levels, journal, practice, tooltips, safety gate, pause, and migration.
- **Not verified:**
  - Audio playback (the test machine has no sound device).
  - Lunar Client, the CurseForge app import, and real Windows or Mac hardware.

## Ideas / backlog

- **Grammar:** word-order and short-sentence building exercises; more verbs and adjectives (stor/liten, varm/kall); "Vad är det här?" for blocks you look at.
- **More languages:** add `languages/<code>/` plus a Piper voice. Progress is already per language.
- **Interface:** Swedish menus at Advanced (only the quiz and word details are Swedish now).
- **Testing:** a placement test that suggests a level.
- **Server play:** an optional server-side companion mod for real item rewards.
