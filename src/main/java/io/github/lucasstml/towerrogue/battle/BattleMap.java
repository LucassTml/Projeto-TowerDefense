package io.github.lucasstml.towerrogue.battle;

import io.github.lucasstml.towerrogue.core.Rng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The battlefield grid plus the lanes derived from the generated paths.
 * Coordinates are in tiles: cell (x, y) spans [x, x+1) × [y, y+1), so its center is (x+0.5, y+0.5).
 * The renderer decides how many pixels a tile is; the simulation never knows.
 */
public final class BattleMap {
    public static final int COLS = 15;
    public static final int ROWS = 10;

    public enum Tile { GRASS, PATH, ROCK, RUNE }

    private final Tile[][] tiles = new Tile[COLS][ROWS];
    private final List<Lane> groundLanes = new ArrayList<>();
    private final List<Lane> airLanes = new ArrayList<>();
    private final List<List<int[]>> pathCells = new ArrayList<>();
    private final List<double[]> spawns = new ArrayList<>();
    private int baseX;
    private int baseY;

    private BattleMap() {
    }

    public static BattleMap generate(Rng rng, boolean twoLanes) {
        BattleMap m = new BattleMap();
        for (Tile[] col : m.tiles) java.util.Arrays.fill(col, Tile.GRASS);

        PathGenerator.Result r = PathGenerator.generate(rng, COLS, ROWS, twoLanes);
        int[] base = r.main().get(r.main().size() - 1);
        m.baseX = base[0];
        m.baseY = base[1];
        double bx = base[0] + 0.5, by = base[1] + 0.5;

        // Main lane: enters from off-screen on the left.
        int[] first = r.main().get(0);
        double[] spawnA = {-0.8, first[1] + 0.5};
        m.addLane(spawnA, r.main(), bx, by);

        if (r.second() != null) {
            int[] s0 = r.second().get(0);
            double[] spawnB = {s0[0] + 0.5, r.secondFromTop() ? -0.8 : ROWS + 0.8};
            List<int[]> cells = new ArrayList<>(r.second());
            cells.addAll(r.main().subList(r.joinIndex(), r.main().size()));
            m.addLane(spawnB, cells, bx, by);
            m.pathCells.set(1, r.second());
        }
        for (List<int[]> cells : m.pathCells) {
            for (int[] c : cells) m.tiles[c[0]][c[1]] = Tile.PATH;
        }
        m.decorate(rng);
        return m;
    }

    private void addLane(double[] spawn, List<int[]> cells, double bx, double by) {
        List<double[]> pts = new ArrayList<>();
        pts.add(spawn);
        for (int[] c : cells) pts.add(new double[]{c[0] + 0.5, c[1] + 0.5});
        groundLanes.add(new Lane(pts));
        List<double[]> air = new ArrayList<>();
        air.add(spawn);
        air.add(new double[]{bx, by});
        airLanes.add(new Lane(air));
        pathCells.add(cells);
        spawns.add(spawn);
    }

    /** Scatters rocks (blocked) and a few rune tiles (empowering) near the path. */
    private void decorate(Rng rng) {
        List<int[]> nearPath = new ArrayList<>();
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                if (tiles[x][y] != Tile.GRASS) continue;
                boolean near = nearPath(x, y);
                if (near) nearPath.add(new int[]{x, y});
                if (rng.chance(near ? 0.03 : 0.11)) tiles[x][y] = Tile.ROCK;
            }
        }
        List<int[]> candidates = new ArrayList<>();
        for (int[] c : nearPath) if (tiles[c[0]][c[1]] == Tile.GRASS) candidates.add(c);
        rng.shuffle(candidates);
        int runes = Math.min(3, candidates.size());
        for (int i = 0; i < runes; i++) tiles[candidates.get(i)[0]][candidates.get(i)[1]] = Tile.RUNE;
    }

    private boolean nearPath(int x, int y) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int ax = x + dx, ay = y + dy;
                if (inBounds(ax, ay) && tiles[ax][ay] == Tile.PATH) return true;
            }
        }
        return false;
    }

    public static boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < COLS && y < ROWS;
    }

    public Tile tile(int x, int y) {
        return tiles[x][y];
    }

    public boolean buildable(int x, int y) {
        return inBounds(x, y) && (tiles[x][y] == Tile.GRASS || tiles[x][y] == Tile.RUNE);
    }

    public List<Lane> groundLanes() {
        return Collections.unmodifiableList(groundLanes);
    }

    public List<Lane> airLanes() {
        return Collections.unmodifiableList(airLanes);
    }

    public List<List<int[]>> pathCells() {
        return Collections.unmodifiableList(pathCells);
    }

    public List<double[]> spawns() {
        return Collections.unmodifiableList(spawns);
    }

    public int laneCount() {
        return groundLanes.size();
    }

    public int baseX() {
        return baseX;
    }

    public int baseY() {
        return baseY;
    }
}
