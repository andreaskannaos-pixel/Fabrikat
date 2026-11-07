# SvenskaCraft – learn Swedish in Minecraft

Client-side Fabric mod for **Minecraft Java 26.3**. Only you install it; it works in singleplayer and on any server.

You discover Swedish words by playing: picking things up, looking at animals, jumping, eating, sleeping. Short quizzes bring words back at the right time until you've mastered them, and a journal keeps your whole vocabulary with pronunciation and example sentences.

![discovery](docs/discovery-animal.png)
![journal](docs/journal-words.png)

## What's in it

- **116 words in 9 categories:** animals, food, nature, household objects, tools, materials, numbers, verbs, everyday phrases. Every word has an example sentence and natural Swedish voice.
- **Discovery:**

  | Category | How you discover it |
  |---|---|
  | Items | Pick them up |
  | Numbers 1–10 | Pick up that many items at once |
  | Animals | Look at one from close by |
  | Verbs | Do the action: walk, run, jump, swim, eat, sleep, dig, build |
  | Phrases | Moments in the game: *Hej!* when you join, *God natt!* at night, *Tack!* when trading, *Aj!* when hurt, *Hjälp!* at low health |

- **Word states:**

  | State | Meaning |
  |---|---|
  | New | Not found yet |
  | Learning | Recently found, or answered wrong |
  | Familiar | Answered correctly 3+ times |
  | Mastered | Consistently right, on at least 2 different days |

- **Smart repetition:**
  - Difficult and overdue words come back more often; mastered words return now and then to check you still know them.
  - The same word never appears within 5 questions or 10 minutes.
  - Review gaps: 2 min → 5 min → 20 min → 1 day → 3 days → 7 days.
- **Journal (press J):**
  - Words by category, with icon, Swedish, English, state, and a ♪ button to hear the word.
  - Click a word to see its example sentence (♪ to hear it) and its review stats.
  - An overview shows completion per category and your milestones.
- **Milestones:** first word, 10/25/50/100 words, first familiar and first mastered word, 10/25/50 mastered, answer streaks, each completed category. Big milestones give a **shield** (max 3) that blocks your next quiz penalty.

![overview](docs/journal-overview.png)
![quiz](docs/quiz-wrong.png)

## Settings (Journal → Settings, or `/svenska settings`)

![settings](docs/settings.png)

| Setting | Options |
|---|---|
| Learning intensity | **Relaxed**: 1 question every 15 min, no reminders. **Balanced**: 2 questions every 10 min. **Immersive**: 3 questions every 5 min, frequent hotbar reminders |
| Translations | **Always** · **On demand**: English hidden until you click "Visa engelska", hover the chat line or select the word · **Never**: Swedish and pictures only; quizzes use pictures as answers |
| Voice | **Off** · **Discovery only**: a new word is spoken once, short reactions in quizzes, ♪ buttons · **Manual playback**: only when you press ♪ |
| Voice volume | 0–100% (Minecraft's Voice/Speech slider also applies) |
| Quiz penalty | **None** · **Small**: lose 1 item · **Survival**: lose a whole stack |
| Quiz every / answer time | Override the intensity's interval; 15–60 s per question |

![picture quiz](docs/quiz-pictures.png)

### Audio design

- A word is spoken **once**, when you first find it. Picking it up again never triggers speech.
- Quiz audio is optional (♪ Lyssna). Reactions are short and varied (*Rätt! Snyggt! Precis! Bra! Perfekt!* / *Inte riktigt. Nästan. Hoppsan.*), never the same line twice in a row.
- A single voice channel: clips never overlap. ♪ replaces what's playing; new-word clips wait their turn.
- Everything else is subtle chimes, not speech.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft **26.3**.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) (26.3) and [`dist/svenskacraft-2.0.0.jar`](dist/svenskacraft-2.0.0.jar) in `.minecraft/mods`.

**Upgrading from 1.0:** your learned words, points and settings are moved over automatically the first time you start the game. Old words are not spoken again.

## Commands

| Command | |
|---|---|
| `/svenska journal` (or **J**) | Open the journal |
| `/svenska settings` | Settings |
| `/svenska quiz` | Quiz now |
| `/svenska interval <minutes>` | Time between quizzes |
| `/svenska penalty on\|off` | Survival / no penalty |
| `/svenska on\|off` | Turn the mod on/off |
| `/svenska stats` | Progress summary |

## Saving

- **Location:**
  - `.minecraft/config/svenskacraft/settings.json`
  - `.minecraft/config/svenskacraft/progress-sv.json`
- **Shared progress:** it's shared by all worlds and servers. You're learning, not the world.
- **Safe writes:** files are written to a temp file and swapped in, with a `.bak` copy of the previous version. If a file is damaged, the backup is loaded.
- **When:** at most every 30 s, and when you leave a world or quit.

## Limitations

- **On servers, wrong answers throw items on the ground instead of deleting them.** A client mod can't delete items there because the server owns your inventory. Shields and "None" work everywhere.
- **Milestone rewards are shields and notifications, not items,** for the same reason.
- **Some discoveries are approximate:**
  - Animals need line of sight within 10 blocks.
  - "Bygga" (build) is detected when a held block stack shrinks while you hold right-click.
  - Numbers come from how many items you pick up at once; high numbers are rarer.
- **"Never" translations:** verbs and phrases have no clear picture, so their quiz shows the icon and asks for the Swedish word.
- **Short review gaps use real time,** so time offline also counts toward when a word is due.
- **Voice:** a synthetic voice (Piper NST, CC0). It was the clearest of the three Swedish Piper voices in a speech-recognition test, but it isn't a human recording.

## For developers

```bash
./gradlew build     # Java 25; runs unit tests → build/libs/svenskacraft-2.0.0.jar
./gradlew test      # unit tests only (vocabulary data, mastery, scheduler, saving, migration, milestones)
```

```
src/main/resources/assets/svenskacraft/
  vocab/categories.json      categories (id, icon, English name)
  vocab/concepts.json        language-neutral words: meaning, category, icon, how they're discovered
  languages/sv/pack.json     Swedish UI text, category names, voice reactions
  languages/sv/words.json    Swedish word + example sentence per concept
  sounds/sv/...              generated voice clips
src/main/java/svenskacraft/
  vocab/ lang/               data loading
  learning/                  word states, scheduler, milestones (no Minecraft code, unit tested)
  progress/ settings/        saving, migration
  discovery/ audio/ ui/      in-game parts
```

**Add a word:**
1. Add an entry to `concepts.json` (`items`, `entities`, `events` or `amount`).
2. Add its Swedish text to `languages/sv/words.json`.
3. Regenerate the voice:

```bash
python3 tools/generate_voices.py sv sv-se-nst-medium.onnx
```

**Add a language:**
1. Create `languages/<code>/pack.json` and `words.json`.
2. Generate its clips with a Piper voice for that language.
3. Set `"language"` in `settings.json`.

Progress is saved per language.

Voice: Piper `sv-se-nst-medium` by KBLab / National Library of Sweden (CC0).
