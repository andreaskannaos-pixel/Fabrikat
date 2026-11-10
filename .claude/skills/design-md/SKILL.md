---
name: design-md
description: Pull a real-world design system reference (DESIGN.md) from VoltAgent/awesome-design-md and build UI against it. Use when the user wants UI "like Stripe/Linear/Notion/…", needs a concrete design system, tokens or visual reference, or when a new UI has no defined look yet.
argument-hint: <brand>
---

# DESIGN.md references

DESIGN.md (Google Stitch format) = one markdown file with YAML tokens (colors, typography, spacing, radius, components) plus prose rules. Source: VoltAgent/awesome-design-md (MIT), 84 brands.

## Get one

```bash
# preferred here: git works through the proxy, getdesign.md may not
git clone --depth 1 https://github.com/VoltAgent/awesome-design-md .design-refs/awesome-design-md
ls .design-refs/awesome-design-md/design-md            # brand folders
cat .design-refs/awesome-design-md/design-md/<brand>/DESIGN.md

# alternative where getdesign.md is reachable
npx getdesign@latest add <brand>
```

`.design-refs/` is git-ignored. Do not commit the clone.

Brands (folder names): 1win airbnb airtable aljazeera apple binance bmw bmw-m bugatti cal claude clay clickhouse cohere coinbase composio cursor dell-1996 dji elevenlabs expo ferrari figma framer hashicorp hp ibm intercom jobright kraken lamborghini linear.app lovable mastercard mercury meta minimax mintlify miro mistral.ai mongodb mydealz nerdwallet nike nintendo-2001 notion nvidia ollama opencode.ai oracle oura pinterest playstation posthog raycast renault replicate resend revolut runwayml sanity sap sentry shopify slack spacex spotify starbucks stripe supabase superhuman tesla theverge together.ai uber vercel vodafone voltagent warp webflow (list may have grown: `ls`).

## Use it

1. Pick by fit, not fame: dense tool UI → linear.app, raycast, warp; data/finance → stripe, mercury, kraken; playful/retro → nintendo-2001, dell-1996, playstation; editorial → notion, theverge.
2. Read the whole file. Extract into the project's own tokens (CSS custom properties): colours, type scale, spacing, radius, shadow, component rules.
3. Adapt, don't clone: these are "inspired-by" analyses. Keep the structure and ratios; change brand names, logos and proprietary fonts (substitute an open font with similar metrics). Never ship another company's trademarks.
4. If the project already has tokens or a `DESIGN.md`, that wins; use the reference only to fill gaps.
5. For a lasting project decision, write a project-specific `DESIGN.md` at the repo root (ask first) rather than copying a brand's file verbatim.
6. Verify with `browser-check`.
