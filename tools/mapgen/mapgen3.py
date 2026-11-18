"""Branching (scattered) climbing map generator with validation.

Rocks grow from ANY existing rock (or the floor) in any upward/sideways
direction, so the result is a field of holds with forks, shortcuts, crossings
and dead ends instead of fixed lanes. Meters, y up from the floor top.
"""
import heapq
import math
import random
import sys

import mapgen as base

X_MIN, X_MAX = 3.5, 30.5          # rock centres stay in this band
SUMMIT_X = 17.0
TOP_HEIGHT = 42.0                 # highest regular rock centre
MIN_CLEARANCE = 1.2               # unrelated rocks: the body passes with arms out

def section(h):
    if h < 12:
        return "easy"
    if h < 27:
        return "medium"
    return "hard"

SPEC = {
    "easy":   dict(r=(1.35, 1.70), gap=(1.00, 1.12), limit=1.20, count=16),
    "medium": dict(r=(0.95, 1.30), gap=(1.10, 1.28), limit=1.35, count=21),
    "hard":   dict(r=(0.65, 0.95), gap=(1.25, 1.40), limit=1.42, count=21),
}
DYNAMIC_GAP = (1.75, 2.05)        # hard, from a ledge: stand + jump, or swing
DYNAMIC_LIMIT = 2.10
SHAPES = {"boulder": (1.0, 1.0), "wide": (1.3, 0.8), "tall": (0.8, 1.25), "ledge": (1.55, 0.55)}


def height(poly):
    return sum(p[1] for p in poly) / len(poly)


def centre_x(poly):
    return sum(p[0] for p in poly) / len(poly)


def grow(rng):
    rocks, ledges = {}, set()
    counts = {k: 0 for k in SPEC}
    attempts = 0
    while attempts < 40000 and any(counts[k] < SPEC[k]["count"] for k in SPEC):
        attempts += 1
        # parent: the floor, or a rock weighted toward the current top so the field climbs
        names = list(rocks)
        use_floor = not names or rng.random() < max(0.0, 0.35 - 0.03 * len(names))
        if use_floor:
            parent = None
            h0 = 0.0
        else:
            weights = [math.exp(height(rocks[n]) / 7.0) for n in names]
            pname = rng.choices(names, weights)[0]
            parent = rocks[pname]
            h0 = height(parent)
        sec = section(h0 + 1.5)
        spec = SPEC[sec]
        if counts[sec] >= spec["count"]:
            continue
        dynamic = (not use_floor and sec == "hard" and pname in ledges and rng.random() < 0.5)
        r = rng.uniform(*spec["r"])
        shape = rng.choices(list(SHAPES), [3, 2, 2, 2 if sec != "easy" else 1])[0]
        sx, sy = SHAPES[shape]
        n = rng.randint(8, 11)
        rot = rng.uniform(-0.3, 0.3)
        pseed = rng.random()

        if use_floor:
            cx = rng.uniform(X_MIN + 1, X_MAX - 1)
            lift = rng.uniform(0.0, 1.2)        # lowest point 0..1.2 m above the floor
            poly = base.irregular(cx, 0, r, sx, sy, n, random.Random(pseed), rot)
            low = min(p[1] for p in poly)
            poly = [(x, y - low + lift) for x, y in poly]
        else:
            gap = rng.uniform(*(DYNAMIC_GAP if dynamic else spec["gap"]))
            max_angle = 35 if dynamic else 88
            ang = math.radians(rng.uniform(-max_angle, max_angle))
            px, py = centre_x(parent), h0
            poly, dist = None, 0.4
            while dist < 9:
                cand = base.irregular(px + math.sin(ang) * dist, py + math.cos(ang) * dist,
                                      r, sx, sy, n, random.Random(pseed), rot)
                if base.poly_dist(parent, cand) >= gap:
                    poly = cand
                    break
                dist += 0.05
            if poly is None:
                continue

        cx, h = centre_x(poly), height(poly)
        if not (X_MIN <= cx <= X_MAX) or h > TOP_HEIGHT or section(h) != sec and not use_floor and section(h) != section(h0):
            # the new rock must land in a section that still has room
            if counts.get(section(h), 99) >= SPEC.get(section(h), {"count": 0})["count"] or not (X_MIN <= cx <= X_MAX) or h > TOP_HEIGHT:
                continue
        if any(base.poly_dist(poly, o) < (0.0 if o is parent else MIN_CLEARANCE) for o in rocks.values()):
            continue
        if parent is not None and any(base.poly_dist(poly, o) < MIN_CLEARANCE for o in rocks.values() if o is not parent):
            continue
        name = f"R{len(rocks)}"
        rocks[name] = poly
        counts[section(h)] = counts.get(section(h), 0) + 1
        if shape == "ledge":
            ledges.add(name)

    # summit pillar, centred above the field
    top = max(max(p[1] for p in poly) for poly in rocks.values())
    for _ in range(300):
        summit = base.irregular(SUMMIT_X + rng.uniform(-1.5, 1.5), top + rng.uniform(1.5, 2.3),
                                1.1, 0.8, 1.3, 10, random.Random(rng.random()), 0.0)
        if all(base.poly_dist(summit, o) >= MIN_CLEARANCE for o in rocks.values()):
            rocks["S"] = summit
            break
    return rocks, ledges


def graph(rocks, ledges):
    names = list(rocks)
    gaps = {}
    for i, a in enumerate(names):
        for b in names[i + 1:]:
            gaps[(a, b)] = gaps[(b, a)] = base.poly_dist(rocks[a], rocks[b])

    def limit(a, b):
        h = max(height(rocks[a]), height(rocks[b]))
        sec = section(h)
        if sec == "hard" and (a in ledges or b in ledges):
            return DYNAMIC_LIMIT
        return SPEC[sec]["limit"]

    edges = {n: [] for n in names}
    for a in names:
        for b in names:
            if a != b and 0 <= gaps[(a, b)] <= limit(a, b):
                edges[a].append((b, gaps[(a, b)]))
    starts = [n for n in names if min(p[1] for p in rocks[n]) <= 2.2]
    return gaps, edges, starts


def analyse(rocks, ledges, verbose=True):
    gaps, edges, starts = graph(rocks, ledges)
    names = list(rocks)
    if any(g < 0 for g in gaps.values()):
        return None
    # reachability
    seen, stack = set(starts), list(starts)
    while stack:
        a = stack.pop()
        for b, _ in edges[a]:
            if b not in seen:
                seen.add(b); stack.append(b)
    if "S" not in seen:
        return None
    # dead ends: reachable rocks with no neighbour noticeably higher
    dead = [a for a in seen if a != "S" and not any(height(rocks[b]) > height(rocks[a]) + 0.4 for b, _ in edges[a])]
    # route diversity: rocks that lie on SOME floor->summit path that only climbs or traverses
    up_edges = {a: [b for b, _ in edges[a] if height(rocks[b]) > height(rocks[a]) - 0.8] for a in names}
    can_reach_top = {"S"}
    changed = True
    while changed:
        changed = False
        for a in names:
            if a not in can_reach_top and any(b in can_reach_top for b in up_edges[a]):
                can_reach_top.add(a); changed = True
    useful = [a for a in seen if a in can_reach_top]
    # how many distinct entries into the hard section lead to the top
    hard_entries = [a for a in useful if section(height(rocks[a])) == "hard"
                    and any(section(height(rocks[b])) == "medium" for b, _ in edges[a])]
    dyn = [(a, b) for a in names for b, g in edges[a] if g > SPEC["hard"]["limit"] and a < b]
    # shortest route in moves and the "easiest" route (smallest worst gap)
    dist = {s: 0 for s in starts}
    q = [(0, s) for s in starts]
    while q:
        d, a = heapq.heappop(q)
        if d > dist.get(a, 1e9):
            continue
        for b, _ in edges[a]:
            if d + 1 < dist.get(b, 1e9):
                dist[b] = d + 1; heapq.heappush(q, (d + 1, b))
    info = dict(rocks=len(rocks), reachable=len(seen), useful=len(useful), dead=len(dead),
                hard_entries=len(hard_entries), dynamic=len(dyn), min_moves=dist.get("S"),
                peak=max(p[1] for p in rocks["S"]))
    if verbose:
        xs = [p[0] for poly in rocks.values() for p in poly]
        print(info, f"x {min(xs):.1f}..{max(xs):.1f}")
        for sec in ("easy", "medium", "hard"):
            ns = [a for a in names if section(height(rocks[a])) == sec]
            steps = []
            for a in ns:
                up = [g for b, g in edges[a] if height(rocks[b]) > height(rocks[a]) + 0.4]
                if up:
                    steps.append(min(up))
            if steps:
                print(f"  {sec:6} rocks {len(ns):2}  next-hold gap avg {sum(steps)/len(steps):.2f} max {max(steps):.2f}")
    return info


def score(info):
    # prefer: several hard entries, some dead ends, a few dynamic moves, moderate path length
    if info is None or info["min_moves"] is None:
        return -1
    return (min(info["hard_entries"], 5) * 3 + min(info["dead"], 8) + min(info["dynamic"], 6) * 2
            + min(info["useful"], 45) * 0.3 - abs(info["peak"] - 46) * 2)


if __name__ == "__main__":
    first = int(sys.argv[1])
    best = None
    for seed in range(first, first + 60):
        rocks, ledges = grow(random.Random(seed))
        if "S" not in rocks:
            continue
        info = analyse(rocks, ledges, verbose=False)
        sc = score(info)
        if sc > (best[0] if best else -1):
            best = (sc, seed, rocks, ledges)
    if not best:
        print("no valid map"); sys.exit(1)
    sc, seed, rocks, ledges = best
    print("best seed", seed, "score", round(sc, 1))
    analyse(rocks, ledges)
    if len(sys.argv) > 2:
        base.write_tmx(rocks, sys.argv[2], 300)
        print("written", sys.argv[2])
