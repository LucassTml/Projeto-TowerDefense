package io.github.lucasstml.towerrogue.ui.screens;

import io.github.lucasstml.towerrogue.content.AttackKind;
import io.github.lucasstml.towerrogue.content.Content;
import io.github.lucasstml.towerrogue.content.OnHitDef;
import io.github.lucasstml.towerrogue.content.RelicDef;
import io.github.lucasstml.towerrogue.content.Stat;
import io.github.lucasstml.towerrogue.content.StatMod;
import io.github.lucasstml.towerrogue.content.StatSheet;
import io.github.lucasstml.towerrogue.content.TowerDef;
import io.github.lucasstml.towerrogue.core.SeedCodec;
import io.github.lucasstml.towerrogue.run.Blueprint;
import io.github.lucasstml.towerrogue.run.RunState;
import io.github.lucasstml.towerrogue.ui.App;
import io.github.lucasstml.towerrogue.ui.Art;
import io.github.lucasstml.towerrogue.ui.Draw;
import io.github.lucasstml.towerrogue.ui.Icons;
import io.github.lucasstml.towerrogue.ui.Theme;
import io.github.lucasstml.towerrogue.ui.Ui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Widgets shared by several screens: the run HUD, cards, badges and tooltips. */
final class Widgets {
    private Widgets() {
    }

    // ================================================================== backgrounds

    static void background(Graphics2D g, double time, Color accent) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x161B29), 0, 720, new Color(0x0B0D14)));
        g.fillRect(0, 0, 1280, 720);
        for (int i = 0; i < 40; i++) {
            double sx = (i * 137.5) % 1280;
            double sy = 720 - ((time * (8 + i % 7 * 3) + i * 53) % 760);
            double r = 1 + (i % 3);
            Draw.circle(g, sx + Math.sin(time * 0.6 + i) * 12, sy, r, Theme.alpha(accent, 0.12 + (i % 4) * 0.05));
        }
    }

    // ================================================================== run HUD

    /** Top bar used on every run screen: HP, gold, act/floor, ascension, seed and relics. */
    static void runHud(Graphics2D g, Ui ui, App app, String center) {
        RunState r = app.run().state();
        g.setPaint(new GradientPaint(0, 0, new Color(0x1E2436), 0, 44, new Color(0x151925)));
        g.fillRect(0, 0, 1280, 44);
        Draw.line(g, 0, 44, 1280, 44, Theme.BORDER, 1);

        Icons.heart(g, 24, 22, 22, Theme.HP);
        Draw.textLeftMid(g, r.hp + " / " + r.maxHp, 42, 22, Theme.bold(17), Theme.TEXT);
        if (ui.hover(10, 4, 110, 36)) ui.tooltip("Health", List.of("Enemies reaching your base remove HP.", "At 0 HP the run ends."));
        Icons.coin(g, 140, 22, 20);
        Draw.textLeftMid(g, String.valueOf(r.gold), 156, 22, Theme.bold(17), Theme.GOLD);
        if (ui.hover(128, 4, 80, 36)) ui.tooltip("Gold", List.of("Spent at merchants. Earned from battles and events."));

        String act = "Act " + r.act + " · " + app.content.act(r.act).name();
        Draw.textLeftMid(g, act, 226, 22, Theme.bold(15), Theme.TEXT_DIM);
        if (r.ascension > 0) {
            double ax = 226 + Draw.width(g, act, Theme.bold(15)) + 16;
            Draw.textLeftMid(g, "Ascension " + r.ascension, ax, 22, Theme.bold(14), Theme.BOSS);
        }
        if (center != null) Draw.shadowText(g, center, 640, 22, Theme.title(20), Theme.TEXT);

        double x = 1270;
        for (int i = r.relics.size() - 1; i >= 0; i--) {
            RelicDef d = app.content.relic(r.relics.get(i));
            x -= 34;
            if (x < 820) break;
            relicBadge(g, ui, d, x + 16, 22, 28);
        }
        Draw.textRight(g, "Seed " + SeedCodec.encode(r.seed), x - 10, 27, Theme.font(12), Theme.TEXT_FAINT);
    }

    // ================================================================== relics

    static void relicBadge(Graphics2D g, Ui ui, RelicDef d, double cx, double cy, double size) {
        Color c = Theme.rgb(d.color());
        double r = size / 2;
        Draw.circle(g, cx + 1, cy + 2, r, new Color(0, 0, 0, 100));
        g.setPaint(new GradientPaint((float) cx, (float) (cy - r), Theme.shade(c, 0.55), (float) cx, (float) (cy + r), Theme.shade(c, 0.25)));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, size, size));
        Draw.ring(g, cx, cy, r, Theme.rgb(d.rarity().color), 1.6);
        Draw.textCentered(g, d.glyph(), cx, cy + 1, Theme.title((int) (size * 0.55)), Theme.shade(c, 1.2));
        if (ui.hover(cx - r, cy - r, size, size)) relicTooltip(ui, d);
    }

    static void relicTooltip(Ui ui, RelicDef d) {
        ui.tooltip(d.name(), Theme.rgb(d.rarity().color), List.of(d.rarity().label + " relic", d.description()));
    }

    // ================================================================== towers

    static StatSheet statsFor(Content c, TowerDef def, int tier) {
        List<StatMod> mods = new ArrayList<>();
        for (int k = 0; k < Math.min(tier, c.balance().blueprintTiers.size()); k++) mods.addAll(c.balance().blueprintTiers.get(k));
        return StatSheet.compute(def.base(), mods);
    }

    static String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-6) return String.valueOf((long) Math.rint(v));
        return String.format(Locale.ROOT, "%.1f", v);
    }

    /** Short stat summary lines for a tower type at a blueprint tier. */
    static List<String> statLines(Content c, TowerDef def, int tier) {
        StatSheet s = statsFor(c, def, tier);
        List<String> out = new ArrayList<>();
        switch (def.kind()) {
            case SUPPORT -> {
                out.add("Aura: +" + Math.round(s.get(Stat.SUPPORT_DAMAGE) * 100) + "% damage, +"
                        + Math.round(s.get(Stat.SUPPORT_SPEED) * 100) + "% attack speed");
                out.add("Aura radius " + fmt(s.get(Stat.RANGE)) + " tiles");
            }
            case GENERATOR -> out.add("+" + fmt(s.get(Stat.INCOME)) + " mana every " + fmt(s.effectiveCooldown()) + "s during waves");
            default -> {
                out.add("Damage " + fmt(s.get(Stat.DAMAGE)) + " " + def.damageType().label.toLowerCase(Locale.ROOT)
                        + "  ·  " + fmt(1 / s.effectiveCooldown()) + " attacks/s");
                String extra = "Range " + fmt(s.get(Stat.RANGE));
                if (s.get(Stat.SPLASH) > 0) extra += "  ·  Splash " + fmt(s.get(Stat.SPLASH));
                if (s.get(Stat.CHAIN) > 0) extra += "  ·  Chains " + (int) s.get(Stat.CHAIN);
                if (def.kind() == AttackKind.PULSE) extra += "  ·  Hits all in range";
                out.add(extra);
                if (s.get(Stat.CRIT_CHANCE) > 0) out.add(Math.round(s.get(Stat.CRIT_CHANCE) * 100) + "% crit for x" + fmt(s.get(Stat.CRIT_MULT)));
                if (s.get(Stat.ARMOR_PEN) > 0) out.add("Ignores " + fmt(s.get(Stat.ARMOR_PEN)) + " armor");
                out.add(s.flag(Stat.HIT_AIR) ? "Can hit flying enemies" : "Cannot hit flying enemies");
            }
        }
        for (OnHitDef o : def.onHit()) out.add("On hit: " + o.describe());
        return out;
    }

    static void towerTooltip(Ui ui, Content c, TowerDef def, int tier) {
        List<String> lines = new ArrayList<>();
        lines.add(def.rarity().label + " · " + String.join(", ", def.tags()) + "  ·  " + def.cost() + " mana");
        lines.add(def.description());
        lines.add("");
        lines.addAll(statLines(c, def, tier));
        if (!def.branches().isEmpty()) {
            lines.add("");
            lines.add("Specializations at level " + def.maxLevel() + ":");
            for (TowerDef.Branch b : def.branches()) lines.add("  " + b.name() + ": " + b.description());
        }
        ui.tooltip(def.name() + Blueprint.tierName(tier), Theme.rgb(def.rarity().color), lines);
    }

    /**
     * Large tower card (rewards, shop). Returns true when hovered.
     * @param badge small label at the top ("NEW", "UPGRADE", a price...)
     */
    static boolean towerCard(Graphics2D g, Ui ui, App app, TowerDef def, int tier, String badge,
                             double x, double y, double w, double h, boolean dim) {
        boolean hover = !dim && ui.hover(x, y, w, h);
        double lift = hover ? -6 : 0;
        Color rc = Theme.rgb(def.rarity().color);
        Draw.panel(g, x, y + lift, w, h, hover ? Theme.PANEL_LIGHT : Theme.PANEL, dim ? Theme.BORDER : rc);
        Draw.fillRound(g, x + 10, y + lift + 10, w - 20, 120, 12, new Color(0x121622));
        Art.tower(g, def, x + w / 2, y + lift + 72, 92, -Math.PI / 4 + Math.sin(ui.time()) * 0.4, 1, false, ui.time(), false);
        if (badge != null) {
            double bw = Draw.width(g, badge, Theme.bold(12)) + 16;
            Draw.fillRound(g, x + w / 2 - bw / 2, y + lift + 4, bw, 20, 10, rc);
            Draw.textCentered(g, badge, x + w / 2, y + lift + 14, Theme.bold(12), new Color(0x10131C));
        }
        Draw.textCentered(g, def.name() + Blueprint.tierName(tier), x + w / 2, y + lift + 150, Theme.bold(17), Theme.TEXT);
        Draw.textCentered(g, def.rarity().label + " · " + def.cost() + " mana", x + w / 2, y + lift + 170, Theme.font(12), rc);
        double ty = y + lift + 194;
        ty += Draw.paragraph(g, def.description(), x + 14, ty, w - 28, Theme.font(13), Theme.TEXT_DIM, 17) + 6;
        for (String line : statLines(app.content, def, tier)) {
            if (ty > y + lift + h - 14) break;
            Color c = line.startsWith("Cannot") ? Theme.BAD : line.startsWith("Can hit") ? Theme.GOOD : Theme.TEXT;
            ty += Draw.paragraph(g, line, x + 14, ty, w - 28, Theme.font(12), c, 16);
        }
        if (dim) Draw.fillRound(g, x, y, w, h, 14, new Color(10, 12, 18, 150));
        if (hover) towerTooltip(ui, app.content, def, tier);
        return hover;
    }

    static boolean relicCard(Graphics2D g, Ui ui, RelicDef d, String badge, double x, double y, double w, double h, boolean dim) {
        boolean hover = !dim && ui.hover(x, y, w, h);
        double lift = hover ? -6 : 0;
        Color rc = Theme.rgb(d.rarity().color);
        Draw.panel(g, x, y + lift, w, h, hover ? Theme.PANEL_LIGHT : Theme.PANEL, dim ? Theme.BORDER : rc);
        relicBadge(g, ui, d, x + w / 2, y + lift + 62, 70);
        if (badge != null) {
            double bw = Draw.width(g, badge, Theme.bold(12)) + 16;
            Draw.fillRound(g, x + w / 2 - bw / 2, y + lift + 4, bw, 20, 10, rc);
            Draw.textCentered(g, badge, x + w / 2, y + lift + 14, Theme.bold(12), new Color(0x10131C));
        }
        Draw.textCentered(g, d.name(), x + w / 2, y + lift + 122, Theme.bold(17), Theme.TEXT);
        Draw.textCentered(g, d.rarity().label + " relic", x + w / 2, y + lift + 142, Theme.font(12), rc);
        Draw.paragraph(g, d.description(), x + 14, y + lift + 168, w - 28, Theme.font(13), Theme.TEXT_DIM, 17);
        if (dim) Draw.fillRound(g, x, y, w, h, 14, new Color(10, 12, 18, 150));
        return hover;
    }

    /** Compact blueprint list (map and campfire side panels). */
    static void blueprintList(Graphics2D g, Ui ui, App app, double x, double y, double w) {
        RunState r = app.run().state();
        Draw.text(g, "Blueprints", x, y, Theme.bold(16), Theme.TEXT);
        double yy = y + 14;
        for (Blueprint b : r.blueprints) {
            TowerDef def = app.content.tower(b.towerId);
            boolean hover = ui.hover(x, yy, w, 34);
            Draw.fillRound(g, x, yy, w, 34, 8, hover ? Theme.PANEL_LIGHT : new Color(0x181C29));
            Art.tower(g, def, x + 18, yy + 17, 30, -Math.PI / 4, 0, false, ui.time(), false);
            Draw.textLeftMid(g, def.name(), x + 40, yy + 17, Theme.bold(13), Theme.TEXT);
            for (int t = 0; t < Blueprint.MAX_TIER; t++) {
                Icons.star(g, x + w - 38 + t * 16, yy + 17, 6, t < b.tier ? Theme.GOLD : Theme.BORDER);
            }
            if (hover) towerTooltip(ui, app.content, def, b.tier);
            yy += 38;
        }
    }

    /** Icon grid of owned blueprints with tier stars; hover for details. */
    static void blueprintListCompact(Graphics2D g, Ui ui, App app, double x, double y, double w) {
        RunState r = app.run().state();
        Draw.text(g, "Your blueprints", x, y, Theme.bold(14), Theme.TEXT_DIM);
        int perRow = Math.max(1, (int) (w / 46));
        for (int i = 0; i < r.blueprints.size(); i++) {
            Blueprint b = r.blueprints.get(i);
            TowerDef def = app.content.tower(b.towerId);
            double cx = x + 20 + (i % perRow) * 46, cy = y + 30 + (i / perRow) * 50;
            Art.tower(g, def, cx, cy, 38, -Math.PI / 4, 0, false, ui.time(), false);
            for (int t = 0; t < b.tier; t++) Icons.star(g, cx - 6 + t * 12, cy + 22, 4.5, Theme.GOLD);
            if (ui.hover(cx - 20, cy - 20, 40, 44)) towerTooltip(ui, app.content, def, b.tier);
        }
    }

    /** Horizontal relic card (badge left, text right) for tight layouts. Returns true when hovered. */
    static boolean relicCardWide(Graphics2D g, Ui ui, RelicDef d, double x, double y, double w, double h, boolean dim) {
        boolean hover = !dim && ui.hover(x, y, w, h);
        double lift = hover ? -4 : 0;
        Color rc = Theme.rgb(d.rarity().color);
        Draw.panel(g, x, y + lift, w, h, hover ? Theme.PANEL_LIGHT : Theme.PANEL, dim ? Theme.BORDER : rc);
        relicBadge(g, ui, d, x + 50, y + lift + h / 2, 64);
        Draw.text(g, d.name(), x + 96, y + lift + 28, Theme.bold(16), Theme.TEXT);
        Draw.text(g, d.rarity().label + " relic", x + 96, y + lift + 46, Theme.font(12), rc);
        Draw.paragraph(g, d.description(), x + 96, y + lift + 66, w - 110, Theme.font(13), Theme.TEXT_DIM, 16);
        if (dim) Draw.fillRound(g, x, y, w, h, 14, new Color(10, 12, 18, 150));
        if (hover) relicTooltip(ui, d);
        return hover;
    }
}
