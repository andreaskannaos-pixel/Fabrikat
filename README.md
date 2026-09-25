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
| `C` | Company: contracts, goals, research, upgrades, land, market, blueprints |
| `P` | Factory statistics |
| `B` | Save a blueprint (drag a box) |
| `WASD` / arrows | Move camera |
| Scroll, `+` `-` | Zoom |
| `H` | Help |

## Economy

Everything runs on money. The **Money Generator** turns items into cash, and each item's price comes from its recipe: the value of its inputs plus the work that went into it. Processing an item always sells for more than its inputs. Ore sells for about $1, circuit boards for about $90, quantum processors for about $5,000, and dark matter for about $55,000.

- **Generator models:** Mk. I (raw only) → Mk. II (processed) → Mk. III (components) → Industrial Money Converter → Financial Singularity (anything). Each model is faster and pays out more.
- **Company panel (`C`):** research (13 techs, from Automation to Experimental Physics), factory upgrades, cosmetics, land (a 5×5 grid of plots with rare zones at the edges), and a market to buy buildings.
- **Goals:** money milestones from First Paycheck ($100) to Industrial Empire ($1B), plus six challenges.
- **Machines:** Assembler, Chemical Plant and Fabricator, with 17 raw resources and about 50 craftable items.

## Systems

- **Contracts:** five companies (Fabrik Industries, Nordex Energy, Titan Construction, Syntek, Aegis Aerospace) post delivery and supply-rate contracts sized to your factory. Finishing one pays cash and reputation. Reputation level 2 unlocks a company perk. A missed deadline only costs reputation.
- **Statistics (`P`):** revenue, spending and profit per minute with a 30-minute revenue chart; items made, used and sold per minute; each machine's state (working, waiting, output full, idle) and condition; belt use, jams, storage and train routes.
- **Problems:** alerts for furnaces out of coal, shortages, full outputs, belt congestion, worn machines, generators rejecting items and contracts about to expire. Click an alert to jump to the building.
- **Maintenance:** machines wear from 100% down to 50%, and their speed drops with it. Repair them for cash or a replacement part, or build a Maintenance Bay that repairs automatically.
- **Regions:** Forest (wood), Desert (sand, oil, bauxite), Mountains (iron, coal, tungsten, cobalt), Volcanic (rare earth, platinum, uranium) and Arctic (lithium, titanium).
- **Logistics:** splitters, and train stations that ship up to 50 items per trip between any two stations.
- **Blueprints:** save a group of buildings, rotate and place copies (shift+click buys any missing buildings), and share them as text codes.
- **Experimental research:** unknown technologies you unlock with materials instead of money: Magnetic belts, Machine Overclocking and Deep Core Drilling.

- Drills drop ore into whatever their chute faces.
- Belts feed any building they point into.
- Inserters move items from the building behind them into the one in front.
- Furnaces need coal. Assemblers craft the recipe you set.
