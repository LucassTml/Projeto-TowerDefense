package io.github.lucasstml.towerrogue.ui.screens;

import io.github.lucasstml.towerrogue.content.EventDef;
import io.github.lucasstml.towerrogue.run.RunController;
import io.github.lucasstml.towerrogue.run.RunState;
import io.github.lucasstml.towerrogue.ui.App;
import io.github.lucasstml.towerrogue.ui.Draw;
import io.github.lucasstml.towerrogue.ui.Icons;
import io.github.lucasstml.towerrogue.ui.Screen;
import io.github.lucasstml.towerrogue.ui.Theme;
import io.github.lucasstml.towerrogue.ui.Ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;

/** Narrative "?" node (also the run-start blessing). */
public final class EventScreen implements Screen {
    private final App app;
    private final RunController run;
    private double time;

    public EventScreen(App app) {
        this.app = app;
        this.run = app.run();
    }

    @Override
    public void update(double dt) {
        time += dt;
    }

    @Override
    public void render(Graphics2D g, Ui ui, double frameDt) {
        RunState s = run.state();
        EventDef e = run.currentEvent();
        if (e == null) { // the run already moved on; we're fading out
            Widgets.background(g, time, Theme.TEXT_DIM);
            return;
        }
        Color c = colorFor(e.id());
        Widgets.background(g, time, c);
        Widgets.runHud(g, ui, app, null);

        // illustration
        double ix = 50, iy = 80, iw = 440, ih = 580;
        Draw.panel(g, ix, iy, iw, ih, new Color(0x161A26), Theme.alpha(c, 0.8));
        double cx = ix + iw / 2, cy = iy + ih / 2 - 20;
        g.setPaint(new RadialGradientPaint((float) cx, (float) cy, 230f, new float[]{0f, 1f},
                new Color[]{Theme.alpha(c, 0.45), Theme.alpha(c, 0)}));
        g.fill(new Ellipse2D.Double(cx - 230, cy - 230, 460, 460));
        for (int i = 0; i < 3; i++) {
            double r = 90 + i * 40 + Math.sin(time * 1.5 + i) * 6;
            Draw.ring(g, cx, cy, r, Theme.alpha(c, 0.35 - i * 0.09), 2);
        }
        emblem(g, e.id(), cx, cy, 120, c);
        Draw.textCentered(g, e.special() ? "A gift for the journey" : "Unknown encounter", cx, iy + ih - 34, Theme.font(14), Theme.TEXT_DIM);

        // text & choices
        double tx = 540, tw = 690;
        Draw.shadowText(g, e.title(), tx + tw / 2, 110, Theme.title(34), Theme.TEXT);
        double ty = 160 + Draw.paragraph(g, e.text(), tx, 160, tw, Theme.font(17), Theme.TEXT_DIM, 24) + 20;

        if (!s.eventResolved) {
            for (int i = 0; i < e.choices().size(); i++) {
                EventDef.Choice ch = e.choices().get(i);
                boolean ok = run.choiceAvailable(ch);
                double by = ty + i * 74;
                if (ui.button("", tx, by, tw, 62, ok, Ui.Style.NORMAL)) {
                    run.chooseEvent(i);
                }
                Draw.text(g, ch.label(), tx + 20, by + 26, Theme.bold(17), ok ? Theme.GOLD : Theme.TEXT_FAINT);
                String desc = ch.description().isEmpty() ? "" : ch.description();
                if (!ok) desc = requirement(ch) + "   " + desc;
                Draw.text(g, desc, tx + 20, by + 48, Theme.font(14), ok ? Theme.TEXT_DIM : Theme.BAD);
            }
        } else {
            Draw.text(g, "Outcome", tx, ty + 4, Theme.bold(18), Theme.GOLD);
            double ly = ty + 34;
            for (String line : s.eventLog) {
                ly += Draw.paragraph(g, "-  " + line, tx, ly, tw, Theme.font(16), Theme.TEXT, 22) + 4;
            }
            String label = s.eventBattlePending ? "To battle!" : "Continue";
            if (ui.button(label, tx + tw / 2 - 130, Math.max(ly + 20, 560), 260, 50, true,
                    s.eventBattlePending ? Ui.Style.DANGER : Ui.Style.PRIMARY, Theme.bold(17))) {
                run.continueEvent();
                app.showRunPhase();
            }
        }
    }

    private static String requirement(EventDef.Choice ch) {
        StringBuilder sb = new StringBuilder("Requires");
        if (ch.requires().has("gold")) sb.append(" ").append(ch.requires().getInt("gold")).append(" gold");
        if (ch.requires().has("hp")) sb.append(" ").append(ch.requires().getInt("hp")).append(" HP");
        return sb.toString();
    }

    private static Color colorFor(String id) {
        return switch (id) {
            case "blessing", "sage" -> new Color(0xF2C14E);
            case "shrine" -> new Color(0xC08CFF);
            case "cart", "forge" -> new Color(0xFF8A3D);
            case "gambler", "idol" -> new Color(0x6AD182);
            case "fountain", "library" -> new Color(0x5AB0FF);
            case "ambush" -> new Color(0xE05561);
            case "refugees" -> new Color(0xF28BB0);
            default -> new Color(0x8FC8FF);
        };
    }

    private void emblem(Graphics2D g, String id, double cx, double cy, double s, Color c) {
        switch (id) {
            case "blessing", "sage" -> Icons.star(g, cx, cy, s * 0.5, c);
            case "shrine" -> Icons.skull(g, cx, cy, s, c);
            case "cart" -> Icons.chest(g, cx, cy, s, c);
            case "forge" -> Icons.campfire(g, cx, cy, s, c, time);
            case "fountain" -> Icons.mana(g, cx, cy, s);
            case "ambush" -> Icons.sword(g, cx, cy, s, c);
            case "idol" -> Icons.crown(g, cx, cy, s, c);
            case "refugees" -> Icons.heart(g, cx, cy, s, c);
            case "library" -> Icons.bag(g, cx, cy, s, c);
            default -> Draw.textCentered(g, "?", cx, cy, Theme.title((int) s), c);
        }
    }
}
