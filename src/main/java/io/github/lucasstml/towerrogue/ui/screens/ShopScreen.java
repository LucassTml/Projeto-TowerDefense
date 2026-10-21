package io.github.lucasstml.towerrogue.ui.screens;

import io.github.lucasstml.towerrogue.run.RunController;
import io.github.lucasstml.towerrogue.run.RunState;
import io.github.lucasstml.towerrogue.run.ShopStock;
import io.github.lucasstml.towerrogue.ui.App;
import io.github.lucasstml.towerrogue.ui.Draw;
import io.github.lucasstml.towerrogue.ui.Icons;
import io.github.lucasstml.towerrogue.ui.Screen;
import io.github.lucasstml.towerrogue.ui.Theme;
import io.github.lucasstml.towerrogue.ui.Ui;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/** Merchant node: buy blueprints and relics, forge a blueprint, heal. */
public final class ShopScreen implements Screen {
    private final App app;
    private final RunController run;
    private final BlueprintPicker picker = new BlueprintPicker();
    private double time;

    public ShopScreen(App app) {
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
        ShopStock shop = s.shop;
        Widgets.background(g, time, Theme.GOLD);
        if (shop == null) return; // left the shop; fading out
        ui.setBlocked(picker.isOpen());
        Widgets.runHud(g, ui, app, "Merchant");
        Draw.textCentered(g, "\"Fine designs and finer trinkets, commander.\"", 500, 74, Theme.font(15), Theme.TEXT_DIM);

        List<Integer> towers = new ArrayList<>(), relics = new ArrayList<>();
        for (int i = 0; i < shop.items.size(); i++) (shop.items.get(i).kind == ShopStock.Kind.TOWER ? towers : relics).add(i);

        double w = 220, h = 300, gap = 22;
        double x0 = 500 - (towers.size() * w + (towers.size() - 1) * gap) / 2;
        for (int k = 0; k < towers.size(); k++) {
            int idx = towers.get(k);
            ShopStock.Item it = shop.items.get(idx);
            double x = x0 + k * (w + gap), y = 96;
            Widgets.towerCard(g, ui, app, app.content.tower(it.id), 0, it.sold ? "SOLD" : null, x, y, w, h, it.sold);
            price(g, it, x + w / 2, y + h + 18, s.gold);
            if (!it.sold && ui.clicked(x, y, w, h)) run.buyItem(idx);
        }
        if (towers.isEmpty()) {
            Draw.textCentered(g, "You already own every blueprint the merchant could offer.", 500, 250, Theme.font(16), Theme.TEXT_DIM);
        }
        if (relics.isEmpty()) {
            Draw.textCentered(g, "No relics left in stock.", 500, 540, Theme.font(16), Theme.TEXT_DIM);
        }
        double rw = 200, rh = 200;
        double r0 = 500 - (relics.size() * rw + (relics.size() - 1) * gap) / 2;
        for (int k = 0; k < relics.size(); k++) {
            int idx = relics.get(k);
            ShopStock.Item it = shop.items.get(idx);
            double x = r0 + k * (rw + gap), y = 440;
            boolean hover = Widgets.relicCard(g, ui, app.content.relic(it.id), it.sold ? "SOLD" : null, x, y, rw, rh, it.sold);
            if (hover) Widgets.relicTooltip(ui, app.content.relic(it.id));
            price(g, it, x + rw / 2, y + rh + 18, s.gold);
            if (!it.sold && ui.clicked(x, y, rw, rh)) run.buyItem(idx);
        }

        // services
        double sx = 990, sy = 96, sw = 270;
        Draw.panel(g, sx, sy, sw, 560);
        Draw.text(g, "Services", sx + 20, sy + 34, Theme.bold(18), Theme.TEXT);
        Draw.text(g, "Forge", sx + 20, sy + 74, Theme.bold(15), Theme.GOLD);
        Draw.paragraph(g, "Permanently upgrade one blueprint (Mk II / Mk III). Once per visit.", sx + 20, sy + 96, sw - 40, Theme.font(13), Theme.TEXT_DIM, 17);
        boolean canForge = !shop.forgeUsed && s.gold >= shop.forgePrice && !s.upgradeableBlueprints().isEmpty();
        if (ui.button(shop.forgeUsed ? "Used" : "Forge  ·  " + shop.forgePrice + " gold", sx + 20, sy + 140, sw - 40, 40, canForge, Ui.Style.NORMAL)) picker.open();
        Draw.text(g, "Healer", sx + 20, sy + 220, Theme.bold(15), Theme.HP);
        int heal = (int) Math.round(s.maxHp * app.content.balance().healServicePct);
        Draw.paragraph(g, "Restore " + heal + " HP. Once per visit.", sx + 20, sy + 242, sw - 40, Theme.font(13), Theme.TEXT_DIM, 17);
        boolean canHeal = !shop.healUsed && s.gold >= shop.healPrice && s.hp < s.maxHp;
        if (ui.button(shop.healUsed ? "Used" : "Heal  ·  " + shop.healPrice + " gold", sx + 20, sy + 270, sw - 40, 40, canHeal, Ui.Style.NORMAL)) run.buyHeal();
        Widgets.blueprintListCompact(g, ui, app, sx + 20, sy + 350, sw - 40);
        if (ui.button("Leave", sx + 20, sy + 500, sw - 40, 44, true, Ui.Style.PRIMARY)) {
            run.leaveShop();
            app.showRunPhase();
        }
        ui.setBlocked(false);

        String picked = picker.render(g, ui, app, "Forge: choose a blueprint");
        if (picked != null) run.buyForge(picked);
    }

    private void price(Graphics2D g, ShopStock.Item it, double cx, double cy, int gold) {
        if (it.sold) return;
        Icons.coin(g, cx - 22, cy, 18);
        Draw.textLeftMid(g, String.valueOf(it.price), cx - 8, cy, Theme.bold(17), gold >= it.price ? Theme.GOLD : Theme.BAD);
    }
}
