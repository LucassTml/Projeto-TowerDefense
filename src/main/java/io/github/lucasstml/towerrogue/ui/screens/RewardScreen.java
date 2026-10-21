package io.github.lucasstml.towerrogue.ui.screens;

import io.github.lucasstml.towerrogue.content.TowerDef;
import io.github.lucasstml.towerrogue.run.Blueprint;
import io.github.lucasstml.towerrogue.run.RewardOption;
import io.github.lucasstml.towerrogue.run.RunController;
import io.github.lucasstml.towerrogue.run.RunState;
import io.github.lucasstml.towerrogue.ui.App;
import io.github.lucasstml.towerrogue.ui.Draw;
import io.github.lucasstml.towerrogue.ui.Icons;
import io.github.lucasstml.towerrogue.ui.Screen;
import io.github.lucasstml.towerrogue.ui.Theme;
import io.github.lucasstml.towerrogue.ui.Ui;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/** Post-battle / treasure rewards: gold, a "pick 1" blueprint draft and relics. */
public final class RewardScreen implements Screen {
    private final App app;
    private final RunController run;
    private double time;

    public RewardScreen(App app) {
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
        Widgets.background(g, time, Theme.GOLD);
        Widgets.runHud(g, ui, app, null);

        List<Integer> draft = new ArrayList<>(), relics = new ArrayList<>();
        for (int i = 0; i < s.rewards.size(); i++) {
            (s.rewards.get(i).group == RewardOption.GROUP_DRAFT ? draft : relics).add(i);
        }
        boolean treasure = draft.isEmpty();
        Draw.shadowText(g, treasure ? "Treasure!" : "Victory!", 640, 86, Theme.title(40), Theme.GOLD);
        Icons.coin(g, 590, 124, 22);
        Draw.textLeftMid(g, "+" + s.lastGoldReward + " gold", 608, 124, Theme.bold(18), Theme.GOLD);

        double y = 150;
        if (!draft.isEmpty()) {
            boolean resolved = run.groupResolved(RewardOption.GROUP_DRAFT);
            Draw.textCentered(g, resolved ? "Blueprint taken" : "Choose one blueprint", 640, y + 6, Theme.bold(16), Theme.TEXT_DIM);
            double w = 224, h = relics.isEmpty() ? 360 : 290, gap = 22;
            double x0 = 640 - (draft.size() * w + (draft.size() - 1) * gap) / 2;
            for (int k = 0; k < draft.size(); k++) {
                int idx = draft.get(k);
                RewardOption o = s.rewards.get(idx);
                double x = x0 + k * (w + gap);
                boolean dim = resolved && !o.taken;
                if (drawOption(g, ui, o, x, y + 24, w, h, dim) && !resolved && ui.clicked(x, y + 24, w, h)) run.takeReward(idx);
                if (o.taken) Draw.textCentered(g, "TAKEN", x + w / 2, y + 24 + h + 16, Theme.bold(14), Theme.GOOD);
            }
            y += h + 50;
        }
        if (!relics.isEmpty()) {
            boolean resolved = run.groupResolved(RewardOption.GROUP_RELIC);
            String label = relics.size() > 1 ? "Choose one relic" : "Relic";
            Draw.textCentered(g, resolved ? "Relic taken" : label, 640, y + 6, Theme.bold(16), Theme.TEXT_DIM);
            // Below a draft, relics use a compact horizontal card so everything fits above the button.
            boolean compact = !treasure;
            double w = compact ? 330 : 220, h = compact ? 110 : 280, gap = 22;
            double x0 = 640 - (relics.size() * w + (relics.size() - 1) * gap) / 2;
            for (int k = 0; k < relics.size(); k++) {
                int idx = relics.get(k);
                RewardOption o = s.rewards.get(idx);
                double x = x0 + k * (w + gap);
                boolean dim = resolved && !o.taken;
                boolean hover = compact && o.kind == RewardOption.Kind.RELIC
                        ? Widgets.relicCardWide(g, ui, app.content.relic(o.id), x, y + 24, w, h, dim)
                        : drawOption(g, ui, o, x, y + 24, w, h, dim);
                if (hover && !resolved && ui.clicked(x, y + 24, w, h)) run.takeReward(idx);
                if (o.taken) Draw.textCentered(g, "TAKEN", x + w / 2, y + 24 + h + 14, Theme.bold(14), Theme.GOOD);
            }
        }

        boolean anyLeft = false;
        for (RewardOption o : s.rewards) if (!o.taken && !run.groupResolved(o.group)) anyLeft = true;
        String label = anyLeft ? "Skip remaining & continue" : "Continue";
        if (ui.button(label, 640 - 140, 660, 280, 46, true, anyLeft ? Ui.Style.NORMAL : Ui.Style.PRIMARY)
                || (!anyLeft && ui.in.keyPressed(KeyEvent.VK_ENTER))) {
            run.continueFromRewards();
            app.showRunPhase();
        }
    }

    /** @return true when hovered */
    private boolean drawOption(Graphics2D g, Ui ui, RewardOption o, double x, double y, double w, double h, boolean dim) {
        return switch (o.kind) {
            case NEW_TOWER -> Widgets.towerCard(g, ui, app, app.content.tower(o.id), 0, "NEW BLUEPRINT", x, y, w, h, dim);
            case UPGRADE_TOWER -> {
                TowerDef def = app.content.tower(o.id);
                yield Widgets.towerCard(g, ui, app, def, o.amount, "UPGRADE →" + Blueprint.tierName(o.amount), x, y, w, h, dim);
            }
            case RELIC -> {
                boolean hover = Widgets.relicCard(g, ui, app.content.relic(o.id), null, x, y, w, h, dim);
                if (hover) Widgets.relicTooltip(ui, app.content.relic(o.id));
                yield hover;
            }
            case GOLD -> {
                boolean hover = !dim && ui.hover(x, y, w, h);
                Draw.panel(g, x, y + (hover ? -6 : 0), w, h, hover ? Theme.PANEL_LIGHT : Theme.PANEL, Theme.GOLD);
                Icons.coin(g, x + w / 2, y + 80, 60);
                Draw.textCentered(g, "+" + o.amount + " gold", x + w / 2, y + 150, Theme.bold(18), Theme.GOLD);
                yield hover;
            }
        };
    }
}
