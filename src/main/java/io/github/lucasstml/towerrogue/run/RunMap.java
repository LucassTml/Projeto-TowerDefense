package io.github.lucasstml.towerrogue.run;

import io.github.lucasstml.towerrogue.content.NodeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The branching node graph of one act. Floors go bottom (0) to top; the boss sits above the last floor. */
public final class RunMap {

    public static final class Node {
        public final int id;
        public final int floor;
        public final int col;
        public NodeType type;
        public final List<Integer> next = new ArrayList<>();
        /** Small cosmetic offsets so the map does not look like a spreadsheet. */
        public final double jitterX, jitterY;

        Node(int id, int floor, int col, double jitterX, double jitterY) {
            this.id = id;
            this.floor = floor;
            this.col = col;
            this.jitterX = jitterX;
            this.jitterY = jitterY;
        }
    }

    private final List<Node> nodes;
    private final int floors;
    private final int width;
    private final int bossId;

    RunMap(List<Node> nodes, int floors, int width, int bossId) {
        this.nodes = nodes;
        this.floors = floors;
        this.width = width;
        this.bossId = bossId;
    }

    public Node node(int id) {
        return nodes.get(id);
    }

    public List<Node> nodes() {
        return Collections.unmodifiableList(nodes);
    }

    public int floors() {
        return floors;
    }

    public int width() {
        return width;
    }

    public int bossId() {
        return bossId;
    }

    public List<Node> parentsOf(Node n) {
        List<Node> out = new ArrayList<>();
        for (Node p : nodes) if (p.next.contains(n.id)) out.add(p);
        return out;
    }
}
