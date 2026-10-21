package io.github.lucasstml.towerrogue.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Cosmetic particle system. It owns its own random generator: visual randomness must never
 * consume numbers from the simulation streams, or turning effects off would change gameplay.
 * Coordinates are virtual pixels.
 */
public final class Particles {
    private static final int MAX = 1600;

    private enum Kind { DOT, SPARK, RING, TEXT, ARC, FLASH, SHARD }

    private static final class P {
        Kind kind;
        double x, y, vx, vy, life, max, size, size2, gravity, rot, vrot;
        Color color;
        String text;
        Font font;
        double[] pts;
    }

    private final List<P> list = new ArrayList<>();
    private final SplittableRandom rnd = new SplittableRandom(42);

    public int count() {
        return list.size();
    }

    public void clear() {
        list.clear();
    }

    private P add(Kind kind, double x, double y, double life, Color color) {
        if (list.size() >= MAX) list.remove(0);
        P p = new P();
        p.kind = kind;
        p.x = x;
        p.y = y;
        p.life = p.max = life;
        p.color = color;
        list.add(p);
        return p;
    }

    public void update(double dt) {
        for (int i = list.size() - 1; i >= 0; i--) {
            P p = list.get(i);
            p.life -= dt;
            if (p.life <= 0) {
                list.remove(i);
                continue;
            }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vy += p.gravity * dt;
            p.vx *= Math.pow(0.08, dt);
            if (p.kind != Kind.TEXT) p.vy *= Math.pow(0.2, dt);
            p.rot += p.vrot * dt;
        }
    }

    public void render(Graphics2D g) {
        Composite old = g.getComposite();
        for (P p : list) {
            double t = 1 - p.life / p.max; // 0 → 1
            float a = (float) Math.max(0, Math.min(1, p.kind == Kind.TEXT ? Math.min(1, p.life / p.max * 2.2) : 1 - t));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            switch (p.kind) {
                case DOT -> Draw.circle(g, p.x, p.y, p.size * (1 - t * 0.5), p.color);
                case SPARK -> Draw.line(g, p.x, p.y, p.x - p.vx * 0.03, p.y - p.vy * 0.03, p.color, p.size);
                case RING -> Draw.ring(g, p.x, p.y, p.size + (p.size2 - p.size) * easeOut(t), p.color, Math.max(1, 3 * (1 - t)));
                case FLASH -> Draw.circle(g, p.x, p.y, p.size * (0.6 + 0.4 * t), p.color);
                case TEXT -> {
                    Draw.textCentered(g, p.text, p.x + 1, p.y + 1.5, p.font, new Color(0, 0, 0, 180));
                    Draw.textCentered(g, p.text, p.x, p.y, p.font, p.color);
                }
                case ARC -> {
                    Path2D path = new Path2D.Double();
                    path.moveTo(p.pts[0], p.pts[1]);
                    for (int i = 2; i < p.pts.length; i += 2) path.lineTo(p.pts[i], p.pts[i + 1]);
                    g.setColor(p.color);
                    g.setStroke(Draw.stroke(3));
                    g.draw(path);
                    g.setColor(Color.WHITE);
                    g.setStroke(Draw.stroke(1.2));
                    g.draw(path);
                }
                case SHARD -> {
                    Path2D s = new Path2D.Double();
                    double r = p.size;
                    s.moveTo(p.x + Math.cos(p.rot) * r, p.y + Math.sin(p.rot) * r);
                    s.lineTo(p.x + Math.cos(p.rot + 2.4) * r * 0.5, p.y + Math.sin(p.rot + 2.4) * r * 0.5);
                    s.lineTo(p.x + Math.cos(p.rot + 3.9) * r * 0.5, p.y + Math.sin(p.rot + 3.9) * r * 0.5);
                    s.closePath();
                    g.setColor(p.color);
                    g.fill(s);
                }
            }
        }
        g.setComposite(old);
    }

    private static double easeOut(double t) {
        return 1 - (1 - t) * (1 - t);
    }

    // ================================================================== emitters

    public void burst(double x, double y, Color c, int count, double speed, double size, double life) {
        for (int i = 0; i < count; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double v = speed * (0.4 + rnd.nextDouble() * 0.8);
            P p = add(Kind.DOT, x, y, life * (0.6 + rnd.nextDouble() * 0.6), c);
            p.vx = Math.cos(a) * v;
            p.vy = Math.sin(a) * v;
            p.size = size * (0.6 + rnd.nextDouble() * 0.7);
            p.gravity = 60;
        }
    }

    public void sparks(double x, double y, Color c, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double v = speed * (0.5 + rnd.nextDouble());
            P p = add(Kind.SPARK, x, y, 0.18 + rnd.nextDouble() * 0.15, c);
            p.vx = Math.cos(a) * v;
            p.vy = Math.sin(a) * v;
            p.size = 1.6;
        }
    }

    public void ring(double x, double y, double from, double to, Color c, double life) {
        P p = add(Kind.RING, x, y, life, c);
        p.size = from;
        p.size2 = to;
    }

    public void flash(double x, double y, double r, Color c, double life) {
        P p = add(Kind.FLASH, x, y, life, c);
        p.size = r;
    }

    public void text(double x, double y, String text, Color c, Font font, double life) {
        P p = add(Kind.TEXT, x, y, life, c);
        p.text = text;
        p.font = font;
        p.vy = -38;
        p.vx = (rnd.nextDouble() - 0.5) * 20;
    }

    public void shards(double x, double y, Color c, int count, double size) {
        for (int i = 0; i < count; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double v = 60 + rnd.nextDouble() * 90;
            P p = add(Kind.SHARD, x, y, 0.45 + rnd.nextDouble() * 0.25, c);
            p.vx = Math.cos(a) * v;
            p.vy = Math.sin(a) * v;
            p.size = size * (0.6 + rnd.nextDouble() * 0.6);
            p.rot = rnd.nextDouble() * 6;
            p.vrot = (rnd.nextDouble() - 0.5) * 18;
            p.gravity = 140;
        }
    }

    /** Jagged lightning between two points. */
    public void arc(double x1, double y1, double x2, double y2, Color c, double life) {
        int segs = 6;
        double[] pts = new double[(segs + 1) * 2];
        double dx = x2 - x1, dy = y2 - y1, len = Math.max(1, Math.hypot(dx, dy));
        double nx = -dy / len, ny = dx / len;
        for (int i = 0; i <= segs; i++) {
            double t = i / (double) segs;
            double off = (i == 0 || i == segs) ? 0 : (rnd.nextDouble() - 0.5) * Math.min(18, len * 0.3);
            pts[i * 2] = x1 + dx * t + nx * off;
            pts[i * 2 + 1] = y1 + dy * t + ny * off;
        }
        P p = add(Kind.ARC, x1, y1, life, c);
        p.pts = pts;
    }

    public void bubble(double x, double y, double r, Color c) {
        P p = add(Kind.DOT, x, y, 0.5, c);
        p.size = r;
        p.vy = -30;
        p.vx = (rnd.nextDouble() - 0.5) * 20;
    }
}
