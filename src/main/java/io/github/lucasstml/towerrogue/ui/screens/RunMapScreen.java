package io.github.lucasstml.towerrogue.ui.screens;

import io.github.lucasstml.towerrogue.content.ActDef;
import io.github.lucasstml.towerrogue.content.NodeType;
import io.github.lucasstml.towerrogue.content.RelicDef;
import io.github.lucasstml.towerrogue.run.RunController;
import io.github.lucasstml.towerrogue.run.RunMap;
import io.github.lucasstml.towerrogue.run.RunState;
import io.github.lucasstml.towerrogue.ui.App;
import io.github.lucasstml.towerrogue.ui.Draw;
import io.github.lucasstml.towerrogue.ui.Icons;
import io.github.lucasstml.towerrogue.ui.Screen;
import io.github.lucasstml.towerrogue.ui.Theme;
import io.github.lucasstml.towerrogue.ui.Ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.geom.Line2D;
import java.util.List;

/** The branching act map: choose the next node. */
public final class RunMapScreen implements Screen {
    private static final double MAP_X = 360, COL_W = 140, BOTTOM = 650, FLOOR_H = 76;

    private final App app;
    private final RunController run;
    private double time;
    private boolean menuOpen;
    private boolean confirmAbandon;

    public RunMapScreen(App app) {
        this.app = app;
        this.run = app.run();
    }

    @Override
    public void update(double dt) {
        time += dt;
    }

    private double nx(RunMap.Node n) {
        if (n.type == NodeType.BOSS) return MAP_X + COL_W * 2;
        return MAP_X + n.col * COL_W + n.jitterX * 60;
    }

    private double ny(RunMap.Node n) {
        return BOTTOM - n.floor * FLOOR_H + n.jitterY * 30 - (n.type == NodeType.BOSS ? 12 : 0);
    }

    @Override
    public void render(Graphics2D g, Ui ui, double frameDt) {
        RunState s = run.state();
        ActDef act = app.content.act(s.act);
        Color accent = Theme.rgb(act.theme().accent());
        Widgets.background(g, time, accent);
        g.setPaint(new GradientPaint(330, 0, new Color(0x1A2030), 330, 720, new Color(0x121622)));
        g.fillRoundRect(318, 56, 644, 652, 20, 20);
        Draw.strokeRound(g, 318, 56, 644, 652, 20, Theme.BORDER, 1);

        ui.setBlocked(menuOpen);
        Widgets.runHud(g, ui, app, "Act " + s.act + ": " + act.name());
        drawLeftPanel(g, ui, s, act);
        drawRightPanel(g, ui, s);
        drawMap(g, ui, s);
        ui.setBlocked(false);

        boolean escape = ui.in.keyPressed(KeyEvent.VK_ESCAPE);
        if (menuOpen) drawMenu(g, ui, escape);
        else if (escape) menuOpen = true;
    }

    private void drawMap(Graphics2D g, Ui ui, RunState s) {
        RunMap map = run.map();
        List<RunMap.Node> reachable = run.reachableNodes();
        // edges
        for (RunMap.Node n : map.nodes()) {
            for (int id : n.next) {
                RunMap.Node m = map.node(id);
                boolean taken = s.visited.contains(n.id) && s.visited.contains(m.id);
                boolean available = (n.id == s.currentNode) && reachable.contains(m);
                if (taken) {
                    Draw.line(g, nx(n), ny(n), nx(m), ny(m), Theme.GOLD, 3.5);
                } else {
                    float[] dash = {6f, 7f};
                    Color c = available ? new Color(242, 193, 78, 200) : new Color(140, 150, 180, 70);
                    g.setColor(c);
                    g.setStroke(new BasicStroke(available ? 2.6f : 1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash,
                            available ? (float) (13 - (time * 18) % 13) : 0f));
                    g.draw(new Line2D.Double(nx(n), ny(n), nx(m), ny(m)));
                }
            }
        }
        RunMap.Node hovered = null;
        for (RunMap.Node n : map.nodes()) {
            double x = nx(n), y = ny(n);
            double r = n.type == NodeType.BOSS ? 34 : 22;
            boolean canGo = reachable.contains(n);
            boolean visited = s.visited.contains(n.id);
            boolean current = n.id == s.currentNode;
            Color c = nodeColor(n.type);
            if (canGo) {
                double pulse = 0.5 + 0.5 * Math.sin(time * 4);
                Draw.circle(g, x, y, r + 8 + pulse * 4, Theme.alpha(c, 0.22));
            }
            Draw.circle(g, x + 1, y + 3, r, new Color(0, 0, 0, 120));
            Color fill = visited ? Theme.shade(c, 0.45) : canGo ? new Color(0x262C3F) : new Color(0x1A1F2D);
            Draw.circle(g, x, y, r, fill);
            Draw.ring(g, x, y, r, current ? Theme.GOLD : canGo ? c : Theme.alpha(c, visited ? 0.8 : 0.45), current ? 3 : 2);
            Icons.node(g, n.type, x, y, r * 1.05, visited ? Theme.alpha(Color.WHITE, 0.7) : canGo ? c : Theme.alpha(c, 0.55));
            if (ui.hover(x - r, y - r, r * 2, r * 2)) hovered = n;
        }
        if (hovered != null) {
            boolean canGo = reachable.contains(hovered);
            ui.tooltip(hovered.type.label, nodeColor(hovered.type), List.of(describe(hovered.type),
                    canGo ? "Click to travel here." : "Not reachable from your position."));
            if (canGo && ui.clicked(nx(hovered) - 30, ny(hovered) - 30, 60, 60)) {
                if (run.chooseNode(hovered.id)) app.showRunPhase();
            }
        }
        if (s.currentNode < 0) Draw.textCentered(g, "Choose a starting point", 640, 694, Theme.bold(14), Theme.GOLD);
    }

    static Color nodeColor(NodeType t) {
        return switch (t) {
            case COMBAT -> new Color(0xD9D4C7);
            case ELITE -> Theme.ELITE;
            case EVENT -> new Color(0x8FC8FF);
            case SHOP -> Theme.GOLD;
            case REST -> new Color(0xFF8A3D);
            case TREASURE -> new Color(0x6AD182);
            case BOSS -> Theme.BOSS;
        };
    }

    static String describe(NodeType t) {
        return switch (t) {
            case COMBAT -> "A regular battle. Reward: gold and a blueprint draft.";
            case ELITE -> "Affixed elite enemies. Harder, but also drops a relic.";
            case EVENT -> "Something unexpected: a choice with risks and rewards.";
            case SHOP -> "Buy blueprints and relics, upgrade a blueprint or heal.";
            case REST -> "Heal, or upgrade one blueprint permanently.";
            case TREASURE -> "A chest with gold and a relic.";
            case BOSS -> "The act boss. Defeat it to advance.";
        };
    }

    private void drawLeftPanel(Graphics2D g, Ui ui, RunState s, ActDef act) {
        Draw.panel(g, 16, 56, 290, 652);
        Draw.text(g, act.name(), 34, 90, Theme.title(22), Theme.TEXT);
        Draw.paragraph(g, act.subtitle(), 34, 114, 254, Theme.font(13), Theme.TEXT_DIM, 17);
        String[] stats = {
                "Floors cleared: " + s.floorsCleared,
                "Enemies defeated: " + s.kills,
                "Elites slain: " + s.elitesKilled + "   ·   Bosses: " + s.bossesKilled,
        };
        for (int i = 0; i < stats.length; i++) Draw.text(g, stats[i], 34, 160 + i * 20, Theme.font(13), Theme.TEXT_DIM);
        Widgets.blueprintList(g, ui, app, 30, 240, 262);
    }

    private void drawRightPanel(Graphics2D g, Ui ui, RunState s) {
        Draw.panel(g, 976, 56, 288, 652);
        Draw.text(g, "Relics", 994, 90, Theme.bold(16), Theme.TEXT);
        if (s.relics.isEmpty()) Draw.text(g, "None yet. Elites, chests and shops have them.", 994, 114, Theme.font(12), Theme.TEXT_FAINT);
        for (int i = 0; i < s.relics.size(); i++) {
            RelicDef d = app.content.relic(s.relics.get(i));
            int col = i % 6, row = i / 6;
            Widgets.relicBadge(g, ui, d, 1012 + col * 42, 124 + row * 42, 34);
        }
        Draw.text(g, "Legend", 994, 440, Theme.bold(16), Theme.TEXT);
        NodeType[] types = NodeType.values();
        for (int i = 0; i < types.length; i++) {
            double y = 466 + i * 32;
            Icons.node(g, types[i], 1006, y, 20, nodeColor(types[i]));
            Draw.textLeftMid(g, types[i].label, 1026, y, Theme.font(14), Theme.TEXT_DIM);
            if (ui.hover(994, y - 14, 260, 28)) ui.tooltip(types[i].label, nodeColor(types[i]), List.of(describe(types[i])));
        }
    }

    private void drawMenu(Graphics2D g, Ui ui, boolean escape) {
        g.setColor(new Color(5, 6, 10, 170));
        g.fillRect(0, 0, 1280, 720);
        double w = 360, h = 280, x = 640 - w / 2, y = 360 - h / 2;
        Draw.panel(g, x, y, w, h);
        Draw.shadowText(g, "Menu", 640, y + 40, Theme.title(28), Theme.TEXT);
        if (ui.button("Resume", x + 40, y + 74, w - 80, 44, true, Ui.Style.PRIMARY) || escape) {
            menuOpen = false;
            confirmAbandon = false;
        }
        if (ui.button("Save & quit to menu", x + 40, y + 128, w - 80, 44, true)) app.toMainMenu();
        if (ui.button(confirmAbandon ? "Click again to abandon" : "Abandon run", x + 40, y + 182, w - 80, 44, true, Ui.Style.DANGER)) {
            if (confirmAbandon) {
                run.abandon();
                app.showRunPhase();
            }
            confirmAbandon = true;
        }
    }
}
