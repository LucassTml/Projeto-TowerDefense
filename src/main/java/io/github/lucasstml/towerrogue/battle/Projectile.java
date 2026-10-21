package io.github.lucasstml.towerrogue.battle;

import io.github.lucasstml.towerrogue.content.OnHitDef;

import java.util.List;

/**
 * A projectile in flight. Homing projectiles track their target; ground-targeted ones
 * (mortar) fly to a point predicted at launch and can miss fast enemies, which is the
 * intended trade-off for their huge splash.
 */
public final class Projectile {
    public final Tower source;
    public final boolean homing;
    public final double splash;
    public final boolean crit;
    final double damage;
    final List<OnHitDef> onHit;
    final double speed;
    Enemy target;
    public double x, y;
    public final double startX, startY;
    public double tx, ty;
    public final double totalDistance;
    boolean alive = true;

    Projectile(Tower source, Enemy target, double damage, boolean crit, double speed, double splash,
               boolean homing, double tx, double ty) {
        this.source = source;
        this.target = target;
        this.damage = damage;
        this.crit = crit;
        this.speed = speed;
        this.splash = splash;
        this.homing = homing;
        this.onHit = source.onHit();
        this.x = this.startX = source.x();
        this.y = this.startY = source.y() - 0.25;
        this.tx = tx;
        this.ty = ty;
        this.totalDistance = Math.max(0.01, Math.hypot(tx - x, ty - y));
    }

    /** 0..1 progress of a ground-targeted shell, used by the renderer to draw an arc. */
    public double flightProgress() {
        double done = Math.hypot(x - startX, y - startY);
        return Math.min(1, done / totalDistance);
    }

    void update(Battle b, double dt) {
        if (!alive) return;
        if (homing && target != null && target.alive) {
            tx = target.x;
            ty = target.y;
        }
        double dx = tx - x, dy = ty - y;
        double d = Math.sqrt(dx * dx + dy * dy);
        double step = speed * dt;
        if (d <= step) {
            x = tx;
            y = ty;
            impact(b);
        } else {
            x += dx / d * step;
            y += dy / d * step;
        }
    }

    private void impact(Battle b) {
        alive = false;
        if (splash > 0) {
            boolean air = target != null && target.flying();
            b.emit(Fx.at(Fx.Type.EXPLOSION, x, y, splash, source.def.color()));
            int layer = air ? 1 : 0;
            for (Enemy e : b.targeting().inRadius(x, y, splash, layer, b.enemies())) {
                b.strike(source, e, damage, crit, onHit);
            }
            return;
        }
        if (homing && target != null && target.alive) {
            b.strike(source, target, damage, crit, onHit);
            return;
        }
        // Homing target died mid-flight, or a ground shell landed: hit whatever is right there.
        List<Enemy> near = b.targeting().inRadius(x, y, 0.3, source.hitsAir() ? 2 : 0, b.enemies());
        if (!near.isEmpty()) b.strike(source, near.get(0), damage, crit, onHit);
    }
}
