package io.github.lucasstml.towerrogue.ui.screens;

import io.github.lucasstml.towerrogue.battle.Battle;
import io.github.lucasstml.towerrogue.battle.BattleCommand;
import io.github.lucasstml.towerrogue.battle.BattleMap;
import io.github.lucasstml.towerrogue.battle.Enemy;
import io.github.lucasstml.towerrogue.battle.Fx;
import io.github.lucasstml.towerrogue.battle.Lane;
import io.github.lucasstml.towerrogue.battle.Projectile;
import io.github.lucasstml.towerrogue.battle.Synergies;
import io.github.lucasstml.towerrogue.battle.Tower;
import io.github.lucasstml.towerrogue.battle.WavePlan;
import io.github.lucasstml.towerrogue.content.AffixDef;
import io.github.lucasstml.towerrogue.content.AttackKind;
import io.github.lucasstml.towerrogue.content.DamageType;
import io.github.lucasstml.towerrogue.content.EncounterKind;
import io.github.lucasstml.towerrogue.content.EnemyDef;
import io.github.lucasstml.towerrogue.content.OnHitDef;
import io.github.lucasstml.towerrogue.content.Stat;
import io.github.lucasstml.towerrogue.content.StatMod;
import io.github.lucasstml.towerrogue.content.StatSheet;
import io.github.lucasstml.towerrogue.content.StatusType;
import io.github.lucasstml.towerrogue.content.SynergyDef;
import io.github.lucasstml.towerrogue.content.TowerDef;
import io.github.lucasstml.towerrogue.run.Blueprint;
import io.github.lucasstml.towerrogue.run.RunController;
import io.github.lucasstml.towerrogue.ui.App;
import io.github.lucasstml.towerrogue.ui.Art;
import io.github.lucasstml.towerrogue.ui.Draw;
import io.github.lucasstml.towerrogue.ui.GameApp;
import io.github.lucasstml.towerrogue.ui.Icons;
import io.github.lucasstml.towerrogue.ui.Particles;
import io.github.lucasstml.towerrogue.ui.Screen;
import io.github.lucasstml.towerrogue.ui.Theme;
import io.github.lucasstml.towerrogue.ui.Ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The tower-defense battle. Owns a {@link Battle} simulation and is only a view/controller
 * around it: every player action becomes a {@link BattleCommand}.
 */
public final class BattleScreen implements Screen {
    private static final int TILE = 60;
    private static final int BX = 12, BY = 54;
    private static final int BW = BattleMap.COLS * TILE, BH = BattleMap.ROWS * TILE;
    private static final int SX = 924, SY = 54, SW = 344;
    private static final int CARD_W = 104, CARD_H = 56;

    private final App app;
    private final RunController run;
    private final Battle battle;
    private final Particles fx = new Particles();
    private final BufferedImage background;
    private final List<String> blueprintIds;
    private final double[] tmp = new double[2];

    private int speed = 1;
    private boolean paused;
    private boolean menuOpen;
    private boolean confirmAbandon;
    private boolean menuJustOpened;
    private String placing;
    private Tower selected;
    private double shake;
    private double time;
    private boolean ended;
    private double endTimer;
    private String toast;
    private double toastTime;

    public BattleScreen(App app) {
        this.app = app;
        this.run = app.run();
        this.battle = new Battle(app.content, run.battleConfig());
        this.battle.setFxSink(this::onFx);
        this.blueprintIds = new ArrayList<>(battle.config().blueprints().keySet());
        this.background = renderBackground();
    }

    // ================================================================== loop

    @Override
    public void update(double dt) {
        time += dt;
        if (!paused && !menuOpen && !ended) {
            for (int i = 0; i < speed; i++) battle.step();
        }
        fx.update(dt);
        shake = Math.max(0, shake - dt * 30);
        if (toastTime > 0) toastTime -= dt;
        if (!ended && (battle.state() == Battle.State.WON || battle.state() == Battle.State.LOST)) {
            ended = true;
            placing = null;
            selected = null;
        }
        if (ended) endTimer += dt;
    }

    @Override
    public void render(Graphics2D g, Ui ui, double frameDt) {
        g.setPaint(new GradientPaint(0, 44, new Color(0x141824), 0, 720, new Color(0x0C0E15)));
        g.fillRect(0, 44, 1280, 676);
        if (!menuOpen && !ended) handleKeys(ui);
        if (selected != null && !battle.towers().contains(selected)) selected = null;

        drawTopBar(g, ui);
        ui.setBlocked(menuOpen || ended);
        drawBoard(g, ui);
        drawSidePanel(g, ui);
        drawBottomStrip(g, ui);
        ui.setBlocked(false);

        if (toastTime > 0 && toast != null) {
            double a = Math.min(1, toastTime * 2);
            double w = Draw.width(g, toast, Theme.bold(15)) + 32;
            Draw.fillRound(g, BX + BW / 2.0 - w / 2, BY + 12, w, 32, 16, new Color(20, 22, 32, (int) (220 * a)));
            Draw.textCentered(g, toast, BX + BW / 2.0, BY + 28, Theme.bold(15), Theme.alpha(Theme.TEXT, a));
        }
        if (menuOpen) drawPauseMenu(g, ui);
        if (ended && endTimer > 0.8) drawEndOverlay(g, ui);
    }

    private void handleKeys(Ui ui) {
        var in = ui.in;
        for (int i = 0; i < Math.min(9, blueprintIds.size()); i++) {
            if (in.keyPressed(KeyEvent.VK_1 + i)) selectBlueprint(blueprintIds.get(i));
        }
        if (in.keyPressed(KeyEvent.VK_SPACE)) startWave();
        if (in.keyPressed(KeyEvent.VK_ESCAPE)) {
            if (placing != null) placing = null;
            else if (selected != null) selected = null;
            else {
                menuOpen = true;
                menuJustOpened = true;
            }
        }
        if (in.keyPressed(KeyEvent.VK_P)) paused = !paused;
        if (in.keyPressed(KeyEvent.VK_F) || in.keyPressed(KeyEvent.VK_TAB)) speed = speed % 3 + 1;
        if (in.keyPressed(KeyEvent.VK_D)) {
            app.damageNumbers = !app.damageNumbers;
            showToast("Damage numbers " + (app.damageNumbers ? "on" : "off"));
        }
        if (selected != null) {
            if (in.keyPressed(KeyEvent.VK_U) && !selected.isMaxLevel()) submit(new BattleCommand.Upgrade(selected.id));
            if (in.keyPressed(KeyEvent.VK_S)) sellSelected();
            if (in.keyPressed(KeyEvent.VK_T)) submit(new BattleCommand.SetTarget(selected.id, selected.targetMode().next()));
        }
        if (in.rightClicked()) {
            placing = null;
            selected = null;
        }
    }

    private void selectBlueprint(String id) {
        placing = id.equals(placing) ? null : id;
        selected = null;
    }

    private void startWave() {
        if (battle.canStartWave()) submit(new BattleCommand.StartWave());
    }

    private void sellSelected() {
        if (selected == null) return;
        submit(new BattleCommand.Sell(selected.id));
        selected = null;
    }

    /** Commands are executed immediately (also while paused) so the UI reacts at once. */
    private boolean submit(BattleCommand c) {
        int before = battle.commandLog().size();
        battle.clearRejection();
        battle.submit(c);
        battle.processCommands();
        boolean ok = battle.commandLog().size() > before;
        if (!ok && battle.lastRejection() != null) showToast(battle.lastRejection());
        return ok;
    }

    private void showToast(String text) {
        toast = text;
        toastTime = 1.6;
    }

    // ================================================================== fx bridge

    private double px(double tileX) {
        return BX + tileX * TILE;
    }

    private double py(double tileY) {
        return BY + tileY * TILE;
    }

    private void onFx(Fx f) {
        double x = px(f.x()), y = py(f.y());
        Color c = Theme.rgb(f.color());
        switch (f.type()) {
            case HIT -> {
                if (f.text() != null) return; // damage over time: no spam
                if (app.damageNumbers && f.value() >= 12) {
                    fx.text(x, y - 16, Widgets.fmt(Math.round(f.value())), Theme.alpha(Theme.shade(c, 1.2), 0.9), Theme.bold(12), 0.6);
                }
                fx.sparks(x, y, c, 2, 140);
            }
            case CRIT -> {
                if (app.damageNumbers) fx.text(x, y - 20, Math.round(f.value()) + "!", Theme.GOLD, Theme.bold(16), 0.85);
                fx.sparks(x, y, Theme.GOLD, 6, 200);
            }
            case DEATH -> {
                fx.burst(x, y, c, 12, 170, 3.6, 0.55);
                fx.ring(x, y, 4, f.value() * TILE * 1.8, Theme.alpha(c, 0.8), 0.3);
            }
            case TEXT -> fx.text(x, y, f.text(), c, Theme.bold(14), 1.5);
            case CHAIN -> fx.arc(x, y, px(f.x2()), py(f.y2()), c, 0.16);
            case PULSE -> fx.ring(x, y, 8, f.value() * TILE, Theme.alpha(c, 0.85), 0.35);
            case EXPLOSION -> {
                fx.flash(x, y, f.value() * TILE * 0.8, Theme.alpha(c, 0.35), 0.18);
                fx.ring(x, y, 4, f.value() * TILE, Theme.alpha(c, 0.9), 0.3);
                fx.burst(x, y, c, 9, 170, 3, 0.4);
            }
            case LEAK -> {
                shake = Math.max(shake, 9);
                fx.text(x - 20, y - 20, "-" + (int) f.value() + " HP", Theme.HP, Theme.bold(18), 1.1);
                fx.flash(x, y, 42, Theme.alpha(Theme.HP, 0.45), 0.3);
            }
            case MANA -> fx.text(x + 12, y - 26, "+" + (int) f.value(), Theme.alpha(Theme.MANA, 0.95), Theme.bold(11), 0.7);
            case SHOCKWAVE -> {
                shake = Math.max(shake, 11);
                fx.ring(x, y, 10, f.value() * TILE, Theme.alpha(c, 0.9), 0.6);
                fx.ring(x, y, 4, f.value() * TILE * 0.7, Theme.alpha(Color.WHITE, 0.6), 0.45);
            }
            case PHASE -> {
                shake = Math.max(shake, 7);
                fx.ring(x, y, 10, f.value() * TILE, Theme.GOLD, 0.7);
                fx.burst(x, y, Theme.GOLD, 18, 220, 3.5, 0.7);
            }
            case SHATTER -> fx.shards(x, y, new Color(0xCFF4FF), 10, 7);
            case HEAL -> {
                fx.ring(x, y, 4, f.value() * TILE, Theme.alpha(Theme.GOOD, 0.8), 0.5);
                for (int i = 0; i < 4; i++) fx.bubble(x + (i - 1.5) * 8, y, 3, Theme.GOOD);
            }
            case BLINK -> {
                fx.ring(x, y, 2, 18, Theme.alpha(c, 0.8), 0.3);
                fx.ring(px(f.x2()), py(f.y2()), 18, 2, Theme.alpha(c, 0.8), 0.3);
                fx.arc(x, y, px(f.x2()), py(f.y2()), Theme.alpha(c, 0.6), 0.2);
            }
            case SPAWN -> fx.ring(x, y, 4, f.value() * TILE, Theme.alpha(c, 0.8), 0.4);
            case MUZZLE -> fx.flash(x, y, 7, Theme.alpha(c.brighter(), 0.7), 0.07);
        }
    }

    // ================================================================== top bar

    private void drawTopBar(Graphics2D g, Ui ui) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x1E2436), 0, 44, new Color(0x151925)));
        g.fillRect(0, 0, 1280, 44);
        Draw.line(g, 0, 44, 1280, 44, Theme.BORDER, 1);
        Icons.heart(g, 24, 22, 22, Theme.HP);
        Draw.textLeftMid(g, battle.hp() + " / " + battle.maxHp(), 42, 22, Theme.bold(17), Theme.TEXT);
        Icons.coin(g, 140, 22, 20);
        Draw.textLeftMid(g, String.valueOf(run.state().gold), 156, 22, Theme.bold(17), Theme.GOLD);
        String where = "Act " + run.state().act + " · " + battle.act().name();
        Draw.textLeftMid(g, where, 226, 22, Theme.bold(15), Theme.TEXT_DIM);

        String title = switch (battle.kind()) {
            case NORMAL -> "Battle";
            case ELITE -> "Elite Battle";
            case BOSS -> "Boss: " + app.content.enemy(battle.act().boss()).name();
        };
        Color tc = battle.kind() == EncounterKind.BOSS ? Theme.BOSS : battle.kind() == EncounterKind.ELITE ? Theme.ELITE : Theme.TEXT;
        Draw.shadowText(g, title, 640, 22, Theme.title(20), tc);

        double x = 1270;
        List<String> relics = run.state().relics;
        for (int i = relics.size() - 1; i >= 0; i--) {
            x -= 34;
            if (x < 820) break;
            Widgets.relicBadge(g, ui, app.content.relic(relics.get(i)), x + 16, 22, 28);
        }
    }

    // ================================================================== board

    private BufferedImage renderBackground() {
        BufferedImage img = new BufferedImage(BW, BH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        GameApp.applyHints(g);
        Random rnd = new Random(battle.config().seed());
        var theme = battle.act().theme();
        Color grass = Theme.rgb(theme.grass()), grassAlt = Theme.rgb(theme.grassAlt());
        BattleMap map = battle.map();
        for (int x = 0; x < BattleMap.COLS; x++) {
            for (int y = 0; y < BattleMap.ROWS; y++) {
                Color base = ((x + y) % 2 == 0) ? grass : grassAlt;
                double j = 0.94 + rnd.nextDouble() * 0.12;
                g.setColor(Theme.shade(base, j));
                g.fillRect(x * TILE, y * TILE, TILE, TILE);
                for (int k = 0; k < 6; k++) {
                    double gx = x * TILE + rnd.nextDouble() * TILE, gy = y * TILE + rnd.nextDouble() * TILE;
                    Draw.line(g, gx, gy, gx + (rnd.nextDouble() - 0.5) * 3, gy - 3 - rnd.nextDouble() * 3,
                            Theme.alpha(Theme.shade(base, rnd.nextBoolean() ? 1.25 : 0.75), 0.6), 1);
                }
            }
        }
        // roads: thick rounded polylines through the lane waypoints
        Color edge = Theme.rgb(theme.pathEdge()), path = Theme.rgb(theme.path());
        for (Lane lane : map.groundLanes()) strokeLane(g, lane, TILE * 0.9, edge);
        for (Lane lane : map.groundLanes()) strokeLane(g, lane, TILE * 0.72, path);
        for (List<int[]> cells : map.pathCells()) {
            for (int[] c : cells) {
                for (int k = 0; k < 5; k++) {
                    Draw.circle(g, c[0] * TILE + 8 + rnd.nextDouble() * (TILE - 16), c[1] * TILE + 8 + rnd.nextDouble() * (TILE - 16),
                            1 + rnd.nextDouble() * 1.5, Theme.alpha(Theme.shade(path, rnd.nextBoolean() ? 0.75 : 1.2), 0.7));
                }
            }
        }
        // faint build grid
        for (int x = 0; x < BattleMap.COLS; x++) {
            for (int y = 0; y < BattleMap.ROWS; y++) {
                if (map.buildable(x, y)) Draw.strokeRound(g, x * TILE + 2, y * TILE + 2, TILE - 4, TILE - 4, 8, new Color(255, 255, 255, 16), 1);
            }
        }
        Color rock = Theme.rgb(theme.rock());
        for (int x = 0; x < BattleMap.COLS; x++) {
            for (int y = 0; y < BattleMap.ROWS; y++) {
                if (map.tile(x, y) == BattleMap.Tile.ROCK) drawRock(g, x * TILE + TILE / 2.0, y * TILE + TILE / 2.0, rock, rnd);
                if (map.tile(x, y) == BattleMap.Tile.RUNE) drawRune(g, x * TILE + TILE / 2.0, y * TILE + TILE / 2.0, Theme.rgb(theme.accent()));
            }
        }
        g.dispose();
        return img;
    }

    private static void strokeLane(Graphics2D g, Lane lane, double width, Color c) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < lane.pointCount(); i++) {
            double x = lane.pointX(i) * TILE, y = lane.pointY(i) * TILE;
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
        }
        g.setColor(c);
        g.setStroke(new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(p);
    }

    private static void drawRock(Graphics2D g, double cx, double cy, Color c, Random rnd) {
        int n = 1 + rnd.nextInt(3);
        for (int i = 0; i < n; i++) {
            double r = TILE * (0.18 + rnd.nextDouble() * 0.12);
            double x = cx + (rnd.nextDouble() - 0.5) * TILE * 0.4, y = cy + (rnd.nextDouble() - 0.5) * TILE * 0.35;
            Path2D p = new Path2D.Double();
            int pts = 7;
            for (int k = 0; k < pts; k++) {
                double a = k * Math.PI * 2 / pts;
                double rr = r * (0.8 + rnd.nextDouble() * 0.3);
                double vx = x + Math.cos(a) * rr, vy = y + Math.sin(a) * rr * 0.8;
                if (k == 0) p.moveTo(vx, vy);
                else p.lineTo(vx, vy);
            }
            p.closePath();
            g.setColor(new Color(0, 0, 0, 70));
            g.fill(java.awt.geom.AffineTransform.getTranslateInstance(2, 4).createTransformedShape(p));
            g.setPaint(new GradientPaint((float) x, (float) (y - r), Theme.shade(c, 1.35), (float) x, (float) (y + r), Theme.shade(c, 0.7)));
            g.fill(p);
            g.setColor(Theme.shade(c, 0.5));
            g.setStroke(Draw.stroke(1));
            g.draw(p);
        }
    }

    private static void drawRune(Graphics2D g, double cx, double cy, Color c) {
        Draw.ring(g, cx, cy, TILE * 0.36, Theme.alpha(c, 0.55), 2);
        Draw.ring(g, cx, cy, TILE * 0.26, Theme.alpha(c, 0.35), 1.2);
        g.setColor(Theme.alpha(c, 0.45));
        g.setStroke(Draw.stroke(1.4));
        g.draw(Icons.starShape(cx, cy, TILE * 0.22, TILE * 0.1, 6, 0));
    }

    private void drawBoard(Graphics2D g, Ui ui) {
        double sx = shake > 0 ? (Math.sin(time * 90) * shake * 0.5) : 0;
        double sy = shake > 0 ? (Math.cos(time * 77) * shake * 0.5) : 0;
        var oldTransform = g.getTransform();
        var oldClip = g.getClip();
        g.translate(sx, sy);
        Draw.fillRound(g, BX - 4, BY - 4, BW + 8, BH + 8, 10, new Color(0x0B0D14));
        g.clipRect(BX, BY, BW, BH);
        g.drawImage(background, BX, BY, null);

        var theme = battle.act().theme();
        Color accent = Theme.rgb(theme.accent());
        BattleMap map = battle.map();
        // rune glow
        for (int x = 0; x < BattleMap.COLS; x++) {
            for (int y = 0; y < BattleMap.ROWS; y++) {
                if (map.tile(x, y) == BattleMap.Tile.RUNE) {
                    double a = 0.12 + 0.1 * Math.sin(time * 2 + x + y);
                    Draw.circle(g, px(x + 0.5), py(y + 0.5), TILE * 0.42, Theme.alpha(accent, a));
                }
            }
        }
        // flight lines and road flow
        for (Lane air : map.airLanes()) {
            g.setColor(new Color(170, 210, 255, 55));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{8f, 10f}, (float) (18 - (time * 20) % 18)));
            g.draw(new java.awt.geom.Line2D.Double(px(air.pointX(0)), py(air.pointY(0)), px(air.pointX(1)), py(air.pointY(1))));
        }
        for (Lane lane : map.groundLanes()) {
            for (double d = (time * 1.4) % 1.5; d < lane.length(); d += 1.5) {
                lane.positionAt(d, tmp);
                double x0 = tmp[0], y0 = tmp[1];
                lane.positionAt(d + 0.1, tmp);
                double ang = Math.atan2(tmp[1] - y0, tmp[0] - x0);
                chevron(g, px(x0), py(y0), ang, new Color(255, 255, 255, 40));
            }
        }
        for (Lane lane : map.groundLanes()) {
            double x = (lane.pointX(0) + lane.pointX(1)) / 2, y = (lane.pointY(0) + lane.pointY(1)) / 2;
            portal(g, px(Math.max(0.15, Math.min(BattleMap.COLS - 0.15, x))), py(Math.max(0.15, Math.min(BattleMap.ROWS - 0.15, y))));
        }
        drawBase(g, px(map.baseX() + 0.5), py(map.baseY() + 0.5));

        int tx = (int) Math.floor((ui.in.mx - BX) / TILE), ty = (int) Math.floor((ui.in.my - BY) / TILE);
        boolean overBoard = ui.hover(BX, BY, BW, BH);
        Tower hoverTower = overBoard ? battle.towerAt(tx, ty) : null;

        Tower ranged = selected != null ? selected : (placing == null ? hoverTower : null);
        if (ranged != null) drawRange(g, ranged.x(), ranged.y(), ranged.range(), ranged.providesAura() ? Theme.GOLD : Color.WHITE);
        if (selected != null && selected.providesAura()) {
            for (Tower t : battle.towers()) {
                if (t == selected) continue;
                if (Math.hypot(t.x() - selected.x(), t.y() - selected.y()) <= selected.range()) {
                    Draw.line(g, px(selected.x()), py(selected.y()), px(t.x()), py(t.y()), Theme.alpha(Theme.GOLD, 0.45), 2);
                }
            }
        }

        for (Tower t : battle.towers()) {
            Art.tower(g, t.def, px(t.x()), py(t.y()), TILE, t.aim, t.level(), t.branch() != null, time, t.disabled() > 0);
            if (t == selected) Draw.strokeRound(g, px(t.cx) + 3, py(t.cy) + 3, TILE - 6, TILE - 6, 14, Theme.GOLD, 2);
        }

        Enemy hoverEnemy = null;
        double best = 1e9;
        for (Enemy e : battle.enemies()) {
            if (!e.alive || e.flying()) continue;
            drawEnemy(g, e);
            double d = Math.hypot(ui.in.mx - px(e.x), ui.in.my - py(e.y));
            if (overBoard && d < e.radius() * TILE + 6 && d < best) {
                best = d;
                hoverEnemy = e;
            }
        }
        for (Projectile p : battle.projectiles()) drawProjectile(g, p);
        for (Enemy e : battle.enemies()) {
            if (!e.alive || !e.flying()) continue;
            drawEnemy(g, e);
            double d = Math.hypot(ui.in.mx - px(e.x), ui.in.my - (py(e.y) - e.radius() * TILE * 0.55));
            if (overBoard && d < e.radius() * TILE + 6 && d < best) {
                best = d;
                hoverEnemy = e;
            }
        }
        fx.render(g);

        if (placing != null && overBoard && BattleMap.inBounds(tx, ty)) drawPlacementGhost(g, tx, ty);
        drawBossBar(g);
        g.setClip(oldClip);
        g.setTransform(oldTransform);

        if (hoverEnemy != null && placing == null) enemyTooltip(ui, hoverEnemy);
        else if (hoverTower != null && placing == null && hoverTower != selected) {
            ui.tooltip(hoverTower.def.name() + " · Lv " + hoverTower.level(), List.of(
                    "Kills " + hoverTower.kills() + " · Damage dealt " + Math.round(hoverTower.damageDealt()),
                    "Click to select"));
        }

        if (overBoard && ui.in.clicked()) {
            ui.in.consumeClick();
            if (placing != null) {
                if (BattleMap.inBounds(tx, ty)) {
                    String id = placing;
                    if (submit(new BattleCommand.Place(id, tx, ty)) && !ui.in.shift) placing = null;
                }
            } else {
                selected = battle.towerAt(tx, ty);
            }
        }
    }

    private void chevron(Graphics2D g, double x, double y, double ang, Color c) {
        double s = 6;
        double ax = Math.cos(ang), ay = Math.sin(ang);
        double nx = -ay, ny = ax;
        Path2D p = new Path2D.Double();
        p.moveTo(x - ax * s + nx * s, y - ay * s + ny * s);
        p.lineTo(x + ax * s * 0.4, y + ay * s * 0.4);
        p.lineTo(x - ax * s - nx * s, y - ay * s - ny * s);
        g.setColor(c);
        g.setStroke(Draw.stroke(2.2));
        g.draw(p);
    }

    private void portal(Graphics2D g, double x, double y) {
        for (int i = 0; i < 3; i++) {
            double r = 16 + i * 6 + Math.sin(time * 3 + i) * 2;
            g.setColor(new Color(150, 80, 220, 90 - i * 25));
            g.setStroke(Draw.stroke(3));
            g.draw(new java.awt.geom.Arc2D.Double(x - r, y - r, r * 2, r * 2, time * 200 * (i % 2 == 0 ? 1 : -1) + i * 60, 250, java.awt.geom.Arc2D.OPEN));
        }
        Draw.circle(g, x, y, 10, new Color(30, 10, 50, 200));
    }

    private void drawBase(Graphics2D g, double x, double y) {
        double s = TILE * 0.8;
        Draw.fillRound(g, x - s / 2 + 2, y - s / 2 + 5, s, s, 8, new Color(0, 0, 0, 90));
        g.setPaint(new GradientPaint((float) x, (float) (y - s / 2), new Color(0xB8B4A8), (float) x, (float) (y + s / 2), new Color(0x6E6A60)));
        g.fill(new java.awt.geom.RoundRectangle2D.Double(x - s / 2, y - s / 2 + 6, s, s - 6, 6, 6));
        for (int i = 0; i < 4; i++) {
            g.fill(new java.awt.geom.Rectangle2D.Double(x - s / 2 + i * s / 3.5, y - s / 2, s / 6, 10));
        }
        Draw.fillRound(g, x - 7, y + 4, 14, s / 2 - 6, 6, new Color(0x3A2A1E));
        double frac = battle.hp() / (double) Math.max(1, battle.maxHp());
        Color flag = frac > 0.5 ? Theme.GOOD : frac > 0.25 ? Theme.GOLD : Theme.HP;
        Draw.line(g, x, y - s / 2, x, y - s / 2 - 18, new Color(0x5A4A3A), 2);
        double wave = Math.sin(time * 4) * 2;
        Path2D f = new Path2D.Double();
        f.moveTo(x, y - s / 2 - 18);
        f.lineTo(x + 16, y - s / 2 - 14 + wave);
        f.lineTo(x, y - s / 2 - 9);
        f.closePath();
        g.setColor(flag);
        g.fill(f);
    }

    private void drawRange(Graphics2D g, double x, double y, double range, Color c) {
        double r = range * TILE;
        g.setColor(Theme.alpha(c, 0.08));
        g.fill(new Ellipse2D.Double(px(x) - r, py(y) - r, r * 2, r * 2));
        Draw.ring(g, px(x), py(y), r, Theme.alpha(c, 0.55), 1.6);
    }

    private void drawPlacementGhost(Graphics2D g, int tx, int ty) {
        TowerDef def = app.content.tower(placing);
        String problem = battle.placementProblem(def, tx, ty);
        boolean rune = battle.map().tile(tx, ty) == BattleMap.Tile.RUNE;
        StatSheet stats = battle.previewStats(def, rune);
        Color c = problem == null ? Theme.GOOD : Theme.BAD;
        drawRange(g, tx + 0.5, ty + 0.5, stats.get(Stat.RANGE), c);
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));
        Art.tower(g, def, px(tx + 0.5), py(ty + 0.5), TILE, -Math.PI / 2, 1, false, time, false);
        g.setComposite(old);
        Draw.strokeRound(g, px(tx) + 2, py(ty) + 2, TILE - 4, TILE - 4, 12, c, 2);
        if (rune && problem == null) Draw.textCentered(g, "Rune: +20% range, +15% dmg", px(tx + 0.5), py(ty) - 10, Theme.bold(12), Theme.GOLD);
    }

    private void drawEnemy(Graphics2D g, Enemy e) {
        e.lane().positionAt(e.dist() + 0.08, tmp);
        double dx = tmp[0] - e.x, dy = tmp[1] - e.y, len = Math.hypot(dx, dy);
        if (len < 1e-6) {
            dx = 1;
            dy = 0;
        } else {
            dx /= len;
            dy /= len;
        }
        double r = e.radius() * TILE;
        double cx = px(e.x), cy = py(e.y);
        Art.enemy(g, e, cx, cy, r, dx, dy, time);
        if (!e.boss()) {
            double top = cy - r - (e.flying() ? r * 0.55 + 8 : 9);
            Art.healthBar(g, e, cx, top, Math.max(26, r * 2.4));
        }
    }

    private void drawProjectile(Graphics2D g, Projectile p) {
        Color c = Theme.rgb(p.source.def.color());
        double x = px(p.x), y = py(p.y);
        if (!p.homing) {
            double t = p.flightProgress();
            double h = Math.sin(Math.PI * t) * TILE * 0.9;
            Draw.circle(g, x, y, 5 * (1 - 0.3 * Math.sin(Math.PI * t)), new Color(0, 0, 0, 70));
            Draw.circle(g, x, y - h, 6.5, new Color(0x3A3A30));
            Draw.circle(g, x - 1.5, y - h - 1.5, 2, new Color(255, 255, 255, 120));
            return;
        }
        double ang = Math.atan2(py(p.ty) - y, px(p.tx) - x);
        switch (p.source.def.art()) {
            case "archer", "ballista" -> {
                double len = p.source.def.art().equals("ballista") ? 16 : 11;
                Draw.line(g, x - Math.cos(ang) * len, y - Math.sin(ang) * len, x, y, new Color(0xE9E6DF), p.source.def.art().equals("ballista") ? 3 : 2);
                if (p.crit) Draw.circle(g, x, y, 4, Theme.alpha(Theme.GOLD, 0.8));
            }
            case "cannon" -> {
                Draw.circle(g, x, y, 5.5, new Color(0x2A2C33));
                Draw.circle(g, x - 1.5, y - 1.5, 1.8, new Color(255, 255, 255, 110));
            }
            case "frost" -> {
                Shape d = Icons.starShape(x, y, 6, 2.5, 4, ang);
                g.setColor(new Color(0xDDF6FF));
                g.fill(d);
            }
            case "venom" -> {
                Draw.circle(g, x, y, 5, Theme.alpha(c, 0.9));
                Draw.circle(g, x - 1, y - 1, 2, new Color(220, 255, 200, 160));
            }
            default -> Draw.circle(g, x, y, 4.5, c);
        }
    }

    private void drawBossBar(Graphics2D g) {
        for (Enemy e : battle.enemies()) {
            if (!e.alive || !e.boss()) continue;
            double w = 520, x = BX + BW / 2.0 - w / 2, y = BY + 14;
            Draw.fillRound(g, x - 8, y - 6, w + 16, 40, 12, new Color(10, 12, 18, 200));
            Draw.textCentered(g, e.def.name() + (e.invulnerable > 0 ? "  (INVULNERABLE)" : ""), x + w / 2, y + 6, Theme.bold(13), Theme.BOSS);
            Draw.bar(g, x, y + 16, w, 10, e.hpFraction(), Theme.HP, new Color(0x2A1A22));
            Draw.textCentered(g, Math.round(e.hp) + " / " + Math.round(e.maxHp), x + w / 2, y + 21, Theme.bold(10), Color.WHITE);
            break;
        }
    }

    private void enemyTooltip(Ui ui, Enemy e) {
        List<String> lines = new ArrayList<>();
        lines.add("HP " + Math.round(e.hp) + " / " + Math.round(e.maxHp) + (e.maxShield > 0 ? "  ·  Shield " + Math.round(e.shield) : ""));
        String armor = "Armor " + Widgets.fmt(e.armor) + "  ·  Speed " + Widgets.fmt(e.baseSpeed * e.speedMult);
        lines.add(armor + (e.flying() ? "  ·  Flying" : ""));
        StringBuilder res = new StringBuilder();
        for (DamageType t : DamageType.values()) {
            double r = e.resistance(t);
            if (r != 0) res.append(t.label).append(r > 0 ? " -" : " +").append(Math.round(Math.abs(r) * 100)).append("%  ");
        }
        if (!res.isEmpty()) lines.add("Damage taken: " + res.toString().trim());
        if (!e.def.description().isEmpty()) lines.add(e.def.description());
        for (AffixDef a : e.affixes) lines.add("Elite affix · " + a.name() + ": " + a.description());
        List<String> st = new ArrayList<>();
        for (StatusType s : StatusType.values()) if (e.status.has(s)) st.add(s.label);
        if (!st.isEmpty()) lines.add("Status: " + String.join(", ", st));
        String name = (e.elite() ? "Elite " : "") + e.def.name();
        ui.tooltip(name, e.boss() ? Theme.BOSS : e.elite() ? Theme.ELITE : Theme.TEXT, lines);
    }

    // ================================================================== side panel

    private void drawSidePanel(Graphics2D g, Ui ui) {
        Draw.panel(g, SX, SY, SW, 658);
        Icons.mana(g, SX + 26, SY + 30, 28);
        Draw.textLeftMid(g, String.valueOf(battle.mana()), SX + 46, SY + 30, Theme.bold(26), Theme.MANA);
        if (ui.hover(SX + 8, SY + 10, 150, 40)) {
            ui.tooltip("Mana", Theme.MANA, List.of("Spent to build and upgrade towers in this battle.",
                    "Earned from kills, cleared waves and early calls.", "Resets every battle."));
        }
        String wave = "Wave " + Math.max(1, battle.wavesStarted()) + " / " + battle.totalWaves();
        if (battle.wavesStarted() == 0) wave = battle.totalWaves() + " waves";
        Draw.textRight(g, wave, SX + SW - 16, SY + 36, Theme.bold(17), Theme.TEXT);

        // speed controls
        double by = SY + 60;
        if (ui.button(paused ? ">" : "II", SX + 12, by, 46, 30, true, paused ? Ui.Style.PRIMARY : Ui.Style.NORMAL)) paused = !paused;
        for (int s = 1; s <= 3; s++) {
            boolean active = speed == s && !paused;
            if (ui.button(s + "x", SX + 64 + (s - 1) * 52, by, 46, 30, true, active ? Ui.Style.PRIMARY : Ui.Style.GHOST)) {
                speed = s;
                paused = false;
            }
        }
        if (ui.hover(SX + 12, by, 200, 30)) ui.tooltip("Speed", List.of("P: pause · F/Tab: cycle speed.", "Speed never changes the outcome: the simulation always steps at 60 Hz."));

        // wave button
        String label;
        boolean can = battle.canStartWave();
        if (battle.state() == Battle.State.PREP) {
            label = "Start Wave " + (battle.wavesStarted() + 1) + "   [Space]";
        } else if (battle.wavesStarted() >= battle.totalWaves()) {
            label = "Final wave in progress";
        } else if (can) {
            label = "Call Wave " + (battle.wavesStarted() + 1) + " early  (+" + (app.content.balance().earlyCallMana + battle.mods().earlyCallBonus) + ")";
        } else {
            label = "Wave " + battle.wavesStarted() + " spawning…";
        }
        if (ui.button(label, SX + 12, SY + 100, SW - 24, 44, can, Ui.Style.PRIMARY, Theme.bold(15))) startWave();

        // blueprints
        Draw.text(g, "Blueprints", SX + 14, SY + 168, Theme.bold(14), Theme.TEXT_DIM);
        if (battle.freeTowersLeft() > 0) Draw.textRight(g, "Next tower FREE", SX + SW - 14, SY + 168, Theme.bold(12), Theme.GOOD);
        double gy = SY + 178;
        for (int i = 0; i < blueprintIds.size(); i++) {
            int col = i % 3, row = i / 3;
            double cx = SX + 12 + col * (CARD_W + 8), cy = gy + row * (CARD_H + 6);
            blueprintCard(g, ui, i, blueprintIds.get(i), cx, cy);
        }
        int rows = (blueprintIds.size() + 2) / 3;
        double panelY = gy + rows * (CARD_H + 6) + 8;
        if (selected != null) selectedPanel(g, ui, panelY);
        else hintPanel(g, panelY);
    }

    private void blueprintCard(Graphics2D g, Ui ui, int index, String id, double x, double y) {
        TowerDef def = app.content.tower(id);
        int tier = battle.blueprintTier(id);
        int cost = battle.buildCost(def);
        boolean affordable = battle.mana() >= cost;
        boolean active = id.equals(placing);
        boolean hover = ui.hover(x, y, CARD_W, CARD_H);
        Color bg = active ? new Color(0x3A3320) : hover ? Theme.PANEL_LIGHT : new Color(0x181C29);
        Draw.fillRound(g, x, y, CARD_W, CARD_H, 10, bg);
        Draw.strokeRound(g, x + 0.5, y + 0.5, CARD_W - 1, CARD_H - 1, 10, active ? Theme.GOLD : Theme.rgb(def.rarity().color).darker(), active ? 2 : 1);
        Draw.text(g, Draw.ellipsize(g, def.name(), Theme.bold(11), CARD_W - 12), x + 7, y + 15, Theme.bold(11), Theme.TEXT);
        Art.tower(g, def, x + 20, y + 36, 28, -Math.PI / 4, 0, false, time, false);
        Icons.mana(g, x + 46, y + 36, 13);
        Draw.textLeftMid(g, String.valueOf(cost), x + 54, y + 36, Theme.bold(13), affordable ? Theme.MANA : Theme.BAD);
        if (index < 9) Draw.textRight(g, String.valueOf(index + 1), x + CARD_W - 6, y + 51, Theme.bold(10), Theme.TEXT_FAINT);
        for (int t = 0; t < tier; t++) Icons.star(g, x + 50 + t * 10, y + 48, 4, Theme.GOLD);
        if (!affordable) Draw.fillRound(g, x, y, CARD_W, CARD_H, 10, new Color(10, 12, 18, 110));
        if (hover) Widgets.towerTooltip(ui, app.content, def, tier);
        if (ui.clicked(x, y, CARD_W, CARD_H)) selectBlueprint(id);
    }

    private void hintPanel(Graphics2D g, double y) {
        Draw.fillRound(g, SX + 12, y, SW - 24, 700 - y, 10, new Color(0x151926));
        String[] lines = {
                "Pick a blueprint (or press 1-9), then",
                "click a tile to build. Shift-click keeps",
                "building. Right-click cancels.",
                "",
                "Click a tower to upgrade, specialize",
                "or sell it (U / S / T for target).",
                "",
                "Glowing rune tiles empower towers.",
                "Selling is free before the first wave."
        };
        for (int i = 0; i < lines.length; i++) Draw.text(g, lines[i], SX + 24, y + 24 + i * 19, Theme.font(13), Theme.TEXT_DIM);
    }

    private void selectedPanel(Graphics2D g, Ui ui, double y) {
        Tower t = selected;
        double x = SX + 12, w = SW - 24, h = 700 - y;
        Draw.fillRound(g, x, y, w, h, 10, new Color(0x151926));
        Draw.strokeRound(g, x, y, w, h, 10, Theme.BORDER, 1);
        Art.tower(g, t.def, x + 30, y + 30, 44, t.aim, t.level(), t.branch() != null, time, t.disabled() > 0);
        Draw.text(g, t.def.name() + Blueprint.tierName(battle.blueprintTier(t.def.id())), x + 60, y + 24, Theme.bold(16), Theme.TEXT);
        String sub = "Level " + t.level() + (t.branch() != null ? " · " + t.branch().name() : "") + (t.onRune ? " · Rune" : "");
        Draw.text(g, sub, x + 60, y + 44, Theme.font(13), t.branch() != null ? Theme.BOSS : Theme.TEXT_DIM);

        StatSheet s = t.stats();
        List<String> lines = new ArrayList<>();
        if (t.def.kind() == AttackKind.SUPPORT || t.providesAura()) {
            lines.add("Aura +" + Math.round(s.get(Stat.SUPPORT_DAMAGE) * 100) + "% dmg, +" + Math.round(s.get(Stat.SUPPORT_SPEED) * 100) + "% speed");
        }
        if (t.def.kind() == AttackKind.GENERATOR) {
            lines.add("+" + Math.round(s.get(Stat.INCOME)) + " mana / " + Widgets.fmt(t.effectiveCooldown()) + "s (waves only)");
        } else if (t.def.kind() != AttackKind.SUPPORT) {
            String aura = t.auraDamage() > 0 ? "  (+" + Math.round(t.auraDamage() * 100) + "% aura)" : "";
            lines.add("Damage " + Widgets.fmt(t.effectiveDamage()) + aura);
            lines.add(String.format(Locale.ROOT, "%.2f attacks/s  ·  Range %s", 1 / t.effectiveCooldown(), Widgets.fmt(t.range())));
            lines.add("≈ " + Math.round(t.estimatedDps()) + " DPS  ·  " + t.targetMode().label + " target");
        }
        for (OnHitDef o : t.onHit()) lines.add(o.describe());
        lines.add("Kills " + t.kills() + "  ·  Dealt " + Math.round(t.damageDealt()));
        double ly = y + 70;
        for (String l : lines) {
            Draw.text(g, Draw.ellipsize(g, l, Theme.font(13), w - 24), x + 12, ly, Theme.font(13), Theme.TEXT_DIM);
            ly += 18;
        }

        double btnY = Math.max(ly + 4, y + h - 132);
        if (!t.isMaxLevel()) {
            int cost = battle.upgradeCost(t);
            if (ui.button("Upgrade to Lv " + (t.level() + 1) + "   " + cost + " mana  [U]", x + 8, btnY, w - 16, 36, battle.mana() >= cost, Ui.Style.PRIMARY, Theme.bold(13))) {
                submit(new BattleCommand.Upgrade(t.id));
            }
            if (ui.hover(x + 8, btnY, w - 16, 36)) ui.tooltip("Level " + (t.level() + 1), modLines(t.def.upgrades().get(t.level() - 1).mods()));
        } else if (t.branch() == null && !t.def.branches().isEmpty()) {
            for (int i = 0; i < 2; i++) {
                TowerDef.Branch b = t.def.branches().get(i);
                int cost = battle.branchCost(t, i);
                double bx = x + 8 + i * ((w - 24) / 2 + 8), bw = (w - 24) / 2;
                if (ui.button(b.name() + " " + cost, bx, btnY, bw, 36, battle.mana() >= cost, Ui.Style.NORMAL, Theme.bold(12))) {
                    submit(new BattleCommand.ChooseBranch(t.id, i));
                }
                if (ui.hover(bx, btnY, bw, 36)) ui.tooltip(b.name(), Theme.BOSS, List.of("Specialization (choose one)", b.description()));
            }
        } else {
            Draw.textCentered(g, t.branch() != null ? "Fully specialized" : "Max level", x + w / 2, btnY + 18, Theme.bold(13), Theme.TEXT_FAINT);
        }
        double rowY = btnY + 44;
        if (t.def.kind() != AttackKind.SUPPORT && t.def.kind() != AttackKind.GENERATOR) {
            if (ui.button("Target: " + t.targetMode().label + "  [T]", x + 8, rowY, (w - 24) / 2, 34, true, Ui.Style.GHOST, Theme.bold(12))) {
                submit(new BattleCommand.SetTarget(t.id, t.targetMode().next()));
            }
        }
        int refund = battle.sellValue(t);
        if (ui.button("Sell +" + refund + "  [S]", x + 16 + (w - 24) / 2, rowY, (w - 24) / 2, 34, true, Ui.Style.DANGER, Theme.bold(12))) {
            sellSelected();
        }
    }

    private static List<String> modLines(List<StatMod> mods) {
        List<String> out = new ArrayList<>();
        for (StatMod m : mods) out.add(m.describe());
        return out;
    }

    // ================================================================== bottom strip

    private void drawBottomStrip(Graphics2D g, Ui ui) {
        double y = 664;
        double x = BX;
        for (Synergies.Status st : battle.synergies().statuses()) {
            if (st.count() == 0) continue;
            SynergyDef d = st.def();
            boolean maxed = st.tier() == d.tiers().size() - 1;
            String label = d.name() + "  " + (maxed ? st.count() + " · MAX" : st.count() + "/" + st.needed());
            double w = Draw.width(g, label, Theme.bold(12)) + 22;
            if (x + w > 600) break;
            Color c = st.active() ? Theme.GOLD : Theme.BORDER_LIGHT;
            Draw.fillRound(g, x, y, w, 26, 13, st.active() ? new Color(0x3A3320) : new Color(0x181C29));
            Draw.strokeRound(g, x, y, w, 26, 13, c, 1.2);
            Draw.textCentered(g, label, x + w / 2, y + 13, Theme.bold(12), st.active() ? Theme.GOLD : Theme.TEXT_DIM);
            if (ui.hover(x, y, w, 26)) {
                List<String> lines = new ArrayList<>();
                if (!d.requiresAll().isEmpty()) lines.add("Requires one tower of each: " + String.join(", ", d.requiresAll()));
                else lines.add("Counts towers tagged \"" + d.tag() + "\" on the field.");
                for (int i = 0; i < d.tiers().size(); i++) {
                    SynergyDef.Tier tier = d.tiers().get(i);
                    String mark = i <= st.tier() ? "+ " : "   ";
                    lines.add(mark + (d.requiresAll().isEmpty() ? "(" + tier.count() + ") " : "") + tier.description());
                }
                ui.tooltip(d.name(), Theme.GOLD, lines);
            }
            x += w + 8;
        }
        if (x == BX) Draw.textLeftMid(g, "Synergies appear as you build towers that share a tag.", BX, y + 13, Theme.font(13), Theme.TEXT_FAINT);

        // next wave preview
        double px = 640;
        List<WavePlan.Group> next = battle.nextWavePreview();
        if (next.isEmpty()) {
            Draw.textLeftMid(g, battle.state() == Battle.State.WON ? "All waves cleared!" : "Final wave!", px, y + 13, Theme.bold(14), Theme.GOLD);
            return;
        }
        Draw.textLeftMid(g, "Next:", px, y + 13, Theme.bold(13), Theme.TEXT_DIM);
        double ix = px + 46;
        for (WavePlan.Group grp : next) {
            if (ix > 890) break;
            EnemyDef d = app.content.enemy(grp.enemyId());
            Art.enemyIcon(g, d, ix + 10, y + 12, d.boss() ? 10 : 8, grp.elite());
            Draw.textLeftMid(g, "×" + grp.count(), ix + 22, y + 14, Theme.bold(12), grp.elite() ? Theme.ELITE : Theme.TEXT);
            if (ui.hover(ix, y, 44, 26)) {
                List<String> lines = new ArrayList<>();
                lines.add(Math.round(d.hp() * 1) + " base HP · speed " + Widgets.fmt(d.speed()) + (d.flying() ? " · FLYING" : ""));
                if (!d.description().isEmpty()) lines.add(d.description());
                if (grp.elite()) lines.add("Elite: has random affixes and gives double mana.");
                ui.tooltip((grp.elite() ? "Elite " : "") + d.name(), grp.elite() ? Theme.ELITE : Theme.TEXT, lines);
            }
            ix += 50;
        }
    }

    // ================================================================== overlays

    private void drawPauseMenu(Graphics2D g, Ui ui) {
        g.setColor(new Color(5, 6, 10, 170));
        g.fillRect(0, 0, 1280, 720);
        double w = 380, h = 330, x = 640 - w / 2, y = 360 - h / 2;
        Draw.panel(g, x, y, w, h);
        Draw.shadowText(g, "Paused", 640, y + 40, Theme.title(30), Theme.TEXT);
        boolean escape = ui.in.keyPressed(KeyEvent.VK_ESCAPE) && !menuJustOpened;
        menuJustOpened = false;
        if (ui.button("Resume", x + 40, y + 76, w - 80, 44, true, Ui.Style.PRIMARY) || escape) {
            menuOpen = false;
            confirmAbandon = false;
        }
        if (ui.button("How to play  [H]", x + 40, y + 130, w - 80, 44, true)) app.toggleHelp();
        if (ui.button("Save & quit to menu", x + 40, y + 184, w - 80, 44, true)) app.toMainMenu();
        if (ui.button(confirmAbandon ? "Click again to abandon" : "Abandon run", x + 40, y + 238, w - 80, 44, true, Ui.Style.DANGER)) {
            if (confirmAbandon) {
                run.abandon();
                app.showRunPhase();
            }
            confirmAbandon = true;
        }
        Draw.textCentered(g, "Quitting mid-battle restarts this battle when you continue.", 640, y + h - 18, Theme.font(12), Theme.TEXT_FAINT);
    }

    private void drawEndOverlay(Graphics2D g, Ui ui) {
        double a = Math.min(1, (endTimer - 0.8) * 3);
        g.setColor(new Color(5, 6, 10, (int) (160 * a)));
        g.fillRect(0, 0, 1280, 720);
        boolean won = battle.state() == Battle.State.WON;
        double w = 440, h = 270, x = 640 - w / 2, y = 340 - h / 2;
        Draw.panel(g, x, y, w, h, Theme.PANEL, won ? Theme.GOLD : Theme.HP);
        Draw.shadowText(g, won ? "Victory!" : "Your base has fallen", 640, y + 46, Theme.title(34), won ? Theme.GOLD : Theme.HP);
        var r = battle.result();
        int lost = battle.config().hp() - r.hp();
        Draw.textCentered(g, r.kills() + " enemies defeated  ·  " + r.leaks() + " leaked  ·  " + lost + " HP lost", 640, y + 96, Theme.font(15), Theme.TEXT_DIM);
        Draw.textCentered(g, Math.round(r.damage()) + " total damage in " + Math.round(r.ticks() * Battle.DT) + "s", 640, y + 122, Theme.font(15), Theme.TEXT_DIM);
        String label = won ? "Claim rewards" : "See results";
        if (ui.button(label, 640 - 120, y + h - 80, 240, 50, true, won ? Ui.Style.PRIMARY : Ui.Style.DANGER, Theme.bold(17))
                || ui.in.keyPressed(KeyEvent.VK_ENTER)) {
            run.finishBattle(r);
            app.showRunPhase();
        }
    }
}
