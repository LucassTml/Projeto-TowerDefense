package io.github.lucasstml.towerrogue.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Stateless drawing helpers on top of Java2D. All coordinates are virtual pixels (1280×720). */
public final class Draw {
    private static final BasicStroke[] STROKES = new BasicStroke[16];

    private Draw() {
    }

    public static Stroke stroke(double width) {
        int i = (int) Math.round(width * 2);
        if (i >= 0 && i < STROKES.length) {
            if (STROKES[i] == null) STROKES[i] = new BasicStroke(i / 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            return STROKES[i];
        }
        return new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    public static void fillRound(Graphics2D g, double x, double y, double w, double h, double r, Color c) {
        g.setColor(c);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, r, r));
    }

    public static void strokeRound(Graphics2D g, double x, double y, double w, double h, double r, Color c, double width) {
        g.setColor(c);
        g.setStroke(stroke(width));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, r, r));
    }

    /** Standard UI panel: soft shadow, vertical gradient and a thin border. */
    public static void panel(Graphics2D g, double x, double y, double w, double h) {
        panel(g, x, y, w, h, Theme.PANEL, Theme.BORDER);
    }

    public static void panel(Graphics2D g, double x, double y, double w, double h, Color fill, Color border) {
        fillRound(g, x + 2, y + 4, w, h, 14, new Color(0, 0, 0, 90));
        g.setPaint(new GradientPaint((float) x, (float) y, Theme.shade(fill, 1.08), (float) x, (float) (y + h), Theme.shade(fill, 0.85)));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 14, 14));
        strokeRound(g, x + 0.5, y + 0.5, w - 1, h - 1, 14, border, 1.2);
    }

    public static void circle(Graphics2D g, double cx, double cy, double r, Color c) {
        g.setColor(c);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    public static void ring(Graphics2D g, double cx, double cy, double r, Color c, double width) {
        g.setColor(c);
        g.setStroke(stroke(width));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    public static void line(Graphics2D g, double x1, double y1, double x2, double y2, Color c, double width) {
        g.setColor(c);
        g.setStroke(stroke(width));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    public static void text(Graphics2D g, String s, double x, double y, Font f, Color c) {
        g.setFont(f);
        g.setColor(c);
        g.drawString(s, (float) x, (float) y);
    }

    /** Draws text with its baseline adjusted so the glyphs are vertically centered on cy. */
    public static void textCentered(Graphics2D g, String s, double cx, double cy, Font f, Color c) {
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(c);
        g.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0), (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    public static void textLeftMid(Graphics2D g, String s, double x, double cy, Font f, Color c) {
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(c);
        g.drawString(s, (float) x, (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    public static void textRight(Graphics2D g, String s, double right, double y, Font f, Color c) {
        g.setFont(f);
        g.setColor(c);
        g.drawString(s, (float) (right - g.getFontMetrics().stringWidth(s)), (float) y);
    }

    public static void shadowText(Graphics2D g, String s, double cx, double cy, Font f, Color c) {
        textCentered(g, s, cx + 1.5, cy + 2, f, new Color(0, 0, 0, 160));
        textCentered(g, s, cx, cy, f, c);
    }

    public static double width(Graphics2D g, String s, Font f) {
        return g.getFontMetrics(f).stringWidth(s);
    }

    public static String ellipsize(Graphics2D g, String s, Font f, double maxW) {
        FontMetrics fm = g.getFontMetrics(f);
        if (fm.stringWidth(s) <= maxW) return s;
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "…") > maxW) t = t.substring(0, t.length() - 1);
        return t + "…";
    }

    public static List<String> wrap(Graphics2D g, String text, Font f, double maxW) {
        FontMetrics fm = g.getFontMetrics(f);
        List<String> lines = new ArrayList<>();
        for (String para : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (fm.stringWidth(candidate) > maxW && !line.isEmpty()) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }

    /** Wrapped paragraph; returns the height used. */
    public static double paragraph(Graphics2D g, String text, double x, double y, double w, Font f, Color c, double lineH) {
        List<String> lines = wrap(g, text, f, w);
        for (int i = 0; i < lines.size(); i++) text(g, lines.get(i), x, y + i * lineH, f, c);
        return lines.size() * lineH;
    }

    public static void bar(Graphics2D g, double x, double y, double w, double h, double frac, Color fg, Color bg) {
        fillRound(g, x, y, w, h, h, bg);
        if (frac > 0) fillRound(g, x, y, Math.max(h, w * Math.min(1, frac)), h, h, fg);
    }
}
