package io.github.lucasstml.towerrogue.run;

import io.github.lucasstml.towerrogue.content.NodeType;
import io.github.lucasstml.towerrogue.core.Rng;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Slay-the-Spire-style map generation.
 *
 * <ol>
 *   <li>Trace several random walks from the bottom floor to the top; each step moves to the
 *       column below-left, below or below-right. A step that would cross an existing edge
 *       picks another direction, so the drawn map never has crossing lines.</li>
 *   <li>The union of visited cells becomes the node graph; every top node links to the boss.</li>
 *   <li>Node types are assigned floor by floor with fixed anchors (first floor = combat,
 *       middle = treasure, last = campfire) and weighted random types elsewhere, re-rolling
 *       when a rule is broken (no elite before floor 2, no two shops/elites/campfires in a row).</li>
 * </ol>
 */
public final class RunMapGenerator {
    private static final int WIDTH = 5;
    private static final int PATHS = 6;

    private RunMapGenerator() {
    }

    public static RunMap generate(Rng rng, int floors) {
        int[][] ids = new int[floors][WIDTH];
        for (int[] row : ids) java.util.Arrays.fill(row, -1);
        List<RunMap.Node> nodes = new ArrayList<>();
        Set<Long> edges = new HashSet<>();

        int firstStart = rng.nextInt(WIDTH);
        for (int p = 0; p < PATHS; p++) {
            int col = p == 0 ? firstStart : rng.nextInt(WIDTH);
            if (p == 1) {
                while (col == firstStart) col = rng.nextInt(WIDTH);
            }
            node(nodes, ids, 0, col, rng);
            for (int f = 0; f < floors - 1; f++) {
                List<Integer> options = new ArrayList<>();
                for (int d = -1; d <= 1; d++) {
                    int c = col + d;
                    if (c >= 0 && c < WIDTH) options.add(c);
                }
                rng.shuffle(options);
                int chosen = options.get(0);
                for (int c : options) {
                    // Crossing check: going (f,col)->(f+1,c) crosses an existing (f,c)->(f+1,col).
                    if (c == col || !edges.contains(edgeKey(f, c, col))) {
                        chosen = c;
                        break;
                    }
                }
                RunMap.Node from = node(nodes, ids, f, col, rng);
                RunMap.Node to = node(nodes, ids, f + 1, chosen, rng);
                if (!from.next.contains(to.id)) from.next.add(to.id);
                edges.add(edgeKey(f, col, chosen));
                col = chosen;
            }
        }
        RunMap.Node boss = new RunMap.Node(nodes.size(), floors, WIDTH / 2, 0, 0);
        boss.type = NodeType.BOSS;
        nodes.add(boss);
        for (RunMap.Node n : nodes) {
            if (n.floor == floors - 1) n.next.add(boss.id);
            n.next.sort(Integer::compare);
        }
        RunMap map = new RunMap(nodes, floors, WIDTH, boss.id);
        assignTypes(map, rng);
        return map;
    }

    private static RunMap.Node node(List<RunMap.Node> nodes, int[][] ids, int floor, int col, Rng rng) {
        if (ids[floor][col] >= 0) return nodes.get(ids[floor][col]);
        RunMap.Node n = new RunMap.Node(nodes.size(), floor, col, rng.range(-0.22, 0.22), rng.range(-0.18, 0.18));
        nodes.add(n);
        ids[floor][col] = n.id;
        return n;
    }

    private static long edgeKey(int floor, int fromCol, int toCol) {
        return ((long) floor << 16) | ((long) fromCol << 8) | toCol;
    }

    private static void assignTypes(RunMap map, Rng rng) {
        int floors = map.floors();
        int treasureFloor = floors / 2;
        List<RunMap.Node> ordered = new ArrayList<>(map.nodes());
        ordered.sort((a, b) -> a.floor != b.floor ? Integer.compare(a.floor, b.floor) : Integer.compare(a.col, b.col));
        for (RunMap.Node n : ordered) {
            if (n.type == NodeType.BOSS) continue;
            if (n.floor == 0) n.type = NodeType.COMBAT;
            else if (n.floor == floors - 1) n.type = NodeType.REST;
            else if (n.floor == treasureFloor) n.type = NodeType.TREASURE;
            else n.type = rollType(map, n, rng, floors);
        }
        ensure(map, rng, NodeType.SHOP, floors);
        ensure(map, rng, NodeType.ELITE, floors);
    }

    private static NodeType rollType(RunMap map, RunMap.Node n, Rng rng, int floors) {
        List<RunMap.Node> parents = map.parentsOf(n);
        for (int attempt = 0; attempt < 12; attempt++) {
            double roll = rng.nextDouble() * 100;
            NodeType t;
            if (roll < 45) t = NodeType.COMBAT;
            else if (roll < 67) t = NodeType.EVENT;
            else if (roll < 81) t = NodeType.ELITE;
            else if (roll < 90) t = NodeType.SHOP;
            else t = NodeType.REST;
            if (t == NodeType.ELITE && n.floor < 2) continue;
            if (t == NodeType.REST && (n.floor < 2 || n.floor >= floors - 2)) continue;
            boolean repeats = false;
            if (t == NodeType.ELITE || t == NodeType.SHOP || t == NodeType.REST) {
                for (RunMap.Node p : parents) if (p.type == t) repeats = true;
            }
            if (!repeats) return t;
        }
        return NodeType.COMBAT;
    }

    /** Guarantees at least one node of the type exists, converting a plain node if needed. */
    private static void ensure(RunMap map, Rng rng, NodeType type, int floors) {
        List<RunMap.Node> candidates = new ArrayList<>();
        for (RunMap.Node n : map.nodes()) {
            if (n.type == type) return;
            if ((n.type == NodeType.COMBAT || n.type == NodeType.EVENT) && n.floor >= 2 && n.floor <= floors - 2) {
                candidates.add(n);
            }
        }
        if (!candidates.isEmpty()) rng.pick(candidates).type = type;
    }
}
