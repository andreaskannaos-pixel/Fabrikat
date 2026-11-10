---
name: browser-check
description: Open the UI in a real browser with playwright-cli to inspect, screenshot and test it. Use after any UI change, to reproduce a visual or interaction bug, to check console errors, responsive widths, dark mode or reduced motion, or whenever a claim about how the UI looks needs evidence.
allowed-tools: Bash(playwright-cli:*) Bash(python3 -m http.server:*)
---

# Browser check (playwright-cli)

microsoft/playwright-cli (Apache-2.0): token-efficient CLI over Playwright. Verified here with `@playwright/cli` 0.1.24 and the pre-installed Chromium.

## Setup

```bash
command -v playwright-cli || npm install -g @playwright/cli@latest
```

Never run `playwright install`. `file:` URLs are blocked: serve over HTTP.

```bash
python3 -m http.server 8765 >/dev/null 2>&1 &     # from the repo root
playwright-cli open http://localhost:8765/index.html
```

## Loop

```bash
playwright-cli snapshot                 # a11y tree with element refs (e12…), saved under .playwright-cli/
playwright-cli find "New Game"          # locate a ref without reading the whole snapshot
playwright-cli click e12                # also: fill e5 "text" --submit, hover, press Enter, drag e2 e8
playwright-cli console error            # must be clean (favicon 404 is noise)
playwright-cli requests                 # failed network calls
playwright-cli eval "getComputedStyle(document.body).fontFamily"
playwright-cli screenshot --filename=<scratchpad>/shot.png   # then Read the image
playwright-cli close
```

Prefer `snapshot`/`find`/`eval` over screenshots for logic; use screenshots for visual judgement.

## Visual pass

```bash
for s in "1440 900" "1024 768" "768 1024" "390 844"; do
  playwright-cli resize $s; playwright-cli screenshot --filename=<scratchpad>/w${s%% *}.png
done
playwright-cli set-color-scheme dark;     playwright-cli screenshot --filename=<scratchpad>/dark.png
playwright-cli set-reduced-motion reduce  # animations must stop or shorten
```

Check: no overflow or clipped text, equal edges, focus ring visible (`press Tab`), hover/pressed states, empty/error states.

## Rules

- Screenshots go to the scratchpad, not the repo. `.playwright-cli/` is git-ignored.
- Canvas content is invisible to `snapshot`: use `eval` on game state and screenshots.
- Stop the server and `playwright-cli close` when done (`pkill -f "http.server 8765"`).
- Report what was actually observed; if a check was not run, say so.
- More: `playwright-cli --help`; mocking (`route`), storage state, tracing, video and test generation are in the upstream skill (`playwright-cli install --skills`).
