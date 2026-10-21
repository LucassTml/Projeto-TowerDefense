package io.github.lucasstml.towerrogue.ui.screens;

import io.github.lucasstml.towerrogue.content.TowerDef;
import io.github.lucasstml.towerrogue.run.Blueprint;
import io.github.lucasstml.towerrogue.ui.App;
import io.github.lucasstml.towerrogue.ui.Draw;
import io.github.lucasstml.towerrogue.ui.Theme;
import io.github.lucasstml.towerrogue.ui.Ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.List;

/** Modal "choose a blueprint to upgrade" overlay used by the forge (shop and campfire). */
final class BlueprintPicker {
    private boolean open;

    void open() {
        open = true;
    }

    void close() {
        open = false;
    }

    boolean isOpen() {
        return open;
    }

    /** @return the tower id the player picked this frame, or null */
    String render(Graphics2D g, Ui ui, App app, String title) {
        if (!open) return null;
        g.setColor(new Color(5, 6, 10, 190));
        g.fillRect(0, 0, 1280, 720);
        Draw.shadowText(g, title, 640, 70, Theme.title(30), Theme.GOLD);
        Draw.textCentered(g, "Upgrades are permanent for this run: +25% damage and stronger effects per tier.", 640, 104,
                Theme.font(14), Theme.TEXT_DIM);
        List<Blueprint> list = app.run().state().upgradeableBlueprints();
        int perRow = 5;
        double w = 210, h = 290, gap = 18;
        String picked = null;
        for (int i = 0; i < list.size(); i++) {
            Blueprint b = list.get(i);
            TowerDef def = app.content.tower(b.towerId);
            int row = i / perRow, col = i % perRow;
            int inRow = Math.min(perRow, list.size() - row * perRow);
            double x = 640 - (inRow * w + (inRow - 1) * gap) / 2 + col * (w + gap);
            double y = 130 + row * (h + 16);
            Widgets.towerCard(g, ui, app, def, b.tier + 1, "→" + Blueprint.tierName(b.tier + 1), x, y, w, h, false);
            if (ui.clicked(x, y, w, h)) picked = b.towerId;
        }
        if (list.isEmpty()) Draw.textCentered(g, "Every blueprint is fully upgraded.", 640, 300, Theme.bold(18), Theme.TEXT_DIM);
        if (ui.button("Cancel", 640 - 90, 650, 180, 44, true) || ui.in.keyPressed(KeyEvent.VK_ESCAPE)) open = false;
        if (picked != null) open = false;
        return picked;
    }
}
