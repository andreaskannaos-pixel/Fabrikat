---
name: image-to-code
description: Turn a design reference image (screenshot, mockup, generated concept, photo of a sketch) into faithful UI code. Use when the user supplies or points to an image and wants it built, or asks to "make it look like this".
---

# Image to code

Method from Leonxlnx/imagegen-skills `image-to-code` (MIT), trimmed. Order is fixed: inspect → extract → implement → compare. Never start coding from a glance.

## 0. Get the reference

- User-supplied image: use it as the spec.
- No image and an image generator is available (e.g. Higgsfield `generate_image`): only generate when the user asks for a concept; one large readable image per section, never one tiny board of many sections. Regenerate a section fresh rather than cropping.
- Unreadable detail: ask for / generate a closer image before coding. Do not guess.

## 1. Extract (write this down before any code)

- Structure: sections in order, grid, alignment, what has visual priority.
- Text: every readable string verbatim (headings, labels, buttons).
- Typography: family character (serif/sans/mono), size ratios between levels, weights, line count and wrapping, tracking.
- Spacing: gutters, section padding, gap between heading/sub/buttons, card padding. Ratios, not pixel OCR.
- Components: button shape/radius/fill vs outline/padding, inputs, cards, dividers, badges, icons.
- Colour: background, surfaces, text hierarchy, accent, borders, shadow tint. Sample real values when possible:
  `python3 -c "from PIL import Image; im=Image.open('ref.png').convert('RGB'); print(im.getpixel((x,y)))"`
- Unclear items: list them.

## 2. Implement

- Map the extraction to tokens first (CSS custom properties), then build.
- Copy-oriented: same layout logic, spacing rhythm, section order, type mood. "Faithful to", not "inspired by".
- Use the project's existing stack and components; no new framework for one screen.
- Ambiguity order: keep visible design language → layout/spacing logic → component family → only then the simplest faithful option.

## 3. No drift

Do not: swap a distinctive section for a generic row, tighten generous spacing, flatten type hierarchy, add nested boxes, replace the palette with defaults, or add elements the image does not show.

## 4. Compare

Use `browser-check`: screenshot at the reference's aspect/viewport, view it next to the reference, list differences (layout, spacing, type, colour), fix, repeat until only minor differences remain. Report what still differs.
