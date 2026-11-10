---
name: ui-libraries
description: Pick and use a UI component or motion library instead of hand-rolling generic UI - Bencho (interactive blocks, micro-interactions), BoardUI (React design system, charts), Transitions.dev (copy-paste transitions), Beautiful UI (AI/chat interface primitives). Use when a UI needs a specific component, interaction or transition and the result should not look like default AI output.
---

# UI libraries

Status (2026-11-10): all four sites were blocked by this environment's network policy, so the facts below come from npm metadata and web search, not the sites' own docs. Marked **[verified]** or **[unverified]**. Read the live docs (and licence) before depending on any of them; if a site is still blocked, say so rather than guessing an API.

| Library | Use for | Stack | Notes |
|---|---|---|---|
| Bencho — https://bencho.dev | Interactive blocks and micro-interactions: thinking loader, jelly tabs, dropdown, confetti, slide-to-confirm, toast stack, time scrubber | React [unverified] | Tune on a "bench", copy code. Freemium: free blocks reported MIT, Pro blocks paid [unverified]. npm `bencho` is an unrelated benchmarking tool — do not install it. |
| BoardUI — https://boardui.com | Full React design system: ~100 components incl. data charts, templates, 400+ tokens, aimed at agentic products [unverified] | React | CLI copies source into the repo (shadcn-style): `npx boardui add <component>`, e.g. `npx boardui add data-table`. npm `boardui` 0.5.9, MIT, docs https://www.boardui.com/docs/cli [verified]. Pro templates paid [unverified]. |
| Transitions.dev — https://transitions.dev | Essential web-app transitions, copy-paste | Likely CSS/JS snippets [unverified] | No npm package [verified]. |
| Beautiful UI — https://beautifului.dev | AI-interface primitives: chat, streaming text, thinking traces, approval cards, tool-call chips, task rows, prompt bar, diff tables | React, copy-paste [unverified] | Reported MIT [unverified]. npm `beautiful-ui` is not confirmed to be this project — do not install it on that assumption. |

## Choosing

- One interaction that should feel great → Bencho.
- A whole app shell, tables, charts → BoardUI.
- Enter/exit, page, list, modal motion → Transitions.dev.
- Anything chat/agent-shaped → Beautiful UI.
- Do not mix two component systems in one screen. One library for structure, at most one for motion.

## Using in a React/Tailwind project

1. Read the component's docs page and licence. Free/MIT only unless the user has bought Pro.
2. Add via the library's CLI or copy the source into the project's component folder.
3. Re-skin to the project's tokens (see `ui-taste`, `design-md`). Never leave library defaults as the final look.
4. `browser-check`, then `ui-audit`.

## Using in a vanilla project (this repo: single-file `index.html`, no build)

These are React libraries; do not add React or a build step for them. Treat them as references:

1. Study the component's behaviour: states, timing, easing, what moves and what stays.
2. Re-implement in plain HTML/CSS/JS with the project's existing helpers and tokens: `transform`/`opacity`, 120–250 ms, listed transition properties (never `transition: all`), `prefers-reduced-motion` fallback.
3. Write original code from the observed behaviour; only paste source when its licence allows it and keep the attribution.
4. `browser-check`, then `ui-audit`.
