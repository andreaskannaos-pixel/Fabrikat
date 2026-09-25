# Fabrikat

A small Factorio-style factory game in one HTML file. It runs in Chrome with no install, so it works on a school Chromebook.

## Play

- **From the repo:** download `index.html`, open the Files app, and double-click it (it opens in Chrome). Works offline.
- **GitHub Pages:** repo Settings → Pages → deploy from branch, folder `/ (root)`. Then open the Pages URL.

Progress saves in the browser automatically.

## Controls

| Key | Action |
| --- | --- |
| `1`–`9` | Pick a building, click to place. Drag to lay belts (they turn corners). |
| `R` | Rotate |
| `X` / right-click (two-finger click, Alt+click) | Remove |
| `Q` | Copy the building under the cursor |
| Click ore | Mine by hand |
| `E` | Inventory and crafting |
| `C` | Company: research, upgrades, land, market, goals |
| `WASD` / arrows | Move camera |
| Scroll, `+` `-` | Zoom |
| `H` | Help |

## Economy

Everything runs on money. The **Money Generator** turns items into cash, and each item's price comes from its recipe: the value of its inputs plus the work that went into it. Processing an item always sells for more than its inputs. Ore sells for about $1, circuit boards for about $90, quantum processors for about $5,000, and dark matter for about $55,000.

- **Generator models:** Mk. I (raw only) → Mk. II (processed) → Mk. III (components) → Industrial Money Converter → Financial Singularity (anything). Each model is faster and pays out more.
- **Company panel (`C`):** research (13 techs, from Automation to Experimental Physics), factory upgrades, cosmetics, land (a 5×5 grid of plots with rare zones at the edges), and a market to buy buildings.
- **Goals:** money milestones from First Paycheck ($100) to Industrial Empire ($1B), plus six challenges.
- **Machines:** Assembler, Chemical Plant and Fabricator, with 16 raw resources and about 45 craftable items.

- Drills drop ore into whatever their chute faces.
- Belts feed any building they point into.
- Inserters move items from the building behind them into the one in front.
- Furnaces need coal. Assemblers craft the recipe you set.
