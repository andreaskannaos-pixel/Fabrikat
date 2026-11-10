---
name: ui-audit
description: Audit UI code against the Vercel Web Interface Guidelines (accessibility, focus, forms, animation, typography, performance, touch, dark mode, i18n, anti-patterns). Use when asked to review UI, check accessibility, audit design or UX, or before shipping a UI change.
argument-hint: <file-or-pattern>
---

# UI audit

Source: vercel-labs/web-interface-guidelines `command.md` (MIT). Same method as the `web-design-guidelines` skill in vercel-labs/agent-skills.

## Steps

1. Rules: read `guidelines.md` in this folder (snapshot @ `434b7f9`). If the network allows, prefer the live copy:
   `git clone --depth 1 https://github.com/vercel-labs/web-interface-guidelines` and read `command.md`; if it differs, update the snapshot.
2. Scope: the files or pattern given; otherwise the files changed on the branch (`git diff --name-only`). For a huge single file (e.g. `index.html`), grep per rule family instead of reading it whole.
3. Check every rule family. Rules written for React/Tailwind map to plain HTML/CSS/JS: `focus-visible:ring-*` → a `:focus-visible` style, `onClick` on a div → `addEventListener` on a non-button, etc.
4. Output exactly the format at the end of `guidelines.md`: grouped by file, `file:line - finding`, terse, no preamble. `✓ pass` for clean files.
5. Fix only if asked. After fixing, re-run and confirm with `browser-check`.

## Notes

- Report real findings only; skip rules that cannot apply (no forms → no form rules).
- Canvas-rendered game UI has no DOM semantics: audit the DOM overlay (menus, panels, dialogs) and note the canvas as out of scope.
