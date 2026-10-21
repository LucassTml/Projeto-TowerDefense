package io.github.lucasstml.towerrogue.ui.screens;

import io.github.lucasstml.towerrogue.core.SeedCodec;
import io.github.lucasstml.towerrogue.run.RunPhase;
import io.github.lucasstml.towerrogue.run.RunState;
import io.github.lucasstml.towerrogue.ui.App;
import io.github.lucasstml.towerrogue.ui.Draw;
import io.github.lucasstml.towerrogue.ui.Icons;
import io.github.lucasstml.towerrogue.ui.Screen;
import io.github.lucasstml.towerrogue.ui.Theme;
import io.github.lucasstml.towerrogue.ui.Ui;

import java.awt.Color;
import java.awt.Graphics2D;

/** Victory/defeat summary with the essence earned. */
public final class RunEndScreen implements Screen {
    private final App app;
    private final int essence;
    private double time;

    public RunEndScreen(App app, int essence) {
        this.app = app;
        this.essence = essence;
    }

    @Override
    public void update(double dt) {
        time += dt;
    }

    @Override
    public void render(Graphics2D g, Ui ui, double frameDt) {
        RunState s = app.run().state();
        boolean won = s.phase == RunPhase.VICTORY;
        Color c = won ? Theme.GOLD : Theme.HP;
        Widgets.background(g, time, c);
        Draw.shadowText(g, won ? "VICTORY" : "DEFEAT", 640, 120, Theme.title(80), c);
        String sub = won ? "The Ashen Colossus has fallen. The realm is safe — for now."
                : "Your defenses crumbled in Act " + s.act + ".";
        Draw.textCentered(g, sub, 640, 182, Theme.font(18), Theme.TEXT_DIM);

        double x = 640 - 260, y = 220, w = 520, h = 300;
        Draw.panel(g, x, y, w, h);
        String[][] rows = {
                {"Floors cleared", String.valueOf(s.floorsCleared)},
                {"Enemies defeated", String.valueOf(s.kills)},
                {"Elites slain", String.valueOf(s.elitesKilled)},
                {"Bosses slain", String.valueOf(s.bossesKilled)},
                {"Total damage", String.valueOf(s.damage)},
                {"Enemies leaked", String.valueOf(s.leaks)},
                {"Blueprints / relics", s.blueprints.size() + " / " + s.relics.size()},
                {"Ascension · Seed", s.ascension + " · " + SeedCodec.encode(s.seed)},
        };
        for (int i = 0; i < rows.length; i++) {
            double ry = y + 38 + i * 32;
            Draw.text(g, rows[i][0], x + 36, ry, Theme.font(16), Theme.TEXT_DIM);
            Draw.textRight(g, rows[i][1], x + w - 36, ry, Theme.bold(16), Theme.TEXT);
        }

        Icons.essence(g, 560, 560, 34);
        Draw.textLeftMid(g, "+" + essence + " essence", 584, 560, Theme.bold(26), new Color(0xE9C8FF));
        if (won && s.ascension + 1 == app.profile.maxAscension) {
            Draw.textCentered(g, "Ascension " + app.profile.maxAscension + " unlocked!", 640, 600, Theme.bold(17), Theme.BOSS);
        }
        if (ui.button("Sanctum", 640 - 230, 640, 210, 48, true, Ui.Style.NORMAL)) app.toSanctum();
        if (ui.button("Main menu", 640 + 20, 640, 210, 48, true, Ui.Style.PRIMARY)) app.toMainMenu();
    }
}
