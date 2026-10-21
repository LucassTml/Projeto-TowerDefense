package io.github.lucasstml.towerrogue.ui.screens;

import io.github.lucasstml.towerrogue.content.Balance;
import io.github.lucasstml.towerrogue.content.TowerDef;
import io.github.lucasstml.towerrogue.core.SeedCodec;
import io.github.lucasstml.towerrogue.meta.Profile;
import io.github.lucasstml.towerrogue.run.RunState;
import io.github.lucasstml.towerrogue.ui.App;
import io.github.lucasstml.towerrogue.ui.Art;
import io.github.lucasstml.towerrogue.ui.Draw;
import io.github.lucasstml.towerrogue.ui.Icons;
import io.github.lucasstml.towerrogue.ui.Screen;
import io.github.lucasstml.towerrogue.ui.Theme;
import io.github.lucasstml.towerrogue.ui.Ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

/** Title screen: continue, new run (seed + Ascension), Sanctum, help. */
public final class MainMenuScreen implements Screen {
    private final App app;
    private RunState saved;
    private boolean newRunOpen;
    private int ascension;
    private final StringBuilder seed = new StringBuilder();
    private double time;

    public MainMenuScreen(App app) {
        this.app = app;
    }

    @Override
    public void enter() {
        saved = app.saves.hasRun() ? app.saves.loadRun() : null;
        ascension = app.profile.maxAscension;
    }

    @Override
    public void update(double dt) {
        time += dt;
    }

    @Override
    public void render(Graphics2D g, Ui ui, double frameDt) {
        app.textInputActive = newRunOpen;
        Widgets.background(g, time, Theme.GOLD);
        drawDecor(g);

        g.setPaint(new RadialGradientPaint(640, 150, 420, new float[]{0f, 1f},
                new Color[]{new Color(242, 193, 78, 40), new Color(242, 193, 78, 0)}));
        g.fill(new Ellipse2D.Double(220, -270, 840, 840));
        Draw.shadowText(g, "TOWER ROGUE", 640, 140, Theme.title(78), Theme.GOLD);
        Draw.textCentered(g, "a roguelike tower defense", 640, 196, Theme.font(20), Theme.TEXT_DIM);

        ui.setBlocked(newRunOpen);
        double bx = 640 - 150, by = 260, bw = 300, bh = 50, gap = 14;
        if (saved != null) {
            String label = "Continue  ·  Act " + saved.act + "  ·  " + saved.hp + "/" + saved.maxHp + " HP";
            if (ui.button(label, bx, by, bw, bh, true, Ui.Style.PRIMARY, Theme.bold(17))) {
                if (!app.continueRun()) saved = null;
            }
            by += bh + gap;
        }
        if (ui.button("New Run", bx, by, bw, bh, true, saved == null ? Ui.Style.PRIMARY : Ui.Style.NORMAL, Theme.bold(17))) {
            newRunOpen = true;
            seed.setLength(0);
        }
        by += bh + gap;
        if (ui.button("Sanctum", bx, by, bw, bh, true, Ui.Style.NORMAL, Theme.bold(17))) app.toSanctum();
        by += bh + gap;
        if (ui.button("How to play  [H]", bx, by, bw, bh, true, Ui.Style.NORMAL, Theme.bold(17))) app.toggleHelp();
        by += bh + gap;
        if (ui.button("Quit", bx, by, bw, bh, true, Ui.Style.GHOST, Theme.bold(17))) app.quit();
        ui.setBlocked(false);

        Profile p = app.profile;
        Icons.essence(g, 470, 668, 20);
        String stats = p.essence + " essence   ·   " + p.runs + " runs   ·   " + p.wins + " wins   ·   best: "
                + p.bestFloor + " floors   ·   Ascension " + p.maxAscension + " unlocked";
        Draw.textLeftMid(g, stats, 486, 668, Theme.font(14), Theme.TEXT_DIM);
        Draw.textRight(g, "v2.0  ·  Java " + Runtime.version().feature(), 1268, 708, Theme.font(11), Theme.TEXT_FAINT);

        if (newRunOpen) drawNewRun(g, ui);
    }

    private void drawDecor(Graphics2D g) {
        List<TowerDef> towers = new ArrayList<>(app.content.towers());
        double spacing = 1280.0 / towers.size();
        for (int i = 0; i < towers.size(); i++) {
            double x = spacing * (i + 0.5);
            double y = 600 + Math.sin(time * 0.8 + i) * 4;
            Art.tower(g, towers.get(i), x, y, 56, -Math.PI / 2 + Math.sin(time * 0.7 + i * 1.3) * 0.8, 0, false, time, false);
        }
        g.setColor(new Color(16, 19, 28, 120));
        g.fillRect(0, 560, 1280, 90);
    }

    private void drawNewRun(Graphics2D g, Ui ui) {
        g.setColor(new Color(5, 6, 10, 180));
        g.fillRect(0, 0, 1280, 720);
        double w = 620, h = 470, x = 640 - w / 2, y = 120;
        Draw.panel(g, x, y, w, h);
        Draw.shadowText(g, "New Run", 640, y + 40, Theme.title(30), Theme.TEXT);

        Balance b = app.content.balance();
        Draw.text(g, "Ascension", x + 36, y + 92, Theme.bold(16), Theme.TEXT);
        if (ui.button("<", x + 200, y + 70, 36, 32, ascension > 0)) ascension--;
        Draw.textCentered(g, String.valueOf(ascension), x + 266, y + 86, Theme.bold(22), ascension > 0 ? Theme.BOSS : Theme.TEXT);
        if (ui.button(">", x + 296, y + 70, 36, 32, ascension < app.profile.maxAscension)) ascension++;
        Draw.text(g, "Max unlocked: " + app.profile.maxAscension + " (win to unlock the next)", x + 350, y + 92, Theme.font(13), Theme.TEXT_FAINT);

        double ly = y + 126;
        if (ascension == 0) {
            Draw.text(g, "Standard difficulty.", x + 36, ly, Theme.font(14), Theme.TEXT_DIM);
        } else {
            for (Balance.AscensionLevel a : b.ascension) {
                if (a.level() > ascension) break;
                Draw.text(g, a.level() + ".  " + a.text(), x + 36, ly, Theme.font(14), Theme.TEXT_DIM);
                ly += 19;
            }
        }

        Draw.text(g, "Seed (optional)", x + 36, y + 352, Theme.bold(16), Theme.TEXT);
        ui.textField(seed, x + 190, y + 330, w - 226, 36, "random  ·  type any text or a seed code", 24);
        if (!seed.isEmpty()) {
            Draw.text(g, "Seed code: " + SeedCodec.encode(SeedCodec.decode(seed.toString())), x + 190, y + 386, Theme.font(12), Theme.TEXT_FAINT);
        }
        if (saved != null) {
            Draw.textCentered(g, "Starting a new run discards your saved run.", 640, y + h - 76, Theme.font(13), Theme.HP);
        }
        if (ui.button("Cancel", x + 36, y + h - 60, 160, 44, true) || ui.in.keyPressed(KeyEvent.VK_ESCAPE)) newRunOpen = false;
        if (ui.button("Begin", x + w - 196, y + h - 60, 160, 44, true, Ui.Style.PRIMARY) || ui.in.keyPressed(KeyEvent.VK_ENTER)) {
            newRunOpen = false;
            app.textInputActive = false;
            long s = seed.isEmpty() ? SeedCodec.randomSeed() : SeedCodec.decode(seed.toString());
            app.newRun(s, ascension);
        }
    }
}
