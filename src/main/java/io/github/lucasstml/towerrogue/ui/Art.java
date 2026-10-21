package io.github.lucasstml.towerrogue.ui;

import io.github.lucasstml.towerrogue.battle.Enemy;
import io.github.lucasstml.towerrogue.content.AffixDef;
import io.github.lucasstml.towerrogue.content.EnemyDef;
import io.github.lucasstml.towerrogue.content.EnemyShape;
import io.github.lucasstml.towerrogue.content.StatusType;
import io.github.lucasstml.towerrogue.content.TowerDef;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Procedural art for towers and enemies (pure Java2D, no sprites). Sizes are in pixels. */
public final class Art {
    private Art() {
    }

    // ================================================================== towers

    /**
     * @param s     tile size in pixels
     * @param aim   turret angle in radians
     * @param level 1..3, drawn as pips; {@code specialized} adds a gem
     */
    public static void tower(Graphics2D g, TowerDef def, double cx, double cy, double s, double aim, int level,
                             boolean specialized, double time, boolean disabled) {
        Color c = Theme.rgb(def.color());
        // stone platform
        double p = s * 0.84;
        Draw.fillRound(g, cx - p / 2 + 2, cy - p / 2 + 4, p, p, s * 0.25, new Color(0, 0, 0, 80));
        g.setPaint(new GradientPaint((float) cx, (float) (cy - p / 2), new Color(0x4A5063), (float) cx, (float) (cy + p / 2), new Color(0x2A2E3B)));
        g.fill(new RoundRectangle2D.Double(cx - p / 2, cy - p / 2, p, p, s * 0.25, s * 0.25));
        Draw.strokeRound(g, cx - p / 2, cy - p / 2, p, p, s * 0.25, new Color(0x5E6680), 1.2);

        switch (def.art()) {
            case "archer" -> archer(g, cx, cy, s, aim, c);
            case "cannon" -> cannon(g, cx, cy, s, aim, c);
            case "frost" -> frost(g, cx, cy, s, c, time);
            case "brazier" -> brazier(g, cx, cy, s, time);
            case "venom" -> venom(g, cx, cy, s, aim, c, time);
            case "tesla" -> tesla(g, cx, cy, s, c, time);
            case "mortar" -> mortar(g, cx, cy, s, aim, c);
            case "ballista" -> ballista(g, cx, cy, s, aim, c);
            case "beacon" -> beacon(g, cx, cy, s, c, time);
            case "alchemist" -> alchemist(g, cx, cy, s, c, time);
            case "obelisk" -> obelisk(g, cx, cy, s, c, time);
            default -> Draw.circle(g, cx, cy, s * 0.25, c);
        }

        for (int i = 0; i < level; i++) {
            double px = cx - (level - 1) * s * 0.08 + i * s * 0.16;
            Draw.circle(g, px, cy + p / 2 - s * 0.08, s * 0.055, Theme.GOLD);
        }
        if (specialized) {
            Path2D gem = new Path2D.Double();
            double gx = cx + p / 2 - s * 0.1, gy = cy - p / 2 + s * 0.1, r = s * 0.09;
            gem.moveTo(gx, gy - r);
            gem.lineTo(gx + r, gy);
            gem.lineTo(gx, gy + r);
            gem.lineTo(gx - r, gy);
            gem.closePath();
            g.setColor(Theme.BOSS);
            g.fill(gem);
            g.setColor(Color.WHITE);
            g.setStroke(Draw.stroke(1));
            g.draw(gem);
        }
        if (disabled) {
            Draw.fillRound(g, cx - p / 2, cy - p / 2, p, p, s * 0.25, new Color(20, 30, 50, 150));
            for (int i = 0; i < 3; i++) {
                double a = time * 3 + i * Math.PI * 2 / 3;
                Icons.star(g, cx + Math.cos(a) * s * 0.22, cy - s * 0.3 + Math.sin(a) * s * 0.08, s * 0.07, new Color(0xBFF0FF));
            }
        }
    }

    private static void rotated(Graphics2D g, double cx, double cy, double angle, Runnable draw) {
        AffineTransform old = g.getTransform();
        g.rotate(angle, cx, cy);
        draw.run();
        g.setTransform(old);
    }

    private static void archer(Graphics2D g, double cx, double cy, double s, double aim, Color c) {
        Draw.circle(g, cx, cy, s * 0.26, Theme.shade(c, 0.55));
        Draw.circle(g, cx, cy, s * 0.21, Theme.shade(c, 0.85));
        rotated(g, cx, cy, aim, () -> {
            g.setColor(new Color(0x6B4A2A));
            g.setStroke(Draw.stroke(s * 0.06));
            g.draw(new Arc2D.Double(cx - s * 0.1, cy - s * 0.24, s * 0.36, s * 0.48, -70, 140, Arc2D.OPEN));
            Draw.line(g, cx + s * 0.14, cy - s * 0.22, cx + s * 0.14, cy + s * 0.22, new Color(0xE9E6DF), 1);
            Draw.line(g, cx - s * 0.12, cy, cx + s * 0.3, cy, new Color(0xD9D4C7), s * 0.035);
            Draw.circle(g, cx + s * 0.31, cy, s * 0.03, new Color(0xB0B8C8));
        });
    }

    private static void cannon(Graphics2D g, double cx, double cy, double s, double aim, Color c) {
        Draw.circle(g, cx, cy, s * 0.27, Theme.shade(c, 0.6));
        rotated(g, cx, cy, aim, () -> {
            g.setPaint(new GradientPaint((float) cx, (float) (cy - s * 0.09), new Color(0x8D8F99), (float) cx, (float) (cy + s * 0.09), new Color(0x3A3C45)));
            g.fill(new RoundRectangle2D.Double(cx - s * 0.05, cy - s * 0.09, s * 0.42, s * 0.18, s * 0.06, s * 0.06));
            Draw.fillRound(g, cx + s * 0.32, cy - s * 0.11, s * 0.08, s * 0.22, s * 0.04, new Color(0x2A2C33));
        });
        Draw.circle(g, cx, cy, s * 0.16, Theme.shade(c, 0.9));
        Draw.circle(g, cx - s * 0.04, cy - s * 0.05, s * 0.05, new Color(255, 255, 255, 70));
    }

    private static void frost(Graphics2D g, double cx, double cy, double s, Color c, double time) {
        double pulse = 0.5 + 0.5 * Math.sin(time * 2.5);
        glow(g, cx, cy, s * (0.38 + 0.04 * pulse), c, 0.35);
        Path2D crystal = new Path2D.Double();
        crystal.moveTo(cx, cy - s * 0.36);
        crystal.lineTo(cx + s * 0.14, cy - s * 0.02);
        crystal.lineTo(cx, cy + s * 0.26);
        crystal.lineTo(cx - s * 0.14, cy - s * 0.02);
        crystal.closePath();
        g.setPaint(new GradientPaint((float) cx, (float) (cy - s * 0.36), new Color(0xE6FAFF), (float) cx, (float) (cy + s * 0.26), Theme.shade(c, 0.7)));
        g.fill(crystal);
        g.setColor(new Color(255, 255, 255, 160));
        g.setStroke(Draw.stroke(1));
        g.draw(crystal);
        for (int k = -1; k <= 1; k += 2) {
            Path2D side = new Path2D.Double();
            double bx = cx + k * s * 0.17;
            side.moveTo(bx, cy - s * 0.16);
            side.lineTo(bx + k * s * 0.07, cy + s * 0.04);
            side.lineTo(bx, cy + s * 0.16);
            side.lineTo(bx - k * s * 0.05, cy + s * 0.02);
            side.closePath();
            g.setColor(Theme.shade(c, 0.95));
            g.fill(side);
        }
    }

    private static void brazier(Graphics2D g, double cx, double cy, double s, double time) {
        glow(g, cx, cy - s * 0.05, s * 0.4, new Color(0xFF8A3D), 0.4 + 0.1 * Math.sin(time * 7));
        g.setColor(new Color(0x3A2A22));
        g.fill(new Arc2D.Double(cx - s * 0.28, cy - s * 0.2, s * 0.56, s * 0.46, 180, 180, Arc2D.CHORD));
        Draw.line(g, cx - s * 0.28, cy + s * 0.03, cx + s * 0.28, cy + s * 0.03, new Color(0x7A5A44), s * 0.05);
        Color[] colors = {new Color(0xFF5A2A), new Color(0xFF8A3D), new Color(0xFFD34D)};
        for (int i = 0; i < 3; i++) {
            double ph = time * (6 + i) + i * 1.7;
            double h = s * (0.34 - i * 0.07) * (1 + 0.15 * Math.sin(ph));
            double w = s * (0.22 - i * 0.05);
            Path2D f = new Path2D.Double();
            double bx = cx + Math.sin(ph * 0.7) * s * 0.02;
            f.moveTo(bx, cy - h);
            f.curveTo(bx + w, cy - h * 0.3, bx + w * 0.6, cy + s * 0.02, bx, cy + s * 0.02);
            f.curveTo(bx - w * 0.6, cy + s * 0.02, bx - w, cy - h * 0.3, bx, cy - h);
            g.setColor(colors[i]);
            g.fill(f);
        }
    }

    private static void venom(Graphics2D g, double cx, double cy, double s, double aim, Color c, double time) {
        double b = 1 + 0.04 * Math.sin(time * 3);
        Draw.circle(g, cx, cy, s * 0.27 * b, Theme.shade(c, 0.45));
        Draw.circle(g, cx, cy, s * 0.22 * b, Theme.shade(c, 0.75));
        for (int i = 0; i < 5; i++) {
            double a = i * 1.3 + 0.4;
            Draw.circle(g, cx + Math.cos(a) * s * 0.13, cy + Math.sin(a) * s * 0.13, s * 0.035, Theme.shade(c, 1.2));
        }
        double mx = cx + Math.cos(aim) * s * 0.14, my = cy + Math.sin(aim) * s * 0.14;
        Draw.circle(g, mx, my, s * 0.08, new Color(0x1E3A14));
        Draw.circle(g, mx, my, s * 0.04, Theme.shade(c, 1.3));
    }

    private static void tesla(Graphics2D g, double cx, double cy, double s, Color c, double time) {
        Draw.circle(g, cx, cy + s * 0.12, s * 0.24, new Color(0x2E2A40));
        for (int i = 0; i < 3; i++) {
            double y = cy + s * 0.12 - i * s * 0.13;
            g.setColor(Theme.shade(c, 0.6 + i * 0.15));
            g.setStroke(Draw.stroke(s * 0.05));
            g.draw(new Ellipse2D.Double(cx - s * (0.18 - i * 0.03), y - s * 0.05, s * (0.36 - i * 0.06), s * 0.1));
        }
        double orbY = cy - s * 0.27;
        glow(g, cx, orbY, s * 0.2, c, 0.5 + 0.2 * Math.sin(time * 11));
        Draw.circle(g, cx, orbY, s * 0.09, new Color(0xF0E0FF));
        int seed = (int) (time * 12);
        for (int i = 0; i < 2; i++) {
            double a = ((seed * 37 + i * 101) % 360) * Math.PI / 180;
            double ex = cx + Math.cos(a) * s * 0.22, ey = orbY + Math.sin(a) * s * 0.16;
            Draw.line(g, cx, orbY, (cx + ex) / 2 + Math.sin(a * 3) * s * 0.05, (orbY + ey) / 2, c, 1.2);
            Draw.line(g, (cx + ex) / 2 + Math.sin(a * 3) * s * 0.05, (orbY + ey) / 2, ex, ey, c, 1.2);
        }
    }

    private static void mortar(Graphics2D g, double cx, double cy, double s, double aim, Color c) {
        Draw.circle(g, cx, cy, s * 0.29, Theme.shade(c, 0.5));
        Draw.circle(g, cx, cy, s * 0.24, Theme.shade(c, 0.8));
        double ox = Math.cos(aim) * s * 0.05, oy = Math.sin(aim) * s * 0.05;
        Draw.circle(g, cx + ox, cy + oy, s * 0.15, new Color(0x2A2C22));
        Draw.circle(g, cx + ox, cy + oy, s * 0.1, new Color(0x111108));
        Draw.ring(g, cx + ox, cy + oy, s * 0.15, Theme.shade(c, 1.15), 1.5);
    }

    private static void ballista(Graphics2D g, double cx, double cy, double s, double aim, Color c) {
        Draw.circle(g, cx, cy, s * 0.2, Theme.shade(c, 0.55));
        rotated(g, cx, cy, aim, () -> {
            Draw.fillRound(g, cx - s * 0.2, cy - s * 0.05, s * 0.5, s * 0.1, s * 0.04, Theme.shade(c, 0.8));
            g.setColor(new Color(0x5A3A20));
            g.setStroke(Draw.stroke(s * 0.06));
            g.draw(new Arc2D.Double(cx - s * 0.02, cy - s * 0.34, s * 0.3, s * 0.68, -80, 160, Arc2D.OPEN));
            Draw.line(g, cx + s * 0.17, cy - s * 0.32, cx - s * 0.05, cy, new Color(0xE9E6DF), 1);
            Draw.line(g, cx + s * 0.17, cy + s * 0.32, cx - s * 0.05, cy, new Color(0xE9E6DF), 1);
            Draw.line(g, cx - s * 0.05, cy, cx + s * 0.38, cy, new Color(0xB0B8C8), s * 0.04);
        });
    }

    private static void beacon(Graphics2D g, double cx, double cy, double s, Color c, double time) {
        double pulse = (time * 0.8) % 1;
        Draw.ring(g, cx, cy, s * (0.2 + pulse * 0.3), Theme.alpha(c, 0.6 * (1 - pulse)), 2);
        g.setPaint(new GradientPaint((float) (cx - s * 0.1), (float) cy, Theme.shade(c, 1.2), (float) (cx + s * 0.1), (float) cy, Theme.shade(c, 0.6)));
        g.fill(new RoundRectangle2D.Double(cx - s * 0.09, cy - s * 0.2, s * 0.18, s * 0.42, s * 0.05, s * 0.05));
        glow(g, cx, cy - s * 0.26, s * 0.18, c, 0.6);
        Icons.star(g, cx, cy - s * 0.26, s * 0.1, new Color(0xFFF2C0));
    }

    private static void alchemist(Graphics2D g, double cx, double cy, double s, Color c, double time) {
        Draw.fillRound(g, cx - s * 0.07, cy - s * 0.3, s * 0.14, s * 0.18, s * 0.04, new Color(0xC8D4E8));
        Draw.circle(g, cx, cy + s * 0.06, s * 0.22, new Color(0xC8D4E8));
        Draw.circle(g, cx, cy + s * 0.07, s * 0.19, Theme.shade(c, 0.9));
        for (int i = 0; i < 3; i++) {
            double t = (time * 0.7 + i * 0.33) % 1;
            Draw.circle(g, cx + Math.sin(i * 2.1 + time) * s * 0.07, cy + s * 0.16 - t * s * 0.3, s * 0.03 * (1 - t * 0.5),
                    new Color(255, 255, 255, (int) (200 * (1 - t))));
        }
        Draw.circle(g, cx - s * 0.08, cy, s * 0.04, new Color(255, 255, 255, 120));
    }

    private static void obelisk(Graphics2D g, double cx, double cy, double s, Color c, double time) {
        Path2D ob = new Path2D.Double();
        ob.moveTo(cx, cy - s * 0.4);
        ob.lineTo(cx + s * 0.12, cy - s * 0.26);
        ob.lineTo(cx + s * 0.16, cy + s * 0.28);
        ob.lineTo(cx - s * 0.16, cy + s * 0.28);
        ob.lineTo(cx - s * 0.12, cy - s * 0.26);
        ob.closePath();
        g.setPaint(new GradientPaint((float) (cx - s * 0.16), (float) cy, new Color(0x3A2A5A), (float) (cx + s * 0.16), (float) cy, new Color(0x1A1030)));
        g.fill(ob);
        g.setColor(Theme.shade(c, 0.9));
        g.setStroke(Draw.stroke(1));
        g.draw(ob);
        double glowA = 0.5 + 0.3 * Math.sin(time * 3);
        glow(g, cx, cy - s * 0.05, s * 0.16, c, glowA);
        Draw.circle(g, cx, cy - s * 0.05, s * 0.05, new Color(0xE8D8FF));
        AffineTransform old = g.getTransform();
        g.rotate(time * 1.2, cx, cy - s * 0.05);
        g.setColor(Theme.alpha(c, 0.8));
        g.setStroke(Draw.stroke(1.4));
        g.draw(new Arc2D.Double(cx - s * 0.26, cy - s * 0.31, s * 0.52, s * 0.52, 0, 110, Arc2D.OPEN));
        g.draw(new Arc2D.Double(cx - s * 0.26, cy - s * 0.31, s * 0.52, s * 0.52, 180, 110, Arc2D.OPEN));
        g.setTransform(old);
    }

    private static void glow(Graphics2D g, double cx, double cy, double r, Color c, double alpha) {
        if (r <= 0) return;
        g.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) r, new float[]{0f, 1f},
                new Color[]{Theme.alpha(c, Math.max(0, Math.min(1, alpha))), Theme.alpha(c, 0)}));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    // ================================================================== enemies

    public static Shape enemyShape(EnemyShape shape, double cx, double cy, double r, double facing) {
        return switch (shape) {
            case CIRCLE -> new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
            case SQUARE -> new RoundRectangle2D.Double(cx - r * 0.9, cy - r * 0.9, r * 1.8, r * 1.8, r * 0.6, r * 0.6);
            case TRIANGLE -> polygon(cx, cy, r * 1.1, 3, facing);
            case DIAMOND -> polygon(cx, cy, r * 1.05, 4, -Math.PI / 2);
            case HEXAGON -> polygon(cx, cy, r, 6, 0);
            case STAR -> Icons.starShape(cx, cy, r * 1.15, r * 0.7, 7, -Math.PI / 2);
        };
    }

    private static Path2D polygon(double cx, double cy, double r, int n, double rot) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < n; i++) {
            double a = rot + i * Math.PI * 2 / n;
            if (i == 0) p.moveTo(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
            else p.lineTo(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
        }
        p.closePath();
        return p;
    }

    /** Draws an enemy body (no health bar). {@code dirX/dirY} is the movement direction. */
    public static void enemy(Graphics2D g, Enemy e, double cx, double cy, double r, double dirX, double dirY, double time) {
        EnemyDef d = e.def;
        Color c = Theme.rgb(d.color());
        double facing = Math.atan2(dirY, dirX);
        double bob = d.flying() ? Math.sin(time * 5 + e.id) * r * 0.18 : 0;
        // shadow
        double sh = d.flying() ? r * 0.75 : r * 0.95;
        g.setColor(new Color(0, 0, 0, d.flying() ? 55 : 80));
        g.fill(new Ellipse2D.Double(cx - sh, cy + r * (d.flying() ? 0.9 : 0.55), sh * 2, sh * 0.6));
        double y = cy - (d.flying() ? r * 0.55 : 0) + bob;

        if (e.elite()) {
            double pr = r * 1.45;
            g.setPaint(new RadialGradientPaint((float) cx, (float) y, (float) pr, new float[]{0.55f, 1f},
                    new Color[]{Theme.alpha(Theme.ELITE, 0.35), Theme.alpha(Theme.ELITE, 0)}));
            g.fill(new Ellipse2D.Double(cx - pr, y - pr, pr * 2, pr * 2));
        }
        if (d.flying()) wings(g, cx, y, r, c, time + e.id);

        Shape body = enemyShape(d.shape(), cx, y, r, facing);
        g.setPaint(new GradientPaint((float) cx, (float) (y - r), Theme.shade(c, 1.25), (float) cx, (float) (y + r), Theme.shade(c, 0.7)));
        g.fill(body);
        g.setColor(Theme.shade(c, 0.45));
        g.setStroke(Draw.stroke(Math.max(1.2, r * 0.12)));
        g.draw(body);

        // eyes look where the enemy walks
        double ex = dirX * r * 0.25, ey = dirY * r * 0.2;
        for (int k = -1; k <= 1; k += 2) {
            double px = cx + ex + (-dirY) * k * r * 0.32, py = y + ey + dirX * k * r * 0.32 - r * 0.1;
            Draw.circle(g, px, py, r * 0.22, Color.WHITE);
            Draw.circle(g, px + dirX * r * 0.08, py + dirY * r * 0.08, r * 0.11, new Color(0x15151C));
        }
        if (e.boss()) Icons.crown(g, cx, y - r * 1.15, r * 0.9, Theme.GOLD);

        // status overlays
        if (e.status.frozen()) {
            Draw.fillRound(g, cx - r * 1.15, y - r * 1.15, r * 2.3, r * 2.3, r * 0.6, new Color(190, 240, 255, 120));
            Draw.strokeRound(g, cx - r * 1.15, y - r * 1.15, r * 2.3, r * 2.3, r * 0.6, new Color(230, 250, 255, 200), 1.4);
        } else if (e.status.has(StatusType.SLOW)) {
            g.setColor(new Color(127, 212, 255, 150));
            g.setStroke(Draw.stroke(2));
            g.draw(new Arc2D.Double(cx - r * 1.25, y - r * 1.25, r * 2.5, r * 2.5, time * 120 % 360, 240, Arc2D.OPEN));
        }
        if (e.status.burnStacks() > 0) {
            for (int i = 0; i < Math.min(3, e.status.burnStacks()); i++) {
                double a = time * 7 + i * 2.1;
                double fx = cx + Math.cos(i * 2.3) * r * 0.5, fy = y - r * 0.7 - Math.abs(Math.sin(a)) * r * 0.5;
                Draw.circle(g, fx, fy, r * 0.16, new Color(255, 140, 50, 200));
                Draw.circle(g, fx, fy - r * 0.08, r * 0.08, new Color(255, 220, 90, 220));
            }
        }
        if (e.status.poisonStacks() > 0) {
            for (int i = 0; i < Math.min(3, 1 + e.status.poisonStacks() / 3); i++) {
                double t = (time * 0.9 + i * 0.37) % 1;
                Draw.circle(g, cx + (i - 1) * r * 0.5, y + r * 0.4 - t * r * 1.6, r * 0.13 * (1 - t * 0.5),
                        new Color(143, 227, 106, (int) (220 * (1 - t))));
            }
        }
        if (e.status.has(StatusType.VULNERABLE)) {
            g.setColor(new Color(192, 140, 255, 190));
            g.setStroke(Draw.stroke(1.5));
            double vr = r * 1.4;
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2 + time;
                g.draw(new Arc2D.Double(cx - vr, y - vr, vr * 2, vr * 2, Math.toDegrees(a), 40, Arc2D.OPEN));
            }
        }
        if (e.shield > 0 && e.maxShield > 0) {
            double a = 0.25 + 0.45 * (e.shield / e.maxShield);
            Draw.ring(g, cx, y, r * 1.3, new Color(90, 176, 255, (int) (a * 255)), 2.2);
        }
        if (e.invulnerable > 0) {
            g.setColor(new Color(242, 193, 78, 90));
            g.fill(new Ellipse2D.Double(cx - r * 1.5, y - r * 1.5, r * 3, r * 3));
            Draw.ring(g, cx, y, r * 1.5, new Color(242, 193, 78, 200), 2);
        }
        if (e.elite()) {
            int n = e.affixes.size();
            for (int i = 0; i < n; i++) {
                AffixDef a = e.affixes.get(i);
                double ang = time * 2 + i * Math.PI * 2 / n;
                Draw.circle(g, cx + Math.cos(ang) * r * 1.3, y + Math.sin(ang) * r * 1.3, r * 0.16, Theme.rgb(a.color()));
            }
        }
    }

    private static void wings(Graphics2D g, double cx, double cy, double r, Color c, double t) {
        double flap = Math.sin(t * 14) * 0.5 + 0.5;
        g.setColor(Theme.shade(c, 0.6));
        for (int k = -1; k <= 1; k += 2) {
            Path2D w = new Path2D.Double();
            w.moveTo(cx + k * r * 0.4, cy);
            w.quadTo(cx + k * r * 1.6, cy - r * (0.4 + flap * 0.9), cx + k * r * 1.75, cy + r * 0.3);
            w.quadTo(cx + k * r * 1.0, cy + r * 0.1, cx + k * r * 0.4, cy + r * 0.3);
            w.closePath();
            g.fill(w);
        }
    }

    /** Health (and shield) bar centered above an enemy. */
    public static void healthBar(Graphics2D g, Enemy e, double cx, double top, double width) {
        double h = e.boss() ? 6 : 4;
        double x = cx - width / 2;
        Draw.fillRound(g, x - 1, top - 1, width + 2, h + 2, h, new Color(0, 0, 0, 170));
        double frac = e.hpFraction();
        Color c = frac > 0.5 ? new Color(0x6AD182) : frac > 0.25 ? new Color(0xF2C14E) : new Color(0xE05561);
        if (frac > 0) Draw.fillRound(g, x, top, width * frac, h, h, c);
        if (e.maxShield > 0 && e.shield > 0) {
            Draw.fillRound(g, x, top - h + 1, width * (e.shield / e.maxShield), h - 1.5, h, new Color(0x5AB0FF));
        }
    }

    /** Small enemy portrait for previews and tooltips. */
    public static void enemyIcon(Graphics2D g, EnemyDef d, double cx, double cy, double r, boolean elite) {
        Color c = Theme.rgb(d.color());
        if (elite) Draw.ring(g, cx, cy, r * 1.35, Theme.ELITE, 1.6);
        Shape body = enemyShape(d.shape(), cx, cy, r, -Math.PI / 2);
        g.setPaint(new GradientPaint((float) cx, (float) (cy - r), Theme.shade(c, 1.25), (float) cx, (float) (cy + r), Theme.shade(c, 0.7)));
        g.fill(body);
        g.setColor(Theme.shade(c, 0.45));
        g.setStroke(Draw.stroke(1.2));
        g.draw(body);
        Draw.circle(g, cx - r * 0.3, cy - r * 0.1, r * 0.2, Color.WHITE);
        Draw.circle(g, cx + r * 0.3, cy - r * 0.1, r * 0.2, Color.WHITE);
        Draw.circle(g, cx - r * 0.3, cy - r * 0.05, r * 0.1, Color.BLACK);
        Draw.circle(g, cx + r * 0.3, cy - r * 0.05, r * 0.1, Color.BLACK);
        if (d.flying()) Draw.line(g, cx - r * 1.2, cy + r * 1.1, cx + r * 1.2, cy + r * 1.1, Theme.alpha(Theme.MANA, 0.6), 1.2);
    }
}
