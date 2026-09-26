# Automation, logistics and military logic

All code lives in `index.html`. Search for the names below.

## Shared pieces

- **`findPath(x0, y0, target)`**: A* on the tile grid.
  - Moves in 8 directions without cutting corners.
  - Roads cost 30% less.
  - Belts, inserters and pipes are walkable; buildings, village houses and sea are not.
  - The goal set is every walkable tile touching the target's footprint.
  - Each search stops after 9,000 nodes.
- **`class Agent`**: the base for couriers, builders and engineers.
  - Holds position, a path and one carried stack.
  - `goTo(target)` finds a path and `follow()` walks it.
  - Walking speed drops in heavy pollution unless a stocked clinic is nearby (`sickness()`).
- **`syncAgents()`**: runs every second and keeps these crews:
  - 2 couriers per hub
  - 2 engineers per Maintenance Bay
  - 3 builders at HQ, plus 1 per hub up to 6
  - Agents whose home building is gone are removed, and anything they carried returns to your inventory.

## LogisticsHub

The hub's state lives on the entity, so it saves with the game:

| Field | Meaning |
| --- | --- |
| `inv` | Stored items (capacity 3,000) |
| `req[item]` | Target amount to keep here |
| `exp[item]` or `exp['*']` | Surplus that may leave |
| `inc`, `out` | Reservations for couriers in flight (not saved) |

- `deficit(B, k)` = `req − stored − incoming`
- `excess(A, k)` = `stored − req − outgoing`, but only if the item is exported
- `findTask(near)` pairs every hub or clinic with a deficit with the hub that has an excess, choosing the pair that is closest for the courier. It moves `min(deficit, excess, 40)` items.
- `reserve()` and `release()` stop two couriers from chasing the same surplus.

## Courier state machine

```
IDLE ──findTask──▶ TO_PICKUP ──arrive──▶ PICKUP (0.5 s)
  ▲                    │ path fails / hub gone               │ take stack
  │                    ▼                                     ▼
  └── RETURN ◀── DROPOFF (0.5 s) ◀──arrive── TO_DROPOFF ◀────┘
                    │ put stack in B (overflow goes to your inventory)
fail(): return the stack, release reservations, cool down 3 s, IDLE
```

## ConstructionManager and Builder state machine

- Placing a blueprint turns each ghost into a job in `sites`.
- `ConstructionManager.source(item)` looks for the building in this order:
  1. the nearest hub
  2. the nearest chest
  3. HQ (your inventory)
- `claim(builder)` gives the builder the nearest unclaimed job that has a source. Jobs with no source are marked `need` and drawn red.

```
IDLE ──claim──▶ TO_SOURCE ──arrive──▶ PICKUP (take 1 building)
                                          │
BUILD (1 s + 0.5 s × tiles) ◀──arrive── TO_SITE
   │ finish(): place the building, keep the recipe
   ▼
 IDLE
If the site is cancelled while carrying, the building returns to your inventory → RETURN
```

## AutoRepairManager and Engineer state machine

- `AutoRepairManager.scan()` runs every 2 seconds. It lists drills, furnaces and machines below 80% condition, worst first, and assigns each to the nearest idle engineer whose bay is within 40 tiles.
- `kit(bay)` looks for a repair kit in this order:
  1. the bay
  2. the nearest hub
  3. your inventory and chests
  4. otherwise, pay 1.5 × the normal repair cost in cash

```
IDLE ──assign──▶ TO_MACHINE ──arrive, take kit──▶ REPAIR (2 s) ──▶ condition 100% ──▶ RETURN ──▶ IDLE
```

## MilitaryUnit

- One unit sprite (infantry, or armor when you have mechanised or armored units) with a badge formatted by `fmtK` (1,500 → "1.5K").
- `MilitaryUnit.sync()` runs every half second and builds:
  - one unit per border camp: your soldiers × supply × ammunition
  - one unit per attack in progress, moving from its source to its target
  - one unit per rival nation, placed on its frontier
- `setTroops()` records gains and losses, and the badge flashes the change (−120 or +300).
- Sprites draw on the world map and at mid zoom. Close up, you see the individual soldiers instead.

## Military logistics

- Each camp holds up to 60 + troops ÷ 15 ammunition.
  - It uses troops ÷ 100 per minute, and six times that while fighting.
- Every 20 seconds (`ammoSecond`), a camp below 80% gets a truck:
  - loaded from the nearest hub holding ammunition, or else from your inventory and chests
  - the ammunition arrives when the truck reaches the camp
- `ammoMult()` averages 1 per supplied camp and 0.35 per dry camp. It multiplies `armyStr()`.
  - While it is below 0.7, your soldiers fire three times slower and do half damage.
