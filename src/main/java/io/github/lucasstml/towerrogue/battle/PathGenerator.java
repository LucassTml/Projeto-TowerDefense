package io.github.lucasstml.towerrogue.battle;

import io.github.lucasstml.towerrogue.core.Rng;

import java.util.ArrayList;
import java.util.List;

/**
 * Procedural enemy paths.
 *
 * <p>The main path is a randomized depth-first self-avoiding walk from the left edge to the
 * right edge. Two rules give readable, winding roads: a new cell may not touch (4-neighbour)
 * any earlier cell except the one it came from, and a walk only succeeds if its length is
 * within [min, max]. Dead ends backtrack; a step budget bounds the worst case, and a
 * straight path is the last-resort fallback so generation can never fail.
 *
 * <p>An optional second lane starts on the top or bottom edge and walks until it is next to
 * the main path, then merges into it.
 */
final class PathGenerator {
    private static final int[] DX = {1, 0, 0, -1};
    private static final int[] DY = {0, -1, 1, 0};

    record Result(List<int[]> main, List<int[]> second, int joinIndex, boolean secondFromTop) {
    }

    private interface Goal {
        boolean reached(int x, int y);
    }

    private PathGenerator() {
    }

    static Result generate(Rng rng, int cols, int rows, boolean twoLanes) {
        List<int[]> main = mainPath(rng, cols, rows);
        if (!twoLanes) return new Result(main, null, -1, false);

        boolean[][] onMain = new boolean[cols][rows];
        int[][] mainIndex = new int[cols][rows];
        for (int i = 0; i < main.size(); i++) {
            int[] c = main.get(i);
            onMain[c[0]][c[1]] = true;
            mainIndex[c[0]][c[1]] = i;
        }
        int minJoin = 5, maxJoin = main.size() - 7;
        for (int attempt = 0; attempt < 120 && maxJoin > minJoin; attempt++) {
            boolean fromTop = rng.nextBoolean();
            int sx = rng.range(3, cols - 6);
            int sy = fromTop ? 0 : rows - 1;
            if (onMain[sx][sy] || touches(onMain, sx, sy, cols, rows)) continue;
            int[] joinAt = new int[1];
            Goal goal = (x, y) -> {
                for (int d = 0; d < 4; d++) {
                    int ax = x + DX[d], ay = y + DY[d];
                    if (in(ax, ay, cols, rows) && onMain[ax][ay]) {
                        int k = mainIndex[ax][ay];
                        if (k >= minJoin && k <= maxJoin) {
                            joinAt[0] = k;
                            return true;
                        }
                    }
                }
                return false;
            };
            double[] weights = fromTop ? new double[]{1.5, 0.4, 3.0, 1.5} : new double[]{1.5, 3.0, 0.4, 1.5};
            List<int[]> second = walk(rng, cols, rows, onMain, sx, sy, 4, 16, goal, weights, onMain);
            if (second != null) {
                // Re-evaluate the goal for the final cell to read the join index it matched.
                int[] last = second.get(second.size() - 1);
                goal.reached(last[0], last[1]);
                return new Result(main, second, joinAt[0], fromTop);
            }
        }
        return new Result(main, null, -1, false);
    }

    private static List<int[]> mainPath(Rng rng, int cols, int rows) {
        int minLen = (int) (cols * 1.9);
        int maxLen = (int) (cols * 3.0);
        boolean[][] blocked = new boolean[cols][rows];
        double[] weights = {3.0, 2.0, 2.0, 0.7};
        for (int attempt = 0; attempt < 300; attempt++) {
            int sy = rng.range(1, rows - 2);
            for (int y = 0; y < rows; y++) blocked[0][y] = y != sy;
            List<int[]> p = walk(rng, cols, rows, blocked, 0, sy, minLen, maxLen, (x, y) -> x == cols - 1, weights, null);
            if (p != null) return p;
        }
        List<int[]> straight = new ArrayList<>();
        for (int x = 0; x < cols; x++) straight.add(new int[]{x, rows / 2});
        return straight;
    }

    /**
     * Iterative randomized DFS. Each frame keeps its own shuffled direction order so
     * backtracking resumes where it left off.
     */
    private static List<int[]> walk(Rng rng, int cols, int rows, boolean[][] blocked, int sx, int sy,
                                    int minLen, int maxLen, Goal goal, double[] weights, boolean[][] avoidTouch) {
        boolean[][] used = new boolean[cols][rows];
        List<int[]> stack = new ArrayList<>(); // {x, y, nextDirIndex, order...}
        stack.add(frame(sx, sy, rng, weights));
        used[sx][sy] = true;
        int budget = 8000;
        while (!stack.isEmpty() && budget-- > 0) {
            int[] f = stack.get(stack.size() - 1);
            int len = stack.size();
            if (len >= minLen && goal.reached(f[0], f[1])) {
                List<int[]> path = new ArrayList<>(len);
                for (int[] fr : stack) path.add(new int[]{fr[0], fr[1]});
                return path;
            }
            if (f[2] >= 4 || len >= maxLen) {
                stack.remove(stack.size() - 1);
                used[f[0]][f[1]] = false;
                continue;
            }
            int d = f[3 + f[2]++];
            int nx = f[0] + DX[d], ny = f[1] + DY[d];
            if (!in(nx, ny, cols, rows) || used[nx][ny] || blocked[nx][ny]) continue;
            if (touchesOther(used, nx, ny, f[0], f[1], cols, rows)) continue;
            boolean reaches = goal.reached(nx, ny);
            if (avoidTouch != null && !reaches && touches(avoidTouch, nx, ny, cols, rows)) continue;
            if (reaches && len + 1 < minLen) continue;
            used[nx][ny] = true;
            stack.add(frame(nx, ny, rng, weights));
        }
        return null;
    }

    /** Frame layout: x, y, cursor, then a weighted random permutation of the 4 directions. */
    private static int[] frame(int x, int y, Rng rng, double[] weights) {
        int[] f = new int[7];
        f[0] = x;
        f[1] = y;
        double[] w = weights.clone();
        for (int k = 0; k < 4; k++) {
            double total = 0;
            for (double v : w) total += v;
            double roll = rng.nextDouble() * total;
            int pick = 0;
            for (int i = 0; i < 4; i++) {
                if (w[i] <= 0) continue;
                pick = i;
                roll -= w[i];
                if (roll < 0) break;
            }
            f[3 + k] = pick;
            w[pick] = 0;
        }
        return f;
    }

    private static boolean touchesOther(boolean[][] used, int x, int y, int px, int py, int cols, int rows) {
        for (int d = 0; d < 4; d++) {
            int ax = x + DX[d], ay = y + DY[d];
            if (ax == px && ay == py) continue;
            if (in(ax, ay, cols, rows) && used[ax][ay]) return true;
        }
        return false;
    }

    private static boolean touches(boolean[][] grid, int x, int y, int cols, int rows) {
        for (int d = 0; d < 4; d++) {
            int ax = x + DX[d], ay = y + DY[d];
            if (in(ax, ay, cols, rows) && grid[ax][ay]) return true;
        }
        return false;
    }

    private static boolean in(int x, int y, int cols, int rows) {
        return x >= 0 && y >= 0 && x < cols && y < rows;
    }
}
