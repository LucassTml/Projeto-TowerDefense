package io.github.lucasstml.towerrogue.battle;

import io.github.lucasstml.towerrogue.content.TargetMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Spatial queries and target priority (Strategy per {@link TargetMode}).
 * A linear scan is used on purpose: with at most a few hundred enemies it is faster and
 * simpler than maintaining a spatial index, and its iteration order is deterministic.
 */
public final class Targeting {

    public boolean inRange(Tower t, Enemy e) {
        if (!e.alive) return false;
        if (e.flying() && !t.hitsAir()) return false;
        double r = t.range() + e.radius() * 0.5;
        double dx = e.x - t.x(), dy = e.y - t.y();
        return dx * dx + dy * dy <= r * r;
    }

    public List<Enemy> allInRange(Tower t, List<Enemy> enemies) {
        List<Enemy> out = new ArrayList<>();
        for (Enemy e : enemies) if (inRange(t, e)) out.add(e);
        return out;
    }

    /** Up to {@code count} distinct targets ordered by the tower's priority. */
    public List<Enemy> select(Tower t, List<Enemy> enemies, int count) {
        List<Enemy> candidates = allInRange(t, enemies);
        if (candidates.size() <= 1 || count <= 0) return candidates;
        candidates.sort(comparator(t));
        return candidates.size() > count ? new ArrayList<>(candidates.subList(0, count)) : candidates;
    }

    private Comparator<Enemy> comparator(Tower t) {
        Comparator<Enemy> c = switch (t.targetMode()) {
            case FIRST -> Comparator.comparingDouble(Enemy::remaining);
            case LAST -> Comparator.comparingDouble((Enemy e) -> e.remaining()).reversed();
            case STRONGEST -> Comparator.comparingDouble((Enemy e) -> e.hp + e.shield).reversed();
            case WEAKEST -> Comparator.comparingDouble((Enemy e) -> e.hp + e.shield);
            case CLOSEST -> Comparator.comparingDouble((Enemy e) -> (e.x - t.x()) * (e.x - t.x()) + (e.y - t.y()) * (e.y - t.y()));
        };
        // Invulnerable enemies go last so towers don't waste shots on a phasing boss.
        return Comparator.comparing((Enemy e) -> e.invulnerable > 0).thenComparing(c).thenComparingInt(e -> e.id);
    }

    public Enemy nearestExcept(double x, double y, double range, boolean hitAir, List<Enemy> exclude, List<Enemy> enemies) {
        Enemy best = null;
        double bestD2 = range * range;
        for (Enemy e : enemies) {
            if (!e.alive || (e.flying() && !hitAir) || exclude.contains(e)) continue;
            double d2 = (e.x - x) * (e.x - x) + (e.y - y) * (e.y - y);
            if (d2 <= bestD2) {
                bestD2 = d2;
                best = e;
            }
        }
        return best;
    }

    /** Enemies whose body overlaps the circle; {@code layer}: 0 ground only, 1 air only, 2 both. */
    public List<Enemy> inRadius(double x, double y, double r, int layer, List<Enemy> enemies) {
        List<Enemy> out = new ArrayList<>();
        for (Enemy e : enemies) {
            if (!e.alive) continue;
            if (layer == 0 && e.flying()) continue;
            if (layer == 1 && !e.flying()) continue;
            double rr = r + e.radius() * 0.5;
            if ((e.x - x) * (e.x - x) + (e.y - y) * (e.y - y) <= rr * rr) out.add(e);
        }
        return out;
    }
}
