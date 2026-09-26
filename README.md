# Fabrikat

A small Factorio-style factory game in one HTML file. It runs in Chrome with no install, so it works on a school Chromebook.

## Play

- **From the repo:** download `index.html`, open the Files app, and double-click it (it opens in Chrome). Works offline.
- **GitHub Pages:** repo Settings → Pages → deploy from branch, folder `/ (root)`. Then open the Pages URL.

The game opens on a start screen: **New Game**, **Continue**, **Customization Shop** and **Character Editor**. Each game has its own save slot (up to 6), saved automatically in the browser. Continue lists them with date, stage and money. An old single save becomes the first slot. Menu → Main menu goes back to the start screen.

## Controls

| Key | Action |
| --- | --- |
| `1`–`9` | Pick a common building, or use the build palette (Mining, Logistics, Production, Chemistry, Power, Storage & Trade). Click to place. Drag to lay belts and pipes: belts turn corners, pressing on the end of an existing belt continues it, and the last belt of a drag turns into an adjacent machine. |
| `R` | Rotate |
| `X` / right-click (two-finger click) | Remove (a right-click without dragging; drag with `X` held as the tool to remove a line) |
| Right-drag | Free look: pan without moving the President; on release the camera glides back (Settings: glide, snap or stay) |
| `Esc` | Close the open tab, popup or tool; with nothing open, opens the Menu |
| `V` / `L` / `U` / `N` | Factory view / Cities / Military / Settings |
| Hold `Alt` | Show machine details: recipe icons, fluid ports, pipe flow, crew and camp labels |
| `Q` | Copy the building under the cursor |
| Click ore | Mine by hand |
| `E` | Inventory and crafting |
| `C` | Company: contracts, goals, research, upgrades, land, market, blueprints |
| `P` | Factory statistics |
| `B` | Save a blueprint (drag a box) |
| `M` | World map |
| `T` | Research |
| `G` | Guide |
| `WASD` | Walk as the President (Shift runs) |
| Arrows | Move the camera |
| `F` / `R` / `Q` | Draw weapon / reload / switch weapon |
| `K` | President: weapons, armour, Character Editor |
| Scroll, `+` `-` | Zoom |
| `O` | Bottleneck overlay |
| `H` | Help |
| `Space` | Pause (speed 1×/2×/4× in the top bar) |

## Economy

Everything runs on money. The **Money Generator** turns items into cash, and each item's price comes from its recipe: the value of its inputs plus the work that went into it. Processing an item always sells for more than its inputs. Ore sells for about $1, circuit boards for about $90, quantum processors for about $5,000, and dark matter for about $55,000.

- **Generator models:** Mk. I (raw only) → Mk. II (processed) → Mk. III (components) → Industrial Money Converter → Financial Singularity (anything). Each model is faster and pays out more.
- **Company panel (`C`):** research (13 techs, from Automation to Experimental Physics), factory upgrades, cosmetics, land (buy neutral provinces next to yours), and a market to buy buildings.
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
- **Crew:** working machines need crew (0.5 for drills and furnaces up to 2 for fabricators). The HQ gives 50, plus 1 per 100 city workers. Too little crew slows every machine, and every soldier you recruit leaves the factory.
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

## One world

- **One map:** the factory and the country share one procedural map of about 60 provinces with coasts and seas. You start with one province around your HQ. Buy neutral land next to yours, or take land from other nations. Zoom out (scroll, or `M`) for the whole map.
- **Territory types:** plains (farmland, faster growth), forest (timber, better wells), desert (strong solar, oil), mountains (faster mining), volcanic (geothermal, rare metals), arctic (low wear). Some provinces are coastal (ports), some hold an old factory whose machines come with the land, some a large town, some a rare deposit.
- **Small deposits:** ore comes in small scattered deposits. You get a warning at 85% mined and a notice when one runs out; the factory keeps running.
- **Villages:** houses, farms, barns, workshops, markets, wells, temples, town halls, small mines and trading posts joined by roads, with villagers walking between them and your machines. Owning a province makes its people your workers and soldiers. Pollution drives them away.
- **The President:** walk with `WASD` (Shift runs), click machines to inspect them and villagers to talk. `F` draws a weapon, `R` reloads, `Q` switches weapons, `K` opens weapons, outfits and armour. You have health; if you fall you wake at HQ and pay medical bills.
- **Combat on the map:** attacks send soldiers walking across the border; your garrison, your army camps and you fight them, and every kill weakens the attack. Bandits raid villages for a bounty. Supply trucks run from HQ to your border camps.
- **Blueprint market:** My blueprints, a Guild market of tested designs (in-game company, with price, rating, research, output, power and size) and Featured. Placing a blueprint lays out construction sites that are built from your inventory and chests; Order missing buys the rest.
- **Feedback:** problem tags over machines (for example OUTPUT BLOCKED, with the pipe spot marked in red), a pollution overlay (`O` twice), sunsets and night lights, a power reserve meter, a workforce warning with the output loss, and a card for each stage's new challenge.

## Late-game automation

- **Logistics hubs:** big storage nodes with Requested and Exported items. Couriers pathfind (A*) between hubs and move items from surplus to deficit.
- **Workforce manager:** there are no fixed crews. Villagers work in the factory until a job needs them: placing a blueprint drafts builders from the nearest village, a machine below 80% condition drafts an engineer, and both go back to work 3 seconds after their job ends. Crew size is 3 + 1 per hub + villages and population (up to 16, half of it engineers).
- **Builders:** blueprints become construction jobs. Drafted builders claim a job, fetch the building from the nearest hub, chest or your inventory, walk to the site and build it over a few seconds.
- **Engineers:** drafted per worn machine and based at the nearest Maintenance Bay, hub or HQ. They use a Repair Kit (bay, hub, then stock). Paying 1.5× in cash needs the **Emergency Requisitions** research; until then a machine without a kit shows a Missing Repair Kit warning and waits. Hand repairs from the machine panel always work.
- **Ammunition:** border camps use ammunition trucked from hubs or your stock. A dry camp fights at 35% strength.
- **Military units:** one soldier or tank sprite per army, with a live troop badge (1.5K), gains and losses, and an ammunition bar.
- **World generation:** layered-noise biomes with exclusive resources (uranium only in deserts, platinum and rare earth in volcanic land, lithium in the arctic, tungsten and cobalt in mountains), a generated world history, and points of interest in unclaimed land.
- **Health:** heavy pollution slows villagers and machines. Clinics stocked with medical kits protect a 14-tile radius.
- **Visuals:** belts autotile (straight, corners, T-junctions, crossings, end caps), warnings are pulsing icons over machines with details on hover, and night is a darkness layer that lights cut through: flickering furnaces, muzzle flashes and explosions light the dark.

See `docs/AUTOMATION.md` for the state machines, `docs/UI14.md` for the start screen, saves, unlocks, overlays and attack popup, and `docs/V18.md` for the tab manager, free look, audio and drill targeting, `docs/ENGINE.md` for the tech tree, ceasefire, object pools, spatial grid, interpolation and pathfinding.

## Interface

- **Hotkeys on the nav bar:** every button shows its key underneath (Factory V, World M, Cities L, Military U, Research T, Company C, Stats P, President K, Inventory E, Blueprint B, Overlay O, Guide G, Settings N, Menu Esc).
- **One tab at a time:** Factory, World, Cities, Military, Research, Company, Stats, President, Inventory, Blueprint, Overlay, Guide, Settings and Menu are mutually exclusive.
  - Opening one closes the others.
  - The open tab's button gets an orange underline and border.
  - Pressing the nav bar clears attack and buy popups.
- **President:** the President tab opens the President's stats, weapons and armour, with a Character Editor button in its header.
- **Layout:** panels never cover the nav bar.
- **Sound:** a cash-register chime for revenue, plus sounds for opening and closing tabs, placing and removing buildings, and failed placements.
- **Early game:**
  - Ore tiles in your starting province hold 4× more ore.
  - **Advanced Drill Targeting** (early Industry research): a drill whose tile runs dry keeps mining the same ore on one of the 8 tiles around it, shown by a dashed green line.

## Endgame and logistics

- **Science labs:** Research Labs unlocks labs and four science packs, all made in assemblers except Chemical science, which comes from chemical plants:
  - Automation (copper plate + gear)
  - Logistics (inserter + belt)
  - Military (ammunition, steel, brick)
  - Chemical (circuit boards, motors, chemicals)
- **Lab research:** late technologies are researched by labs. Each unit consumes one set of the packs it needs. Pick one in the tech tree and every lab works on it together.
  - Assembly Tuning is the starter lab technology.
  - Lab technologies include Steam Turbines, Oil Cracking, Freight Rail, Maritime Trade and Rocket Silo.
- **Rocket silo (3×3):** it builds a rocket part from one rocket control unit, one low density structure and one rocket fuel every 3 s. 50 parts make a rocket.
  - The first launch wins the game and opens **Exotic Industry**, an endless research: +10% machine speed per level, each level costing 50% more.
  - Every later launch pays $2M.
- **Freight rail:** drag rails like belts across your land, neutral land and rivers. Freight depots (2×2) send their train (4 wagons: 400 items or 2,000 fluid) to another depot on the same line.
  - A train leaves when full or 10 s after loading starts, accelerates to 12 tiles/s and unloads into the receiving depot.
  - A depot can carry items or fluids. Fluid depots take fluid from pipes at one end and put it into pipes at the other.
- **Fluids:**
  - Oil Cracking: crude oil → heavy oil → light oil (with water) → petroleum gas (with water).
  - Boilers make steam from water and coal, fuel or heavy oil. Each steam turbine gives up to 1.8 MW, and one boiler feeds two.
  - Every pipe network shows its pressure (20–300 kPa, plus 25 per pump). Pipes pulse red and machines show LOW PRESSURE when they are waiting on a nearly empty network.
- **Sea ports:** built on coastal land you own. Set up to 3 export deals with 4 overseas partners, each paying 25–60% over market in cash or rare resources (rare earth, titanium, uranium).
  - Ships sail off the map with 20 to 300 items.
  - Prices fall while you flood one partner with one item, and recover over time.
- **Rivers:** new worlds have rivers. Only rails, belts, pipes and wells go on them. A well on a river pumps twice as much water, and 50% more within 2 tiles of one.

See `docs/V17.md` for the architecture.

## Nation layer

Your factory pays for cities, cities grow people, and people become workers or soldiers. Soldiers take territory, and territory sends new resources back to the factory.

- **Campaign:** 10 missions walk through the whole game: First Steps (walk the President, mine, sell), Smelting Line, The Tech Tree, Many Hands (blueprints built by villagers), Beyond the Fence (zoom out and buy land), Your First City, Science! (labs), Arms and Borders, Take Ground and Survival. After that, the final objective is to launch a rocket. Menus stay hidden until you need them and then appear with a banner: World after mission 4, Cities with your first city, Military after **Basic Ballistics**, Diplomacy once you own two provinces or are at war, Naval after mission 9 or Maritime Trade. Settings can unlock everything.
- **World map (`M`):** 61 provinces across plains, forest, desert, mountains, volcanic land and arctic, shared with 5 computer-run nations. Captured provinces send their resource to your factory every 10 seconds, into an Import Depot if you build one.
- **Cities:** Mk. I to Mk. V, bought with money and materials. Each province has a few city slots.
- **Population:** automatic. Your HQ houses 4,000 citizens before any city exists, enough for one infantry unit at peacetime. Soldiers are exactly the people serving in your units and update the moment you recruit; everyone else works (taxes, plus up to +100% factory speed). Peacetime, Mobilized and War Economy set how much of the population can enlist and what it costs.
- **Military:** infantry, mechanized, artillery, armor, air wings and special forces. Click enemy or neutral land, or an army badge, on the map: a small popup offers Declare war, Attack with half or all of your force, Buy (neutral land) or Details, with no need to open the Nations tab. Attack any neighbouring province, or land from a port with transport ships. Forts and terrain strengthen defense.
- **Diplomacy:** relations from -100 to +100; alliances, peace, war, gifts, trade routes, requests for allied troops and shipping raids.
- **Town defenses:** walls (wooden palisade, stone, concrete), watchtowers (early warning), up to 3 bunkers, tank traps and minefields, built per province on the World map. They stack with forts and terrain, and computer nations fortify their own towns over time.
- **Strategic:** five rocket types and SAM sites with visible coverage circles and interception.
- **Research (`T`):** a branching tech tree per branch (Industry, Civilization, Military, Strategic, Naval). Lines show prerequisites (green done, amber ready, dashed locked); click a node for what it unlocks and costs, double-click or press Research to buy, drag to pan. Experimental technologies appear as unknown signals. Advanced research costs materials as well as money.
- **Ceasefire:** losing a province or having HQ raided pulls nearby enemies back and blocks attacks on your home province for 90 seconds. The President respawns with 10 seconds of protection, so HQ can't be spawn-camped.
- **Guide (`G`), Settings** (quality presets, FPS limit, particle and effect density, animation and map detail, performance mode) and a News feed.

It's single-player: every other nation is computer-controlled, and there is no online chat.

## Customization

- **Credits** carry over between saves. You earn 40 × stage number for each new stage, 15 per mission and 10 per milestone.
- **Character Editor:** head (hard hat, military cap, bare), body (factory uniform, executive suit, tactical armor), a clothes colour picker and your pet. New worlds use this look, and changes made in-game apply at once.
- **Customization Shop:**
  - machine paint jobs (Crimson Works, Arctic Steel, Military Olive, Synthwave)
  - UI themes (Dark Mode, Blueprint Blue)
  - pets that follow the President (dog, cat, parrot, helper drone)

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
