package io.github.lucasstml.towerrogue.ui;

import io.github.lucasstml.towerrogue.content.NodeType;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Procedural vector icons, so the game ships without any image assets. */
public final class Icons {
    private Icons() {
    }

    public static void heart(Graphics2D g, double cx, double cy, double s, Color c) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx, cy + s * 0.42);
        p.curveTo(cx - s * 0.62, cy, cx - s * 0.45, cy - s * 0.5, cx, cy - s * 0.18);
        p.curveTo(cx + s * 0.45, cy - s * 0.5, cx + s * 0.62, cy, cx, cy + s * 0.42);
        g.setPaint(new GradientPaint((float) cx, (float) (cy - s / 2), Theme.shade(c, 1.25), (float) cx, (float) (cy + s / 2), Theme.shade(c, 0.75)));
        g.fill(p);
        Draw.circle(g, cx - s * 0.18, cy - s * 0.16, s * 0.08, new Color(255, 255, 255, 120));
    }

    public static void coin(Graphics2D g, double cx, double cy, double s) {
        double r = s / 2;
        g.setPaint(new GradientPaint((float) cx, (float) (cy - r), new Color(0xFFE08A), (float) cx, (float) (cy + r), new Color(0xC8902A)));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        Draw.ring(g, cx, cy, r * 0.68, new Color(0xA9761F), Math.max(1, s * 0.08));
        Draw.ring(g, cx, cy, r, new Color(0x8A5E16), 1);
    }

    public static void mana(Graphics2D g, double cx, double cy, double s) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx, cy - s * 0.5);
        p.curveTo(cx + s * 0.42, cy - s * 0.02, cx + s * 0.36, cy + s * 0.45, cx, cy + s * 0.45);
        p.curveTo(cx - s * 0.36, cy + s * 0.45, cx - s * 0.42, cy - s * 0.02, cx, cy - s * 0.5);
        g.setPaint(new GradientPaint((float) cx, (float) (cy - s / 2), new Color(0xA8D8FF), (float) cx, (float) (cy + s / 2), new Color(0x2A6FD0)));
        g.fill(p);
        Draw.circle(g, cx - s * 0.12, cy + s * 0.05, s * 0.07, new Color(255, 255, 255, 150));
    }

    /** Essence (meta currency): a faceted violet gem. */
    public static void essence(Graphics2D g, double cx, double cy, double s) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx, cy - s * 0.5);
        p.lineTo(cx + s * 0.38, cy - s * 0.1);
        p.lineTo(cx, cy + s * 0.5);
        p.lineTo(cx - s * 0.38, cy - s * 0.1);
        p.closePath();
        g.setPaint(new GradientPaint((float) cx, (float) (cy - s / 2), new Color(0xE9C8FF), (float) cx, (float) (cy + s / 2), new Color(0x7A3FB0)));
        g.fill(p);
        Draw.line(g, cx - s * 0.38, cy - s * 0.1, cx + s * 0.38, cy - s * 0.1, new Color(255, 255, 255, 120), 1);
        Draw.line(g, cx, cy - s * 0.5, cx, cy + s * 0.5, new Color(255, 255, 255, 60), 1);
    }

    public static void star(Graphics2D g, double cx, double cy, double r, Color c) {
        g.setColor(c);
        g.fill(starShape(cx, cy, r, r * 0.45, 5, -Math.PI / 2));
    }

    public static Path2D starShape(double cx, double cy, double outer, double inner, int points, double rot) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < points * 2; i++) {
            double a = rot + i * Math.PI / points;
            double r = i % 2 == 0 ? outer : inner;
            if (i == 0) p.moveTo(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
            else p.lineTo(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
        }
        p.closePath();
        return p;
    }

    public static void lock(Graphics2D g, double cx, double cy, double s, Color c) {
        g.setColor(c);
        g.setStroke(Draw.stroke(s * 0.12));
        g.draw(new Arc2D.Double(cx - s * 0.22, cy - s * 0.45, s * 0.44, s * 0.5, 0, 180, Arc2D.OPEN));
        g.fill(new RoundRectangle2D.Double(cx - s * 0.32, cy - s * 0.18, s * 0.64, s * 0.5, s * 0.12, s * 0.12));
    }

    /** Map node icon. */
    public static void node(Graphics2D g, NodeType type, double cx, double cy, double s, Color c) {
        switch (type) {
            case COMBAT -> sword(g, cx, cy, s, c);
            case ELITE -> skull(g, cx, cy, s, c);
            case EVENT -> Draw.textCentered(g, "?", cx, cy + 1, Theme.title((int) (s * 0.9)), c);
            case SHOP -> bag(g, cx, cy, s, c);
            case REST -> campfire(g, cx, cy, s, c, 0);
            case TREASURE -> chest(g, cx, cy, s, c);
            case BOSS -> crown(g, cx, cy, s, c);
        }
    }

    public static void sword(Graphics2D g, double cx, double cy, double s, Color c) {
        g.setColor(c);
        for (int k = -1; k <= 1; k += 2) {
            double a = k * Math.PI / 4;
            double dx = Math.sin(a), dy = -Math.cos(a);
            double bx = cx - dx * s * 0.32, by = cy - dy * s * 0.32;
            double tx = cx + dx * s * 0.4, ty = cy + dy * s * 0.4;
            Draw.line(g, bx, by, tx, ty, c, s * 0.13);
            double gx = cx - dx * s * 0.18, gy = cy - dy * s * 0.18;
            Draw.line(g, gx - dy * s * 0.14, gy + dx * s * 0.14, gx + dy * s * 0.14, gy - dx * s * 0.14, c, s * 0.1);
        }
    }

    public static void skull(Graphics2D g, double cx, double cy, double s, Color c) {
        g.setColor(c);
        g.fill(new Ellipse2D.Double(cx - s * 0.36, cy - s * 0.42, s * 0.72, s * 0.62));
        g.fill(new RoundRectangle2D.Double(cx - s * 0.22, cy + s * 0.05, s * 0.44, s * 0.3, s * 0.1, s * 0.1));
        Color hole = new Color(0x1C2130);
        Draw.circle(g, cx - s * 0.14, cy - s * 0.1, s * 0.1, hole);
        Draw.circle(g, cx + s * 0.14, cy - s * 0.1, s * 0.1, hole);
        Draw.line(g, cx - s * 0.08, cy + s * 0.22, cx - s * 0.08, cy + s * 0.34, hole, s * 0.05);
        Draw.line(g, cx + s * 0.08, cy + s * 0.22, cx + s * 0.08, cy + s * 0.34, hole, s * 0.05);
    }

    public static void bag(Graphics2D g, double cx, double cy, double s, Color c) {
        g.setColor(c);
        g.fill(new Ellipse2D.Double(cx - s * 0.36, cy - s * 0.16, s * 0.72, s * 0.58));
        g.fill(new RoundRectangle2D.Double(cx - s * 0.16, cy - s * 0.36, s * 0.32, s * 0.24, s * 0.1, s * 0.1));
        Draw.textCentered(g, "$", cx, cy + s * 0.14, Theme.bold((int) (s * 0.42)), new Color(0x1C2130));
    }

    public static void chest(Graphics2D g, double cx, double cy, double s, Color c) {
        g.setColor(c);
        g.fill(new RoundRectangle2D.Double(cx - s * 0.4, cy - s * 0.12, s * 0.8, s * 0.42, s * 0.08, s * 0.08));
        g.fill(new Arc2D.Double(cx - s * 0.4, cy - s * 0.4, s * 0.8, s * 0.56, 0, 180, Arc2D.CHORD));
        Color dark = new Color(0x1C2130);
        Draw.line(g, cx - s * 0.4, cy - s * 0.1, cx + s * 0.4, cy - s * 0.1, dark, s * 0.06);
        Draw.circle(g, cx, cy + s * 0.02, s * 0.07, dark);
    }

    public static void crown(Graphics2D g, double cx, double cy, double s, Color c) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx - s * 0.42, cy + s * 0.28);
        p.lineTo(cx - s * 0.46, cy - s * 0.26);
        p.lineTo(cx - s * 0.2, cy);
        p.lineTo(cx, cy - s * 0.38);
        p.lineTo(cx + s * 0.2, cy);
        p.lineTo(cx + s * 0.46, cy - s * 0.26);
        p.lineTo(cx + s * 0.42, cy + s * 0.28);
        p.closePath();
        g.setColor(c);
        g.fill(p);
    }

    public static void campfire(Graphics2D g, double cx, double cy, double s, Color c, double time) {
        Color wood = new Color(0x8A5A34);
        Draw.line(g, cx - s * 0.34, cy + s * 0.34, cx + s * 0.34, cy + s * 0.18, wood, s * 0.12);
        Draw.line(g, cx + s * 0.34, cy + s * 0.34, cx - s * 0.34, cy + s * 0.18, wood, s * 0.12);
        double f = 1 + 0.08 * Math.sin(time * 9);
        Path2D flame = new Path2D.Double();
        flame.moveTo(cx, cy - s * 0.45 * f);
        flame.curveTo(cx + s * 0.32, cy - s * 0.05, cx + s * 0.22, cy + s * 0.25, cx, cy + s * 0.25);
        flame.curveTo(cx - s * 0.22, cy + s * 0.25, cx - s * 0.32, cy - s * 0.05, cx, cy - s * 0.45 * f);
        g.setColor(c);
        g.fill(flame);
    }
}
