package io.github.lucasstml.towerrogue.ui.screens;

import io.github.lucasstml.towerrogue.content.PerkDef;
import io.github.lucasstml.towerrogue.content.RelicDef;
import io.github.lucasstml.towerrogue.content.TowerDef;
import io.github.lucasstml.towerrogue.meta.MetaProgression;
import io.github.lucasstml.towerrogue.meta.Profile;
import io.github.lucasstml.towerrogue.ui.App;
import io.github.lucasstml.towerrogue.ui.Art;
import io.github.lucasstml.towerrogue.ui.Draw;
import io.github.lucasstml.towerrogue.ui.Icons;
import io.github.lucasstml.towerrogue.ui.Screen;
import io.github.lucasstml.towerrogue.ui.Theme;
import io.github.lucasstml.towerrogue.ui.Ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

/** Meta-progression hub: perks, unlocks and run history. */
public final class SanctumScreen implements Screen {
    private final App app;
    private double time;

    public SanctumScreen(App app) {
        this.app = app;
    }

    @Override
    public void update(double dt) {
        time += dt;
    }

    @Override
    public void render(Graphics2D g, Ui ui, double frameDt) {
        Profile p = app.profile;
        Widgets.background(g, time, Theme.BOSS);
        Draw.shadowText(g, "The Sanctum", 640, 46, Theme.title(38), Theme.BOSS);
        Draw.textCentered(g, "Essence earned in every run buys permanent power.", 640, 82, Theme.font(15), Theme.TEXT_DIM);
        Icons.essence(g, 1120, 46, 30);
        Draw.textLeftMid(g, String.valueOf(p.essence), 1140, 46, Theme.bold(26), new Color(0xE9C8FF));

        perks(g, ui, p, 30, 110, 400, 540);
        unlocks(g, ui, p, 448, 110, 400, 540);
        records(g, ui, p, 866, 110, 384, 540);

        if (ui.button("Back", 640 - 90, 664, 180, 42, true, Ui.Style.NORMAL) || ui.in.keyPressed(KeyEvent.VK_ESCAPE)) app.toMainMenu();
    }

    private void perks(Graphics2D g, Ui ui, Profile p, double x, double y, double w, double h) {
        Draw.panel(g, x, y, w, h);
        Draw.text(g, "Perks", x + 20, y + 32, Theme.bold(20), Theme.TEXT);
        double yy = y + 52;
        for (PerkDef perk : app.content.perks()) {
            int lvl = p.perkLevel(perk.id());
            int cost = MetaProgression.perkCost(perk, p);
            Draw.fillRound(g, x + 14, yy, w - 28, 62, 10, new Color(0x181C29));
            Draw.text(g, perk.name(), x + 26, yy + 22, Theme.bold(15), Theme.TEXT);
            Draw.text(g, perk.description(), x + 26, yy + 42, Theme.font(12), Theme.TEXT_DIM);
            for (int i = 0; i < perk.maxLevel(); i++) {
                Draw.circle(g, x + 28 + i * 14, yy + 54, 4, i < lvl ? Theme.BOSS : Theme.BORDER);
            }
            String label = cost < 0 ? "Maxed" : String.valueOf(cost);
            if (ui.button(label, x + w - 104, yy + 14, 76, 34, cost >= 0 && p.essence >= cost, Ui.Style.NORMAL, Theme.bold(14))) {
                if (MetaProgression.buyPerk(perk, p)) app.saveProfile();
            }
            yy += 68;
        }
    }

    private void unlocks(Graphics2D g, Ui ui, Profile p, double x, double y, double w, double h) {
        Draw.panel(g, x, y, w, h);
        Draw.text(g, "Unlocks", x + 20, y + 32, Theme.bold(20), Theme.TEXT);
        Draw.text(g, "Unlocked content can appear in future runs.", x + 20, y + 52, Theme.font(12), Theme.TEXT_FAINT);
        double yy = y + 66;
        for (TowerDef t : app.content.towers()) {
            if (!t.locked()) continue;
            boolean has = p.unlocked.contains(t.id());
            Draw.fillRound(g, x + 14, yy, w - 28, 48, 10, new Color(0x181C29));
            Art.tower(g, t, x + 40, yy + 24, 38, -Math.PI / 4, 0, false, time, false);
            Draw.text(g, t.name(), x + 68, yy + 21, Theme.bold(14), Theme.TEXT);
            Draw.text(g, "Tower · " + t.rarity().label, x + 68, yy + 39, Theme.font(12), Theme.rgb(t.rarity().color));
            if (ui.hover(x + 14, yy, w - 120, 48)) Widgets.towerTooltip(ui, app.content, t, 0);
            unlockButton(ui, p, t.id(), t.unlockCost(), has, x + w - 110, yy + 8);
            yy += 54;
        }
        for (RelicDef r : app.content.relics()) {
            if (!r.locked()) continue;
            boolean has = p.unlocked.contains(r.id());
            Draw.fillRound(g, x + 14, yy, w - 28, 48, 10, new Color(0x181C29));
            Widgets.relicBadge(g, ui, r, x + 40, yy + 24, 32);
            Draw.text(g, r.name(), x + 68, yy + 21, Theme.bold(14), Theme.TEXT);
            Draw.text(g, "Relic · " + r.rarity().label, x + 68, yy + 39, Theme.font(12), Theme.rgb(r.rarity().color));
            if (ui.hover(x + 60, yy, w - 170, 48)) Widgets.relicTooltip(ui, r);
            unlockButton(ui, p, r.id(), r.unlockCost(), has, x + w - 110, yy + 8);
            yy += 54;
        }
    }

    private void unlockButton(Ui ui, Profile p, String id, int cost, boolean has, double x, double y) {
        if (has) {
            Draw.textCentered(ui.g(), "Unlocked", x + 42, y + 16, Theme.bold(13), Theme.GOOD);
        } else if (ui.button(String.valueOf(cost), x, y, 84, 32, p.essence >= cost, Ui.Style.NORMAL, Theme.bold(14))) {
            if (MetaProgression.unlock(p, id, cost)) app.saveProfile();
        }
    }

    private void records(Graphics2D g, Ui ui, Profile p, double x, double y, double w, double h) {
        Draw.panel(g, x, y, w, h);
        Draw.text(g, "Records", x + 20, y + 32, Theme.bold(20), Theme.TEXT);
        String[] lines = {
                "Runs: " + p.runs + "   ·   Wins: " + p.wins,
                "Best: " + p.bestFloor + " floors cleared",
                "Enemies defeated: " + p.totalKills,
                "Bosses slain: " + p.bossesKilled,
                "Essence earned: " + p.totalEssence,
                "Highest Ascension unlocked: " + p.maxAscension,
        };
        for (int i = 0; i < lines.length; i++) Draw.text(g, lines[i], x + 20, y + 62 + i * 21, Theme.font(14), Theme.TEXT_DIM);
        Draw.text(g, "Recent runs", x + 20, y + 206, Theme.bold(15), Theme.TEXT);
        double yy = y + 220;
        if (p.history.isEmpty()) Draw.text(g, "No runs yet.", x + 20, yy + 18, Theme.font(13), Theme.TEXT_FAINT);
        for (Profile.HistoryEntry e : p.history) {
            if (yy > y + h - 30) break;
            Draw.fillRound(g, x + 14, yy, w - 28, 26, 8, new Color(0x181C29));
            Draw.textLeftMid(g, e.won() ? "WIN" : "Act " + e.act(), x + 22, yy + 13, Theme.bold(12), e.won() ? Theme.GOLD : Theme.TEXT_DIM);
            Draw.textLeftMid(g, e.floors() + " floors · A" + e.ascension() + " · +" + e.essence(), x + 82, yy + 13, Theme.font(12), Theme.TEXT_DIM);
            Draw.textRight(g, e.seed(), x + w - 22, yy + 17, Theme.font(11), Theme.TEXT_FAINT);
            if (ui.hover(x + 14, yy, w - 28, 26)) ui.tooltip("Run of " + e.date(), java.util.List.of("Seed " + e.seed() + " (type it in New Run to replay the same map)"));
            yy += 30;
        }
    }
}
