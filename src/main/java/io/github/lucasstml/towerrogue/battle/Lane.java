package io.github.lucasstml.towerrogue.battle;

import java.util.List;

/**
 * A polyline enemies walk along, parameterised by travelled distance.
 * Storing cumulative segment lengths lets an enemy be described by a single number
 * ({@code dist}); position, "closest to base" targeting and blink are all O(log n).
 */
public final class Lane {
    private final double[] xs;
    private final double[] ys;
    private final double[] cumulative;
    private final double length;

    public Lane(List<double[]> points) {
        int n = points.size();
        xs = new double[n];
        ys = new double[n];
        cumulative = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = points.get(i)[0];
            ys[i] = points.get(i)[1];
            if (i > 0) {
                double dx = xs[i] - xs[i - 1], dy = ys[i] - ys[i - 1];
                cumulative[i] = cumulative[i - 1] + Math.sqrt(dx * dx + dy * dy);
            }
        }
        length = cumulative[n - 1];
    }

    public double length() {
        return length;
    }

    public int pointCount() {
        return xs.length;
    }

    public double pointX(int i) {
        return xs[i];
    }

    public double pointY(int i) {
        return ys[i];
    }

    /** Writes the position at distance {@code d} along the lane into {@code out[0..1]}. */
    public void positionAt(double d, double[] out) {
        if (d <= 0) {
            out[0] = xs[0];
            out[1] = ys[0];
            return;
        }
        if (d >= length) {
            out[0] = xs[xs.length - 1];
            out[1] = ys[ys.length - 1];
            return;
        }
        int lo = 0, hi = cumulative.length - 1;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (cumulative[mid] <= d) lo = mid;
            else hi = mid;
        }
        double seg = cumulative[hi] - cumulative[lo];
        double t = seg <= 0 ? 0 : (d - cumulative[lo]) / seg;
        out[0] = xs[lo] + (xs[hi] - xs[lo]) * t;
        out[1] = ys[lo] + (ys[hi] - ys[lo]) * t;
    }

    /** Distance along the lane of the point closest to (x, y); used to place ground summons from air. */
    public double closestDistance(double x, double y) {
        double best = 0, bestD2 = Double.MAX_VALUE;
        for (int i = 1; i < xs.length; i++) {
            double ax = xs[i - 1], ay = ys[i - 1], bx = xs[i], by = ys[i];
            double vx = bx - ax, vy = by - ay;
            double len2 = vx * vx + vy * vy;
            double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * vx + (y - ay) * vy) / len2));
            double px = ax + vx * t, py = ay + vy * t;
            double d2 = (px - x) * (px - x) + (py - y) * (py - y);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = cumulative[i - 1] + Math.sqrt(len2) * t;
            }
        }
        return best;
    }
}
