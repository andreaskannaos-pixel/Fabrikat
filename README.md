# Fabrikat

A small Factorio-style factory game in one HTML file. It runs in Chrome with no install, so it works on a school Chromebook.

## Play

- **From the repo:** download `index.html`, open the Files app, and double-click it (it opens in Chrome). Works offline.
- **GitHub Pages:** repo Settings → Pages → deploy from branch, folder `/ (root)`. Then open the Pages URL.

Progress saves in the browser automatically.

## Controls

| Key | Action |
| --- | --- |
| `1`–`9` | Pick a common building, or use the build palette (Mining, Logistics, Production, Chemistry, Power, Storage & Trade). Click to place; drag to lay belts. |
| `R` | Rotate |
| `X` / right-click (two-finger click, Alt+click) | Remove |
| `Q` | Copy the building under the cursor |
| Click ore | Mine by hand |
| `E` | Inventory and crafting |
| `C` | Company: contracts, goals, research, upgrades, land, market, blueprints |
| `P` | Factory statistics |
| `B` | Save a blueprint (drag a box) |
| `M` | World map |
| `T` | Research |
| `G` | Guide |
| `WASD` / arrows | Move camera |
| Scroll, `+` `-` | Zoom |
| `O` | Bottleneck overlay |
| `H` | Help |
| `Space` | Pause (speed 1×/2×/4× in the top bar) |

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
- Furnaces and machines touching a Money Generator sell their output into it directly (Mk. II or better for plates).

## Factory decisions

- **Fluids and pipes:** water, crude oil, fuel, chemicals, lubricant, polymer, heavy residue and coolant move in barrels on belts or through pipes. A pipe network holds one fluid. Short networks move 20 units/s; past 20 pipes the flow drops, and each pipeline pump adds 15/s. Machines take fluids in from their sides and push fluid output out of the front (the arrow). Oil drills facing a pipe pump 3 units per cycle instead of one barrel. Storage tanks buffer fluid and swap it with barrels via inserters. Water wells need no power.
- **Byproducts:** refining fuel and polymer also makes heavy residue, which leaves the back of the chemical plant. If nothing takes it, the plant stops. Store it, burn it in steam generators, crack it into fuel or carbon, or sell it cheaply.
- **Alternative recipes** (ALT in a machine's recipe list): steel smelted from 5 plates or made from 3 plates and fuel; fuel from oil, from coal and chemicals, or from cracked residue; silicon-heavy or copper-heavy processors; cast-iron pipe before steel. Each machine panel lists every way to make its item.
- **Bottleneck overlay (`O`):** red = bottleneck, yellow = under-supplied, blue = over-supplied, white = idle, green = balanced. A machine's panel shows its input, output, power and idle percentages and the main cause.
- **Power grid:** the HQ supplies 750 kW. Steam generators are cheap but need water and fuel, pollute, and take 5 s to spin up. Solar follows a 12-minute day/night cycle. Geothermal is steady but volcanic-only. Machines draw 3× power for a moment when they start, so battery banks matter for spikes and the night. The Stats panel shows peak demand, sunlight, stored energy and pollution.
- **Crew:** working machines need crew (0.5 for drills and furnaces up to 2 for fabricators). The HQ gives 50, plus 1 per 100 city workers. Too little crew slows every machine, so the worker/soldier slider trades army size against factory capacity.
- **Overclocking:** run a drill or machine at 150% or 200%, paying much more power and faster wear.
- **Modules:** two slots per drill or machine for speed, efficiency and productivity modules. Productivity sometimes makes a free extra batch.
- **Regional bonuses:** mountains mine 25% faster, the arctic halves machine wear, deserts boost solar, and volcanic land allows geothermal power.
- **Dynamic prices:** selling a lot of one item floods the market and lowers its price, which recovers over a few minutes. Booms raise prices and make contracts for those goods pay 35% more, ask for twice as much and come twice as often.
- **World events:** booms, shortages, an energy crisis, dock strikes, droughts, heatwaves, breakthroughs and windfalls, every few minutes (can be turned off in Settings).
- **Military supply:** units use factory goods every minute (iron plates, steel, fuel, chemicals, aluminium, sensors). Short supply weakens the whole army.
- **Factory efficiency score:** Mining, Smelting, Assembly, Logistics and Power percentages in Stats. Click a row to jump to the worst spot.
- **Stages:** Workshop → Industry → Electronics → Megafactory → Global Industry → Rocket Age → Exotic Industry. Each adds a problem: emissions tax, fabricator batches ruined by brownouts, faster wear, faster market flooding, rocket launches powered from battery banks, and exotic fabrication that needs coolant. The Exotic stage adds the 25 MW Singularity Reactor and transmutation of common ore into rare ore.
- **Factory milestones:** 1,000 steel/min, 10 MW, 50 machines working, 100,000 goods sold, a contract finished without market purchases and more. Rewards: money, research credit, blueprint slots and cosmetics.
- **Loans:** Working Capital (small, 10 min), Expansion Loan (large, cheap, 60 min, from the Industry stage) and Emergency Credit (instant, very expensive). Above a 25% debt ratio new loans cost more. A late loan doubles its interest and takes 25% of revenue until it is paid.

## Nation layer

Your factory pays for cities, cities grow people, and people become workers or soldiers. Soldiers take territory, and territory sends new resources back to the factory.

- **Campaign:** 10 missions teach the loop, from your first furnace to surviving an invasion. Locked menus (World, Cities, Military) show a lock and say which mission opens them; Settings can skip ahead.
- **World map (`M`):** 61 provinces across plains, forest, desert, mountains, volcanic land and arctic, shared with 5 computer-run nations. Captured provinces send their resource to your factory every 10 seconds, into an Import Depot if you build one.
- **Cities:** Mk. I to Mk. V, bought with money and materials. Each province has a few city slots.
- **Population:** one slider splits people between workers (taxes, plus up to +100% factory speed) and soldiers. Peacetime, Mobilized and War Economy set how far you can push it and what it costs.
- **Military:** infantry, mechanized, artillery, armor, air wings and special forces. Attack any neighbouring province, or land from a port with transport ships. Forts and terrain strengthen defense.
- **Diplomacy:** relations from -100 to +100; alliances, peace, war, gifts, trade routes, requests for allied troops and shipping raids.
- **Town defenses:** walls (wooden palisade, stone, concrete), watchtowers (early warning), up to 3 bunkers, tank traps and minefields, built per province on the World map. They stack with forts and terrain, and computer nations fortify their own towns over time.
- **Strategic:** five rocket types and SAM sites with visible coverage circles and interception.
- **Research:** five tabs (Industry, Civilization, Military, Strategic, Naval). Advanced research costs materials as well as money.
- **Guide (`G`), Settings** (quality presets, FPS limit, particle and effect density, animation and map detail, performance mode) and a News feed.

It's single-player: every other nation is computer-controlled, and there is no online chat.

## Look and feel

- **One colour system:** every machine is a coloured body with a dark outline, a simple centre icon and a status light (green working, amber waiting, red problem, grey idle). Mining is ochre, smelting red-orange, production blue, chemistry purple, power yellow, logistics charcoal, storage green and money gold.
- **Terrain:** warm tan ground with a faint build grid. Ore shows as solid deposits with a few clean nuggets, forests as tree clusters and oil as dark pools.
- **Belts:** charcoal with coloured edges by tier (orange, red, blue, purple) and moving direction arrows.
- **HQ:** the Fabrikat Industries building shows its revenue and model above it.
- **Selection:** clicking a machine dims the rest of the factory and highlights the belts, inserters, machines and pipes connected to it. Its panel leads with input and output percentages, power, wear and the main cause; recipe, modules and diagnostics are in fold-out sections.
- **HUD:** money on the left, power in the middle, workforce and game speed on the right, with everything else under More. The mission card folds up to one line.
- **World map:** uses the same warm palette.

## Quality of life

- **Introduction:** a new game opens with one of three characters (the Overseer, Baron Cogsworth or the Voice in the Chimney) talking you through the premise in typed subtitles. Replay it from the Menu.

- **Pause and speed:** `Space` pauses; the top bar switches between 1×, 2× and 4×.
- **Offline earnings:** when you come back, your nation earns half its usual income for the time you were away, up to 8 hours.
- **Save codes:** Menu → Export save code gives a text code you can paste into Import on another computer (for example to move between school and home).
- **Sound effects:** synthesized in the browser, with an on/off switch and volume in Settings.
- **Difficulty:** Easy, Normal or Hard computer nations, in Settings.
- **Smart copy:** `Q` on a machine copies its recipe too, so new copies start with the same recipe.
- **Touchscreens:** two-finger drag to pan and pinch to zoom, on both the factory and the World map.
