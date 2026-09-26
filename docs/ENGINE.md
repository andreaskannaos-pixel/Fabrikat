# Engine, tech tree and balance systems

All code is in `index.html`, in the block headed `V16`. Search for the names below.

## Frame loop and render interpolation

```
requestAnimationFrame(loop)
  acc += dt × gameSpeed
  while acc ≥ 1/60 (at most 6 × speed steps): tick()
        tick(): snapPrev() stores px/py for the President, agents, troops, trucks and villagers,
                then runs the fixed 60 Hz simulation
  ALPHA = acc × 60                      fraction of the next step already elapsed
  render(): moving things draw at IX(o) = px + (x − px) × ALPHA (and IY for y)
```

- The simulation is deterministic at 60 Hz whatever the display rate, and 120 Hz or 144 Hz monitors get smooth motion.
- An FPS cap (30 or 60) just runs two steps per frame or one.
- A jump of more than 2 tiles in one step (a teleport or respawn) is drawn directly, not interpolated.
- Camera follow and the pet use frame-rate-independent smoothing: `k = 1 − 0.88^(dt × 60)`.

## Object pools

`Pool` keeps free objects. `sweep(arr, alive, pool)` compacts an array in place and returns dead objects to their pool. The loop never reassigns these arrays, so there is no per-frame garbage.

| Pool | Created by | Freed when |
| --- | --- | --- |
| `FX.pf` floating text | `FX.float(x, y, text, colour)` | 70 frames old |
| `FX.ps` tracers | `FX.shot(...)` (from `fireAt`) | range spent or hit |
| `FX.pb` explosions | `FX.boom(x, y)` | 30 ticks old |
| `FX.pc` build flashes | `FX.built(x, y, w, h)` | 24 frames old |

Troops, trucks and world effects are also swept in place. They are not pooled, because other objects hold references to them.

## Spatial grid and culling

- `TG` is a uniform grid of 4-tile cells over the map. It is stored in typed arrays as a linked list per cell and rebuilt by `troopTick` and `shotsTick` without allocating.
  - `findFoe` looks up to 14 tiles away.
  - Tracer collision checks a 1.5-tile area around each tracer.
  - Both only visit nearby cells, so combat costs O(n) instead of O(n²).
- Simulation that affects the outcome keeps running off-screen: belts, machines, agents, troops.
- Purely cosmetic updates stay on-screen only: villagers' walking, smoke, lights, labels and every draw loop.

## Hit detection

A tracer moves up to half a tile per tick, so hits use swept tests (`segBox`, `segCircle`) along the whole segment it covered, never a single point.

- The President's hitbox matches the sprite: a torso-and-legs box (±0.17 × 0.56 tiles) plus a head circle (r 0.15).
- Troops keep their original 0.8 × 1 tile box, so combat balance is unchanged.

## Pathfinding

- `findPath` is A* on the tile grid:
  - The open list is a binary heap in `Float64Array`/`Int32Array` that grows as needed, with no per-node arrays.
  - Goal tiles are marked in a stamped `Uint32Array`.
- `smoothPath` pulls the path tight: from each waypoint it jumps to the furthest of the next 24 waypoints it can see.
  - `losTiles` samples every 0.25 tile, with 0.3-tile body clearance so villagers never clip corners.
  - It never takes a shortcut across a change between road and off-road, so couriers stay on roads.

## Smart drag-to-build

- Dragging places along the path the pointer takes. Belts turn to follow the drag, and pipes link to neighbours and machine ports as before.
- Pressing on the end of an existing belt of the same tier picks it up and continues the line (`dragStart`).
- On release, `beltAutoConnect` checks the last belt. If nothing is in front of it and a machine, chest, hub, generator or splitter sits beside it, the belt turns into that building and "connected" floats up.

## Lights

`drawNight` cuts lights out of the darkness layer and adds a glow:

- `dynLights` adds muzzle flashes (for the first 3 ticks of a tracer) and explosions, from dusk onward.
- Furnace and generator lights flicker.
- `drawMuzzle` draws the flash sprite in daylight too.
- The President's respawn shield is a pulsing ring.

## Tech tree

- `techLayout(tab)` builds a layered layout of the tree for one tab, cached by tab and research count:
  - Column = longest chain of prerequisites inside the tab.
  - Row order comes from 4 sweeps (forward and backward) that move each node towards the average height of its links, with a minimum gap between nodes.
- Nodes are absolutely positioned buttons. Edges are SVG cubic curves: green when researched, amber when the next one is ready, dashed grey when locked.
- A prerequisite in another tab shows as a "needs X" line on the node, and as a jump button in the side panel.
- Experimental technologies are dashed nodes. They stay "Unknown signal" until their trigger research is done.
- The side panel shows state, description, unlocked items, prerequisites, cost with shortfalls, and the Research or Contribute button.
- Drag the tree to pan; the scroll position survives re-renders. Double-click a ready node to research it.

## Balance fixes

- **Emergency Requisitions** (Industry, tier 1, $1,500 + 50 iron plates, needs Automation) is required before engineers may pay cash for repairs. See `AUTOMATION.md`.
- **HQ population:** `HQ_POP = 4,000` citizens count towards population and manpower.
  - They don't count towards crew or the industry bonus, and they pay no tax, so the early economy is unchanged.
  - It is 4,000 rather than a token 20 because an infantry unit needs 1,000 manpower. At the peacetime limit (30%) it allows exactly one unit before your first city.
  - `nat.soldiers` equals the manpower of your units and updates the moment you recruit.
  - The mission "Reach 10,000 people in your cities" leaves the HQ citizens out.

## Ceasefire (anti-spawn-kill)

`Sanctuary.trigger(reason)` runs when you lose a province or HQ is raided:

- Hostile troops within 40 tiles of HQ, bandits, and troops or attacks aimed at your home province are removed.
- `stats.sanct = 90` seconds, counted down every second and saved with the game. While it runs:
  - `aiAttack` refuses to target the home province. The scripted survival attack is postponed instead.
  - Bandit raids don't spawn.
- The President gets 12 s of invulnerability (`pl.invT`), and enemies stop targeting them.

When the President respawns, `Sanctuary.respawn()` gives 10 s of invulnerability, clears hostiles within 18 tiles of HQ, and sets at least 30 s of ceasefire.

The President panel shows "Ceasefire at HQ · N s" while it lasts.
