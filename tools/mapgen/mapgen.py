"""Generates and validates the climbing map (Tiled TMX).

All design values are in meters, y up from the top of the floor. The TMX uses
pixels (32 px = 1 m), y down from the top of the map.
"""
import math
import random
import sys

PPM = 32
MAP_W_TILES = 34
MAP_H_TILES = 54
MAP_W = MAP_W_TILES * PPM
MAP_H = MAP_H_TILES * PPM
FLOOR_TOP_FROM_BOTTOM_M = 2.0          # floor surface 2 m above the map bottom
FLOOR_MARGIN_M = 20.0                  # floor extends 20 m past each side

# Max boundary-to-boundary gap allowed between consecutive holds, per height.
HANG_LIMIT = 1.45                      # rope 0.76 + reach 0.70 (absolute physical max)
def gap_limit(height):
    if height < 12:
        return 0.8                     # easy: comfortable reach
    if height < 26:
        return 1.1                     # medium
    return 1.35                        # hard: near full pull-up


def to_px(x_m, y_m):
    """Design meters -> TMX pixels."""
    floor_top_px = MAP_H - FLOOR_TOP_FROM_BOTTOM_M * PPM
    return x_m * PPM, floor_top_px - y_m * PPM


def irregular(cx, cy, r, sx, sy, n, rng, rot):
    """Star-shaped (hence simple) irregular polygon around (cx, cy), meters."""
    step = 2 * math.pi / n
    pts = []
    for i in range(n):
        a = i * step + rng.uniform(-0.28, 0.28) * step
        rr = r * rng.uniform(0.80, 1.18)
        x = math.cos(a) * rr * sx
        y = math.sin(a) * rr * sy
        xr = x * math.cos(rot) - y * math.sin(rot)
        yr = x * math.sin(rot) + y * math.cos(rot)
        pts.append((cx + xr, cy + yr))
    return pts


# name, x, y, r, sx, sy  (meters). Kinds: boulder ~1/1, ledge wide/flat, pillar tall.
LAYOUT = [
    # --- easy: big boulders, two routes from the spawn, short steps -------------
    ("E1", 13.2, 1.1, 1.6, 1.5, 0.8),
    ("E2", 21.0, 1.2, 1.6, 1.4, 0.85),
    ("E3", 10.6, 3.9, 1.6, 1.25, 0.9),
    ("E4", 23.4, 4.0, 1.6, 1.25, 0.9),
    ("E5", 13.4, 6.6, 1.6, 1.3, 0.85),
    ("E6", 20.6, 6.8, 1.6, 1.3, 0.85),
    ("E7", 10.4, 9.4, 1.5, 1.2, 0.9),
    ("E8", 23.6, 9.5, 1.5, 1.2, 0.9),
    ("E9", 17.0, 10.4, 1.4, 1.1, 0.8),
    # --- medium: smaller rocks, longer steps, routes cross the middle -----------
    ("M1", 12.6, 12.9, 1.35, 1.1, 0.9),
    ("M2", 21.6, 13.0, 1.35, 1.1, 0.9),
    ("M3", 9.2, 15.6, 1.3, 1.0, 1.0),
    ("M4", 17.0, 15.6, 1.3, 1.35, 0.65),
    ("M5", 24.8, 15.8, 1.3, 1.0, 1.0),
    ("M6", 12.6, 18.4, 1.25, 0.95, 1.05),
    ("M7", 21.4, 18.5, 1.25, 0.95, 1.05),
    ("M8", 8.8, 21.2, 1.2, 1.0, 1.0),
    ("M9", 17.0, 21.2, 1.25, 1.4, 0.6),
    ("M10", 25.2, 21.4, 1.2, 1.0, 1.0),
    ("M11", 12.8, 24.0, 1.2, 1.0, 0.95),
    ("M12", 21.2, 24.1, 1.2, 1.0, 0.95),
    # --- hard: small holds, near-max pull-ups, ledges to rest and jump -----------
    ("H1", 9.4, 27.0, 1.05, 1.0, 1.0),
    ("H2", 17.0, 27.0, 1.0, 1.5, 0.55),
    ("H3", 24.6, 27.2, 1.05, 1.0, 1.0),
    ("H4", 12.8, 30.0, 0.95, 1.0, 1.0),
    ("H5", 21.2, 30.1, 0.95, 1.0, 1.0),
    ("H6", 9.0, 33.0, 0.9, 1.0, 1.0),
    ("H7", 17.0, 33.0, 0.95, 1.45, 0.55),
    ("H8", 25.0, 33.1, 0.9, 1.0, 1.0),
    ("H9", 13.2, 36.0, 0.9, 1.0, 1.0),
    ("H10", 20.8, 36.1, 0.9, 1.0, 1.0),
    ("H11", 17.0, 38.9, 1.0, 1.3, 0.65),
    ("S", 17.0, 41.6, 1.25, 0.85, 1.3),       # summit pillar
]


def seg_point_dist(p, a, b):
    ax, ay = a; bx, by = b; px, py = p
    dx, dy = bx - ax, by - ay
    t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy or 1)))
    cx, cy = ax + t * dx, ay + t * dy
    return math.hypot(px - cx, py - cy)


def point_in_poly(p, poly):
    x, y = p; inside = False
    for i in range(len(poly)):
        (x1, y1), (x2, y2) = poly[i], poly[(i + 1) % len(poly)]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            inside = not inside
    return inside


def segs_intersect(a, b, c, d):
    def orient(p, q, r):
        return (q[0] - p[0]) * (r[1] - p[1]) - (q[1] - p[1]) * (r[0] - p[0])
    return (orient(a, b, c) * orient(a, b, d) < 0) and (orient(c, d, a) * orient(c, d, b) < 0)


def poly_dist(p, q):
    """Boundary distance, or -1 when the polygons overlap."""
    for v in p:
        if point_in_poly(v, q):
            return -1
    for v in q:
        if point_in_poly(v, p):
            return -1
    for i in range(len(p)):
        for j in range(len(q)):
            if segs_intersect(p[i], p[(i + 1) % len(p)], q[j], q[(j + 1) % len(q)]):
                return -1
    best = float('inf')
    for v in p:
        for j in range(len(q)):
            best = min(best, seg_point_dist(v, q[j], q[(j + 1) % len(q)]))
    for v in q:
        for i in range(len(p)):
            best = min(best, seg_point_dist(v, p[i], p[(i + 1) % len(p)]))
    return best


def build(seed):
    rng = random.Random(seed)
    rocks = {}
    for name, x, y, r, sx, sy in LAYOUT:
        n = rng.randint(8, 11)
        rot = rng.uniform(-0.25, 0.25)
        rocks[name] = irregular(x, y, r, sx, sy, n, rng, rot)
    return rocks


def floor_poly():
    """Wide floor: slightly uneven top surface at y = 0, 3 m thick, past both sides."""
    rng = random.Random(7)
    left, right = -FLOOR_MARGIN_M, MAP_W_TILES + FLOOR_MARGIN_M
    top = []
    x = left
    while x < right:
        top.append((x, rng.uniform(-0.12, 0.12)))
        x += 2.5
    top.append((right, 0.0))
    return top + [(right, -3.0), (left, -3.0)]


def validate(rocks):
    names = list(rocks)
    ok = True
    gaps = {}
    for i, a in enumerate(names):
        for b in names[i + 1:]:
            d = poly_dist(rocks[a], rocks[b])
            gaps[(a, b)] = gaps[(b, a)] = d
            if d < 0:
                print("OVERLAP", a, b); ok = False
            elif d < 0.55:
                print("TOO TIGHT (body could wedge)", a, b, round(d, 2)); ok = False

    def lowest(name):
        return min(p[1] for p in rocks[name])

    def height(name):
        return sum(p[1] for p in rocks[name]) / len(rocks[name])

    # Reachability: floor -> rocks whose lowest point is within standing reach (2.2 m),
    # then rock -> rock when the gap fits the limit of the higher rock's section.
    reach = {n for n in names if lowest(n) <= 2.2}
    frontier = list(reach)
    while frontier:
        a = frontier.pop()
        for b in names:
            if b in reach:
                continue
            limit = gap_limit(max(height(a), height(b)))
            if 0 <= gaps[(a, b)] <= limit:
                reach.add(b); frontier.append(b)
    if "S" not in reach:
        print("SUMMIT NOT REACHABLE"); ok = False
    unreachable = [n for n in names if n not in reach]
    if unreachable:
        print("unreachable rocks:", unreachable)

    # Per rock: the nearest rock above it (the next move) and its gap.
    print("rock  height  next-above gap(limit)")
    for a in names:
        above = [(gaps[(a, b)], b) for b in names if b != a and height(b) > height(a) + 0.5 and gaps[(a, b)] >= 0]
        if above:
            g, b = min(above)
            flag = "" if g <= gap_limit(height(b)) else "  <-- exceeds"
            print(f"{a:4} {height(a):6.1f}  {b:4} {g:4.2f} ({gap_limit(height(b))}){flag}")
    return ok


def poly_px(points):
    px = [to_px(x, y) for x, y in points]
    ox, oy = round(px[0][0]), round(px[0][1])
    rel = " ".join(f"{round(x) - ox},{round(y) - oy}" for x, y in px)
    return ox, oy, rel


SPAWN_HALF_WIDTH = 2.7     # 4 players 1.6 m apart (+-2.4 m) plus half a body (0.3 m)
SPAWN_MARGIN = 0.9


def free_spawn_x(rocks):
    """Centre of the widest floor stretch with no rock lower than 2.8 m, so no player spawns in a rock."""
    low = sorted((min(p[0] for p in pts), max(p[0] for p in pts))
                 for pts in rocks.values() if min(p[1] for p in pts) < 2.8)
    free, cur = [], 0.0
    for a, b in low:
        if a > cur:
            free.append((cur, a))
        cur = max(cur, b)
    if cur < MAP_W_TILES:
        free.append((cur, float(MAP_W_TILES)))
    a, b = max(free, key=lambda f: f[1] - f[0])
    need = SPAWN_HALF_WIDTH + SPAWN_MARGIN
    if b - a < 2 * need:
        raise ValueError("no floor stretch wide enough for the spawn")
    # as close as possible to the nearest rock on the climbing side, still clear
    return b - need if b < MAP_W_TILES else a + need


def write_tmx(rocks, path, time_limit):
    spawn_x, spawn_y = to_px(free_spawn_x(rocks), 1.4)
    summit = rocks["S"]
    peak_y = max(p[1] for p in summit)
    sx0, sy0 = to_px(17.0 - 2.0, peak_y + 4.0)    # rectangle top-left (TMX y down)
    s_w, s_h = 4.0 * PPM, 3.6 * PPM                # from 0.4 m above the peak to 4 m above
    lines = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        f'<map version="1.10" tiledversion="1.12.2" orientation="orthogonal" renderorder="right-down" '
        f'width="{MAP_W_TILES}" height="{MAP_H_TILES}" tilewidth="32" tileheight="32" infinite="0" '
        f'nextlayerid="8" nextobjectid="{len(rocks) + 10}">',
        ' <properties>',
        f'  <property name="timeLimit" type="float" value="{time_limit}"/>',
        ' </properties>',
        ' <group id="3" name="Gameplay">',
        '  <objectgroup id="7" name="Player">',
        f'   <object id="1" name="PlayerSpawn" type="SpawnPoints" x="{round(spawn_x)}" y="{round(spawn_y)}">',
        '    <point/>',
        '   </object>',
        f'   <object id="2" name="Summit" type="Goal" x="{round(sx0)}" y="{round(sy0)}" '
        f'width="{round(s_w)}" height="{round(s_h)}"/>',
        '  </objectgroup>',
        '  <objectgroup id="4" name="Surface">',
    ]
    ox, oy, rel = poly_px(floor_poly())
    lines += [f'   <object id="3" name="Floor" type="Rock" x="{ox}" y="{oy}">',
              f'    <polygon points="{rel}"/>', '   </object>']
    for i, (name, pts) in enumerate(rocks.items()):
        ox, oy, rel = poly_px(pts)
        lines += [f'   <object id="{i + 4}" name="{name}" type="Rock" x="{ox}" y="{oy}">',
                  f'    <polygon points="{rel}"/>', '   </object>']
    lines += ['  </objectgroup>', ' </group>', '</map>', '']
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write("\n".join(lines))


if __name__ == "__main__":
    seed = int(sys.argv[1]) if len(sys.argv) > 1 else 42
    rocks = build(seed)
    ok = validate(rocks)
    print("VALID" if ok else "INVALID")
    if ok and len(sys.argv) > 2:
        write_tmx(rocks, sys.argv[2], 300)
        print("written", sys.argv[2])
