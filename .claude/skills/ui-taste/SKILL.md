---
name: ui-taste
description: Design direction before writing UI. Use when building, restyling or polishing any screen, menu, panel, HUD or page, or when the user says the UI looks generic, templated or "AI-made". Sets one visual system and blocks the usual AI-slop defaults.
---

# UI taste

Distilled from Leonxlnx/taste-skill 2.5.1 (MIT) and its anti-slop lists. Decide direction first, code second.

## 1. Commit to a direction (write it down in 5 lines before coding)

- Subject: what this UI is for and who uses it.
- One system: 1 type family pair, 1 accent colour, 1 radius scale, 1 spacing scale, 1 shadow style. No second system.
- Density: sparse (marketing) / medium (app) / dense (game HUD, dashboards).
- Motion budget: which 1–2 moments animate; everything else is quiet.
- Reference: a `DESIGN.md` (see `design-md` skill) or the existing tokens in the project. Existing project tokens always win.

## 2. Hard rules

- Equal left and right content edges. One alignment grid.
- One idea per section/panel. No second heading peeking into the first screen.
- Type does the hierarchy: size and weight contrast, not boxes, pills and badges.
- No cards inside cards inside cards. Max one container level; use spacing and dividers.
- Plain copy. Say what the thing is. No "Elevate / Seamless / Unleash", no fake stats, no lorem.
- Real assets: real icons, real screenshots, real data. Never an empty placeholder ground.
- Every state exists: hover, active/pressed, focus-visible, disabled, loading, empty, error.
- Micro-interactions are small: lift 2px, scale 1.02–1.03, 120–200 ms, `transform`/`opacity` only, honour `prefers-reduced-motion`.
- Check at 1440, 1024, 768, 390 wide. Nothing overflows, nothing unreadable.

## 3. Slop checklist (remove on sight)

- Purple/indigo gradient on dark, glow blobs, glassmorphism by default.
- Centered hero + three equal icon cards + CTA band.
- Everything `rounded-2xl` with the same soft shadow.
- Emoji as icons. Decorative tags, status dots, "system" labels with no function.
- Repeated left-text/right-image rows.
- Inter/system font at one weight for everything.
- Uniform tight spacing; no rhythm between sections.

## 4. Finish

Run `browser-check` (screenshots at the four widths, console clean), then `ui-audit`.

## Full upstream skill (optional, heavy)

40 MB, needs Node 22+ and Chrome, adds a Stop-hook gate; aimed at company websites, not app/game UI.

```bash
npx skills add https://github.com/Leonxlnx/taste-skill --skill "design-taste-frontend"
```
