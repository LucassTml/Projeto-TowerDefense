package io.github.lucasstml.towerrogue.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Tiny immediate-mode GUI (Dear ImGui style): widgets are function calls that draw
 * themselves and return whether they were clicked this frame. There is no widget tree to
 * keep in sync with game state, which suits a game where the UI is rebuilt every frame.
 * Tooltips are deferred and drawn last so they always sit on top.
 */
public final class Ui {
    public enum Style { NORMAL, PRIMARY, DANGER, GHOST }

    public final Input in;
    private Graphics2D g;
    private double time;
    private String tipTitle;
    private Color tipColor;
    private List<String> tipLines;
    private boolean blocked;

    public Ui(Input in) {
        this.in = in;
    }

    public void begin(Graphics2D g, double time) {
        this.g = g;
        this.time = time;
        this.tipTitle = null;
        this.tipLines = null;
        this.blocked = false;
    }

    public Graphics2D g() {
        return g;
    }

    public double time() {
        return time;
    }

    /** While blocked (a modal is open), widgets drawn underneath ignore the mouse. */
    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean hover(double x, double y, double w, double h) {
        return !blocked && in.mx >= x && in.mx < x + w && in.my >= y && in.my < y + h;
    }

    public boolean clicked(double x, double y, double w, double h) {
        if (hover(x, y, w, h) && in.clicked()) {
            in.consumeClick();
            return true;
        }
        return false;
    }

    public boolean button(String label, double x, double y, double w, double h, boolean enabled) {
        return button(label, x, y, w, h, enabled, Style.NORMAL, Theme.bold(15));
    }

    public boolean button(String label, double x, double y, double w, double h, boolean enabled, Style style) {
        return button(label, x, y, w, h, enabled, style, Theme.bold(15));
    }

    public boolean button(String label, double x, double y, double w, double h, boolean enabled, Style style, Font font) {
        boolean hot = enabled && hover(x, y, w, h);
        Color base = switch (style) {
            case PRIMARY -> new Color(0xB8862B);
            case DANGER -> new Color(0x9C3540);
            case GHOST -> new Color(0x1C2130);
            case NORMAL -> new Color(0x2D3550);
        };
        if (!enabled) base = new Color(0x23283A);
        if (hot) base = Theme.shade(base, 1.18);
        boolean pressed = hot && in.leftDown;
        double oy = pressed ? 1.5 : 0;
        Draw.fillRound(g, x + 1, y + 3, w, h, 10, new Color(0, 0, 0, 100));
        g.setPaint(new GradientPaint((float) x, (float) y, Theme.shade(base, 1.15), (float) x, (float) (y + h), Theme.shade(base, 0.8)));
        g.fill(new RoundRectangle2D.Double(x, y + oy, w, h, 10, 10));
        Color border = !enabled ? Theme.BORDER : (style == Style.PRIMARY ? Theme.GOLD : Theme.BORDER_LIGHT);
        if (hot) border = Theme.shade(border, 1.3);
        Draw.strokeRound(g, x + 0.5, y + oy + 0.5, w - 1, h - 1, 10, border, hot ? 1.6 : 1.1);
        Draw.textCentered(g, label, x + w / 2, y + oy + h / 2, font, enabled ? Theme.TEXT : Theme.TEXT_FAINT);
        if (enabled && hover(x, y, w, h) && in.clicked()) {
            in.consumeClick();
            return true;
        }
        return false;
    }

    /** Single-line text field bound to a StringBuilder. */
    public void textField(StringBuilder value, double x, double y, double w, double h, String placeholder, int maxLen) {
        for (char c : in.typed().toCharArray()) {
            if (c == '\b') {
                if (!value.isEmpty()) value.setLength(value.length() - 1);
            } else if (c >= 32 && c < 127 && value.length() < maxLen) {
                value.append(c);
            }
        }
        Draw.fillRound(g, x, y, w, h, 8, new Color(0x0D1018));
        Draw.strokeRound(g, x + 0.5, y + 0.5, w - 1, h - 1, 8, Theme.BORDER_LIGHT, 1.2);
        boolean empty = value.isEmpty();
        String shown = empty ? placeholder : value.toString();
        Draw.textLeftMid(g, shown, x + 12, y + h / 2, Theme.font(16), empty ? Theme.TEXT_FAINT : Theme.TEXT);
        if (!empty || ((int) (time * 2) % 2 == 0)) {
            double tx = x + 12 + (empty ? 0 : Draw.width(g, shown, Theme.font(16))) + 2;
            if ((int) (time * 2) % 2 == 0) Draw.line(g, tx, y + 9, tx, y + h - 9, Theme.TEXT_DIM, 1.5);
        }
    }

    public void tooltip(String title, Color titleColor, List<String> lines) {
        tipTitle = title;
        tipColor = titleColor;
        tipLines = lines;
    }

    public void tooltip(String title, List<String> lines) {
        tooltip(title, Theme.GOLD, lines);
    }

    /** Draws the pending tooltip next to the cursor, clamped to the screen. */
    public void end() {
        if (tipTitle == null) return;
        Font tf = Theme.bold(15), lf = Theme.font(13);
        double w = Draw.width(g, tipTitle, tf) + 24;
        List<String> wrapped = new ArrayList<>();
        if (tipLines != null) {
            for (String l : tipLines) wrapped.addAll(l.isEmpty() ? List.of("") : Draw.wrap(g, l, lf, 300));
        }
        for (String l : wrapped) w = Math.max(w, Draw.width(g, l, lf) + 24);
        double h = 34 + wrapped.size() * 18;
        double x = in.mx + 18, y = in.my + 14;
        if (x + w > 1272) x = in.mx - w - 12;
        if (y + h > 712) y = 712 - h;
        if (x < 8) x = 8;
        Draw.panel(g, x, y, w, h, new Color(0x141826), Theme.BORDER_LIGHT);
        Draw.text(g, tipTitle, x + 12, y + 22, tf, tipColor);
        for (int i = 0; i < wrapped.size(); i++) {
            String line = wrapped.get(i);
            Color c = Theme.TEXT_DIM;
            if (line.startsWith("+ ") || line.startsWith("Can hit")) c = Theme.GOOD;
            if (line.startsWith("Cannot")) c = Theme.BAD;
            Draw.text(g, line, x + 12, y + 42 + i * 18, lf, c);
        }
    }
}
