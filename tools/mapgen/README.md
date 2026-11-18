# Map generator

Python 3 scripts that generate and validate the climbing map (`assets/maps/map.tmx`).
They are a design aid only: the game loads the `.tmx`, not these scripts.

- `mapgen.py`: geometry helpers (irregular rock shapes, rock-to-rock distance),
  the wide and deep floor, a rock-free spawn, the `PlayArea` side limits (10 m past
  the outermost rocks) and the TMX writer. Map size and floor live here.
- `mapgen3.py`: grows rocks from any existing rock in any upward or sideways
  direction (forks, crossings and dead ends instead of fixed lanes), then checks
  that nothing overlaps, that unrelated rocks keep 1.2 m apart (the body passes
  with its arms out) and that the summit is reachable within the physical limits
  of each section.

## Design values (meters)

| Section | Height | Rock radius | Gap to the next hold |
| --- | --- | --- | --- |
| Easy | 0-12 | 1.35-1.70 | 1.00-1.12 |
| Medium | 12-27 | 0.95-1.30 | 1.10-1.28 |
| Hard | 27-42 | 0.65-0.95 | 1.25-1.40, plus 1.75-2.05 jumps from ledges |

Limits come from the server physics: arm reach about 0.70 m, rope 0.76 m, so a
hanging character reaches at most about 1.45 m to the next hold.

## Usage

```
cd tools/mapgen
python mapgen3.py 1                          # search seeds 1..60, print the best one
python mapgen3.py 1 ../../assets/maps/map.tmx  # also write it
```

The committed map is seed 57 with two manual edits made afterwards: rock `R1` was
removed and `PlayerSpawn` moved to x = 691 px, to the right of `R0`. Regenerating
overwrites those edits.
