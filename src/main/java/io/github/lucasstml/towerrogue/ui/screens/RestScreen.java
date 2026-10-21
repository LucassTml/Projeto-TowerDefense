package io.github.lucasstml.towerrogue.ui.screens;

import io.github.lucasstml.towerrogue.run.RunController;
import io.github.lucasstml.towerrogue.run.RunState;
import io.github.lucasstml.towerrogue.ui.App;
import io.github.lucasstml.towerrogue.ui.Draw;
import io.github.lucasstml.towerrogue.ui.Icons;
import io.github.lucasstml.towerrogue.ui.Particles;
import io.github.lucasstml.towerrogue.ui.Screen;
import io.github.lucasstml.towerrogue.ui.Theme;
import io.github.lucasstml.towerrogue.ui.Ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;

/** Campfire: heal or permanently upgrade a blueprint. */
public final class RestScreen implements Screen {
    private final App app;
    private final RunController run;
    private final BlueprintPicker picker = new BlueprintPicker();
    private final Particles embers = new Particles();
    private double time;
    private double emberTimer;

    public RestScreen(App app) {
        this.app = app;
        this.run = app.run();
    }

    @Override
    public void update(double dt) {
        time += dt;
        emberTimer -= dt;
        if (emberTimer <= 0) {
            emberTimer = 0.08;
            embers.bubble(640 + (Math.random() - 0.5) * 60, 290, 2 + Math.random() * 2, new Color(255, 160, 60));
        }
        embers.update(dt);
    }

    @Override
    public void render(Graphics2D g, Ui ui, double frameDt) {
        RunState s = run.state();
        Widgets.background(g, time, new Color(0xFF8A3D));
        ui.setBlocked(picker.isOpen());
        Widgets.runHud(g, ui, app, "Campfire");

        g.setPaint(new RadialGradientPaint(640f, 280f, 260f, new float[]{0f, 1f},
                new Color[]{new Color(255, 140, 60, 90), new Color(255, 140, 60, 0)}));
        g.fill(new Ellipse2D.Double(380, 20, 520, 520));
        embers.render(g);
        Icons.campfire(g, 640, 270, 150, new Color(0xFF8A3D), time);
        Icons.campfire(g, 640, 282, 90, new Color(0xFFD34D), time * 1.3);
        Draw.textCentered(g, "The fire crackles. You have time for one thing.", 640, 390, Theme.font(17), Theme.TEXT_DIM);

        int heal = Math.min(run.restHealAmount(), s.maxHp - s.hp);
        double w = 300, h = 170, y = 430;
        double x1 = 640 - w - 20, x2 = 640 + 20;
        if (option(g, ui, x1, y, w, h, "Rest", "Heal " + heal + " HP (" + s.hp + " → " + Math.min(s.maxHp, s.hp + heal) + ")", Theme.HP, true)) {
            run.rest();
            app.showRunPhase();
        }
        boolean canForge = !s.upgradeableBlueprints().isEmpty();
        if (option(g, ui, x2, y, w, h, "Forge", canForge ? "Permanently upgrade one blueprint" : "All blueprints are maxed", Theme.GOLD, canForge)) {
            picker.open();
        }
        ui.setBlocked(false);

        String picked = picker.render(g, ui, app, "Forge: choose a blueprint");
        if (picked != null && run.restForge(picked)) app.showRunPhase();
    }

    private boolean option(Graphics2D g, Ui ui, double x, double y, double w, double h, String title, String desc,
                           Color c, boolean enabled) {
        boolean hover = enabled && ui.hover(x, y, w, h);
        Draw.panel(g, x, y + (hover ? -6 : 0), w, h, hover ? Theme.PANEL_LIGHT : Theme.PANEL, enabled ? c : Theme.BORDER);
        double oy = hover ? -6 : 0;
        if (title.equals("Rest")) Icons.heart(g, x + w / 2, y + oy + 52, 54, c);
        else Icons.sword(g, x + w / 2, y + oy + 52, 54, c);
        Draw.textCentered(g, title, x + w / 2, y + oy + 104, Theme.title(24), enabled ? Theme.TEXT : Theme.TEXT_FAINT);
        Draw.textCentered(g, desc, x + w / 2, y + oy + 136, Theme.font(14), enabled ? Theme.TEXT_DIM : Theme.TEXT_FAINT);
        return enabled && ui.clicked(x, y, w, h);
    }
}
