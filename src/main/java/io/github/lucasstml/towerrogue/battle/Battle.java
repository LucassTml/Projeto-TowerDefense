package io.github.lucasstml.towerrogue.battle;

import io.github.lucasstml.towerrogue.battle.ability.Abilities;
import io.github.lucasstml.towerrogue.battle.ability.EnemyAbility;
import io.github.lucasstml.towerrogue.content.ActDef;
import io.github.lucasstml.towerrogue.content.AffixDef;
import io.github.lucasstml.towerrogue.content.Balance;
import io.github.lucasstml.towerrogue.content.Content;
import io.github.lucasstml.towerrogue.content.DamageType;
import io.github.lucasstml.towerrogue.content.EffectDef;
import io.github.lucasstml.towerrogue.content.EncounterKind;
import io.github.lucasstml.towerrogue.content.EnemyDef;
import io.github.lucasstml.towerrogue.content.OnHitDef;
import io.github.lucasstml.towerrogue.content.Stat;
import io.github.lucasstml.towerrogue.content.StatMod;
import io.github.lucasstml.towerrogue.content.StatSheet;
import io.github.lucasstml.towerrogue.content.StatusType;
import io.github.lucasstml.towerrogue.content.TowerDef;
import io.github.lucasstml.towerrogue.core.EventBus;
import io.github.lucasstml.towerrogue.core.Rng;
import io.github.lucasstml.towerrogue.rules.Modifiers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * The tower-defense simulation. Pure Java with no UI dependency.
 *
 * <h2>Determinism</h2>
 * The simulation advances only through {@link #step()}, in fixed increments of {@link #DT}.
 * Every random decision comes from streams derived from the battle seed, all collections
 * are iterated in insertion order, and player input arrives as {@link BattleCommand}s
 * executed at a known tick. Since Java 17 floating point is strict on every platform, so the
 * same seed + config + command log produces a bit-identical battle on any machine
 * ({@link #stateHash()} is used to verify it).
 *
 * <h2>Speed control</h2>
 * Fast-forward calls {@code step()} several times per frame. The physics never sees a
 * bigger timestep, so 3x speed produces exactly the same outcome as 1x.
 */
public final class Battle {
    public static final double DT = 1.0 / 60.0;

    public enum State { PREP, WAVE, WON, LOST }

    private static final class ActiveWave {
        final int index;
        final WavePlan plan;
        double time;
        int next;

        ActiveWave(int index, WavePlan plan) {
            this.index = index;
            this.plan = plan;
        }

        boolean doneSpawning() {
            return next >= plan.spawns().size();
        }
    }

    private final Content content;
    private final BattleConfig cfg;
    private final ActDef act;
    private final Balance balance;
    private final Modifiers mods;
    private final Rng combatRng;
    private final EventBus bus = new EventBus();
    private final Targeting targeting = new Targeting();
    private final Synergies synergies;
    private final BattleMap map;
    private final List<WavePlan> waves;
    private final StatusEffects.Caps caps;

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Tower> towers = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Enemy> enemiesView = Collections.unmodifiableList(enemies);
    private final List<Tower> towersView = Collections.unmodifiableList(towers);
    private final List<Projectile> projectilesView = Collections.unmodifiableList(projectiles);
    private final Tower[][] grid = new Tower[BattleMap.COLS][BattleMap.ROWS];
    private final ArrayDeque<BattleCommand> pending = new ArrayDeque<>();
    private final List<BattleCommand.Logged> log = new ArrayList<>();
    private final List<ActiveWave> activeWaves = new ArrayList<>();
    private final int[] aliveByWave;
    private final boolean[] cleared;
    final double[] scratch = new double[2];

    private Consumer<Fx> fxSink = f -> { };
    private State state = State.PREP;
    private long tick;
    private int mana;
    private int hp;
    private final int maxHp;
    private int wavesStarted;
    private int wavesCleared;
    private int nextEnemyId = 1;
    private int nextTowerId = 1;
    private int freeTowersLeft;
    private int kills;
    private int leaks;
    private int elitesKilled;
    private boolean bossKilled;
    private double totalDamage;
    private String lastRejection;

    public Battle(Content content, BattleConfig cfg) {
        this.content = content;
        this.cfg = cfg;
        this.act = content.act(cfg.act());
        this.balance = content.balance();
        this.mods = cfg.mods();
        Rng mapRng = Rng.derive(cfg.seed(), "map");
        Rng waveRng = Rng.derive(cfg.seed(), "waves");
        this.combatRng = Rng.derive(cfg.seed(), "combat");
        this.map = BattleMap.generate(mapRng, mapRng.chance(act.twoLaneChance()));
        this.waves = List.copyOf(WaveDirector.plan(content, act, cfg.kind(), cfg.floor(), mods, waveRng, map.laneCount()));
        this.aliveByWave = new int[waves.size()];
        this.cleared = new boolean[waves.size()];
        this.synergies = new Synergies(content.synergies());
        this.synergies.recompute(towers);
        this.caps = new StatusEffects.Caps(balance.burnMaxStacks + mods.burnStacks,
                balance.poisonMaxStacks + mods.poisonStacks, balance.shredMaxStacks);
        this.mana = Math.max(0, act.startMana() + mods.startMana);
        this.hp = cfg.hp();
        this.maxHp = cfg.maxHp();
        this.freeTowersLeft = mods.freeTowers;
        for (BattleHook hook : cfg.hooks()) hook.attach(this);
    }

    // =================================================================== commands

    /** Queues a player command; it runs at the start of the next {@link #processCommands()}. */
    public void submit(BattleCommand command) {
        pending.add(command);
    }

    /**
     * Executes queued commands at the current tick. Called by {@link #step()} and also by the UI
     * while paused, so building during a pause works. A replay executes the logged commands
     * at the same ticks, before the step of that tick, which reproduces this exactly.
     */
    public void processCommands() {
        while (!pending.isEmpty()) {
            BattleCommand c = pending.poll();
            if (execute(c)) log.add(new BattleCommand.Logged(tick, c));
        }
    }

    private boolean execute(BattleCommand c) {
        if (state == State.WON || state == State.LOST) return false;
        return switch (c) {
            case BattleCommand.Place p -> place(p.towerId(), p.x(), p.y());
            case BattleCommand.Upgrade u -> upgrade(towerById(u.towerId()));
            case BattleCommand.ChooseBranch b -> chooseBranch(towerById(b.towerId()), b.branchIndex());
            case BattleCommand.Sell s -> sell(towerById(s.towerId()));
            case BattleCommand.SetTarget t -> {
                Tower tw = towerById(t.towerId());
                if (tw == null) yield false;
                tw.targetMode = t.mode();
                yield true;
            }
            case BattleCommand.StartWave w -> startWave();
        };
    }

    private boolean reject(String reason) {
        lastRejection = reason;
        return false;
    }

    private boolean place(String towerId, int x, int y) {
        if (!cfg.blueprints().containsKey(towerId)) return reject("Blueprint not owned");
        TowerDef def = content.tower(towerId);
        String problem = placementProblem(def, x, y);
        if (problem != null) return reject(problem);
        int cost = buildCost(def);
        mana -= cost;
        if (freeTowersLeft > 0) freeTowersLeft--;
        Tower t = new Tower(nextTowerId++, def, x, y, map.tile(x, y) == BattleMap.Tile.RUNE, cost);
        towers.add(t);
        grid[x][y] = t;
        recomputeTowers();
        emit(Fx.at(Fx.Type.SPAWN, t.x(), t.y(), 0.6, def.color()));
        bus.publish(new BattleEvents.TowerBuilt(t));
        return true;
    }

    private boolean upgrade(Tower t) {
        if (t == null || t.isMaxLevel()) return reject("Already max level");
        int cost = upgradeCost(t);
        if (mana < cost) return reject("Not enough mana");
        mana -= cost;
        t.invested += cost;
        t.level++;
        recomputeTowers();
        emit(Fx.at(Fx.Type.SPAWN, t.x(), t.y(), 0.7, 0xF2C14E));
        return true;
    }

    private boolean chooseBranch(Tower t, int index) {
        if (t == null || !t.isMaxLevel() || t.branch != null) return reject("Cannot specialize");
        if (index < 0 || index >= t.def.branches().size()) return reject("Invalid branch");
        int cost = branchCost(t, index);
        if (mana < cost) return reject("Not enough mana");
        mana -= cost;
        t.invested += cost;
        t.branch = t.def.branches().get(index);
        recomputeTowers();
        emit(Fx.at(Fx.Type.SPAWN, t.x(), t.y(), 0.9, 0xC56CF0));
        emit(Fx.text(t.x(), t.y() - 0.6, t.branch.name(), 0xF2C14E));
        return true;
    }

    private boolean sell(Tower t) {
        if (t == null) return reject("No tower");
        int refund = sellValue(t);
        mana += refund;
        towers.remove(t);
        grid[t.cx][t.cy] = null;
        recomputeTowers();
        emit(Fx.text(t.x(), t.y() - 0.3, "+" + refund, 0x5AB0FF));
        return true;
    }

    private boolean startWave() {
        if (!canStartWave()) return reject("Wave still spawning");
        boolean early = state == State.WAVE;
        if (early) {
            int bonus = balance.earlyCallMana + mods.earlyCallBonus;
            mana += bonus;
            emit(Fx.text(map.baseX() + 0.5, map.baseY() - 0.2, "Early call +" + bonus, 0x5AB0FF));
        }
        int index = wavesStarted++;
        activeWaves.add(new ActiveWave(index, waves.get(index)));
        state = State.WAVE;
        bus.publish(new BattleEvents.WaveStarted(index, early));
        return true;
    }

    // =================================================================== queries

    public String placementProblem(TowerDef def, int x, int y) {
        if (!map.buildable(x, y)) return "Can't build there";
        if (grid[x][y] != null) return "Tile occupied";
        if (mana < buildCost(def)) return "Not enough mana";
        return null;
    }

    public int buildCost(TowerDef def) {
        if (freeTowersLeft > 0) return 0;
        return (int) Math.round(def.cost() * mods.towerCostMult());
    }

    public int upgradeCost(Tower t) {
        return (int) Math.round(t.def.upgrades().get(t.level - 1).cost() * mods.towerCostMult());
    }

    public int branchCost(Tower t, int index) {
        return (int) Math.round(t.def.branches().get(index).cost() * mods.towerCostMult());
    }

    /** Full refund before the first wave (free re-planning), partial afterwards. */
    public int sellValue(Tower t) {
        if (wavesStarted == 0) return t.invested;
        double pct = mods.sellRefund >= 0 ? mods.sellRefund : balance.sellRefund;
        return (int) Math.floor(t.invested * pct);
    }

    public boolean canStartWave() {
        if (state == State.WON || state == State.LOST || wavesStarted >= waves.size()) return false;
        return activeWaves.isEmpty() || activeWaves.get(activeWaves.size() - 1).doneSpawning();
    }

    public List<WavePlan.Group> nextWavePreview() {
        return wavesStarted < waves.size() ? waves.get(wavesStarted).preview() : List.of();
    }

    public Tower towerAt(int x, int y) {
        return BattleMap.inBounds(x, y) ? grid[x][y] : null;
    }

    public Tower towerById(int id) {
        for (Tower t : towers) if (t.id == id) return t;
        return null;
    }

    public int blueprintTier(String towerId) {
        return cfg.blueprints().getOrDefault(towerId, 0);
    }

    /** Stats a tower of this type would have if built now (for tooltips). */
    public StatSheet previewStats(TowerDef def, boolean onRune) {
        return StatSheet.compute(def.base(), collectMods(def, 1, null, onRune));
    }

    // =================================================================== simulation

    /** Advances the simulation by exactly one fixed tick. */
    public void step() {
        processCommands();
        if (state == State.WON || state == State.LOST) return;
        tick++;
        updateWaves();
        updateAuras();
        for (int i = 0; i < enemies.size(); i++) enemies.get(i).update(this, DT);
        for (int i = 0; i < towers.size(); i++) towers.get(i).update(this, DT);
        for (int i = 0; i < projectiles.size(); i++) projectiles.get(i).update(this, DT);
        enemies.removeIf(e -> !e.alive);
        projectiles.removeIf(p -> !p.alive);
        checkWaves();
    }

    private void updateWaves() {
        for (int i = 0; i < activeWaves.size(); i++) {
            ActiveWave aw = activeWaves.get(i);
            aw.time += DT;
            List<WavePlan.Spawn> spawns = aw.plan.spawns();
            while (aw.next < spawns.size() && spawns.get(aw.next).time() <= aw.time) {
                WavePlan.Spawn s = spawns.get(aw.next++);
                List<AffixDef> affixes = new ArrayList<>();
                for (String a : s.affixes()) affixes.add(content.affix(a));
                spawn(content.enemy(s.enemyId()), s.lane(), 0, affixes, s.hpScale(), aw.index);
            }
        }
    }

    private void updateAuras() {
        for (Tower t : towers) {
            t.auraDamage = 0;
            t.auraSpeed = 0;
        }
        for (Tower s : towers) {
            if (!s.providesAura() || s.disabled > 0) continue;
            double r2 = s.range() * s.range();
            double dmg = s.stats.get(Stat.SUPPORT_DAMAGE), spd = s.stats.get(Stat.SUPPORT_SPEED);
            for (Tower t : towers) {
                if (t == s) continue;
                double dx = t.x() - s.x(), dy = t.y() - s.y();
                if (dx * dx + dy * dy <= r2) {
                    t.auraDamage += dmg;
                    t.auraSpeed += spd;
                }
            }
        }
    }

    private void checkWaves() {
        for (ActiveWave aw : activeWaves) {
            if (!cleared[aw.index] && aw.doneSpawning() && aliveByWave[aw.index] <= 0) {
                cleared[aw.index] = true;
                wavesCleared++;
                int bonus = (int) Math.round((balance.waveClearMana + balance.waveClearManaPerWave * aw.index) * act.manaMult());
                mana += bonus;
                emit(Fx.text(map.baseX() + 0.5, map.baseY() - 0.4, "Wave " + (aw.index + 1) + " cleared  +" + bonus, 0x5AB0FF));
                bus.publish(new BattleEvents.WaveCleared(aw.index));
            }
        }
        activeWaves.removeIf(aw -> cleared[aw.index]);
        if (state == State.LOST) return;
        if (activeWaves.isEmpty()) state = wavesStarted >= waves.size() ? State.WON : State.PREP;
    }

    Enemy spawn(EnemyDef def, int lane, double dist, List<AffixDef> affixes, double hpScale, int wave) {
        Enemy e = new Enemy(nextEnemyId++, def, affixes, wave, hpScale);
        int li = Math.min(lane, map.laneCount() - 1);
        e.laneIndex = li;
        e.lane = def.flying() ? map.airLanes().get(li) : map.groundLanes().get(li);
        double hpMult = 1, speedMult = 1, armorAdd = 0;
        for (AffixDef a : affixes) {
            hpMult *= a.hpMult();
            speedMult *= a.speedMult();
            armorAdd += a.armorAdd();
        }
        e.maxHp = e.hp = def.hp() * hpScale * hpMult;
        e.armor = def.armor() + armorAdd;
        e.baseSpeed = def.speed() * speedMult * Math.max(0.2, 1 + mods.enemySpeedPct);
        e.currentSpeed = e.baseSpeed;
        e.setDist(dist);
        e.lane.positionAt(e.dist, scratch);
        e.x = scratch[0];
        e.y = scratch[1];
        for (EffectDef ad : def.abilities()) e.abilities.add(Abilities.create(ad));
        for (AffixDef a : affixes) for (EffectDef ad : a.abilities()) e.abilities.add(Abilities.create(ad));
        enemies.add(e);
        aliveByWave[wave]++;
        for (EnemyAbility ab : e.abilities) ab.onSpawn(this, e);
        bus.publish(new BattleEvents.EnemySpawned(e));
        return e;
    }

    /** Spawns minions at the parent's position; they belong to the parent's wave. */
    public void summon(Enemy parent, String enemyId, int count) {
        EnemyDef def = content.enemy(enemyId);
        for (int i = 0; i < count; i++) {
            double d;
            if (def.flying() == parent.flying()) {
                d = parent.dist - 0.25 * i;
            } else if (def.flying()) {
                d = parent.progress() * map.airLanes().get(parent.laneIndex).length();
            } else {
                d = map.groundLanes().get(parent.laneIndex).closestDistance(parent.x, parent.y) - 0.25 * i;
            }
            spawn(def, parent.laneIndex, Math.max(0, d), List.of(), parent.scale, parent.wave);
        }
        emit(Fx.at(Fx.Type.SPAWN, parent.x, parent.y, 0.8, def.color()));
    }

    void fireProjectile(Tower t, Enemy target) {
        double dmg = t.effectiveDamage();
        boolean crit = combatRng.chance(t.stats.get(Stat.CRIT_CHANCE));
        if (crit) dmg *= t.stats.get(Stat.CRIT_MULT);
        double speed = t.stats.get(Stat.PROJECTILE_SPEED);
        double tx = target.x, ty = target.y;
        if (!t.def.homing()) {
            // Lead the target: predict where it will be when the shell lands (2 refinements).
            double flight = Math.hypot(target.x - t.x(), target.y - t.y()) / speed;
            for (int i = 0; i < 2; i++) {
                target.predict(target.currentSpeed * flight, scratch);
                flight = Math.hypot(scratch[0] - t.x(), scratch[1] - t.y()) / speed;
            }
            tx = scratch[0];
            ty = scratch[1];
        }
        projectiles.add(new Projectile(t, target, dmg, crit, speed, t.stats.get(Stat.SPLASH), t.def.homing(), tx, ty));
    }

    /** Instant hit (chain/pulse): rolls crit here. */
    void strike(Tower src, Enemy e, double dmg) {
        boolean crit = combatRng.chance(src.stats.get(Stat.CRIT_CHANCE));
        if (crit) dmg *= src.stats.get(Stat.CRIT_MULT);
        strike(src, e, dmg, crit, src.onHit);
    }

    void strike(Tower src, Enemy e, double dmg, boolean crit, List<OnHitDef> onHit) {
        if (!e.alive) return;
        Hit h = new Hit(src, e, src.def.damageType(), dmg);
        h.crit = crit;
        h.armorPen = src.stats.get(Stat.ARMOR_PEN);
        damage(h);
        if (!e.alive) return;
        for (OnHitDef o : onHit) {
            if (o.chance() >= 1 || combatRng.chance(o.chance())) {
                applyStatus(e, o.status(), o.magnitude(), o.duration(), src);
            }
        }
    }

    /**
     * The damage pipeline: relic multipliers (via the bus) → Shatter reaction → armor or
     * resistance → Vulnerable → shield → HP (respecting boss phase gates) → death.
     *
     * @return damage actually removed from shield + HP
     */
    public double damage(Hit h) {
        Enemy e = h.target;
        if (!e.alive) return 0;
        if (e.invulnerable > 0) {
            if (!h.dot && combatRng.chance(0.15)) emit(Fx.text(e.x, e.y - 0.5, "IMMUNE", 0xBBBBBB));
            return 0;
        }
        bus.publish(h);
        double amt = h.amount * h.multiplier;
        if (!h.dot && h.type == DamageType.PHYSICAL && e.status.frozen()) {
            amt *= 2;
            e.status.clearFreeze();
            emit(Fx.at(Fx.Type.SHATTER, e.x, e.y, e.radius(), 0xBFF0FF));
            emit(Fx.text(e.x, e.y - 0.6, "SHATTER", 0xBFF0FF));
        }
        if (h.type == DamageType.PHYSICAL && !h.ignoreArmor) {
            double armor = Math.max(0, e.armor - e.status.armorReduction() - h.armorPen);
            amt = Math.max(amt * 0.25, amt - armor);
        }
        amt *= 1 - e.resistance(h.type);
        amt *= 1 + e.status.vulnerability();
        if (amt <= 0) return 0;

        double absorbed = 0;
        if (e.shield > 0) {
            absorbed = Math.min(e.shield, amt);
            e.shield -= absorbed;
            amt -= absorbed;
        }
        e.timeSinceHit = 0;
        double before = e.hp;
        e.hp -= amt;
        if (e.hpFloor > 0 && e.hp < e.hpFloor) e.hp = e.hpFloor;
        double dealt = (before - Math.max(0, e.hp)) + absorbed;
        totalDamage += dealt;
        if (h.source != null) h.source.damageDealt += dealt;
        emit(new Fx(h.crit ? Fx.Type.CRIT : Fx.Type.HIT, e.x, e.y, e.x, e.y, dealt,
                h.type.color, h.dot ? "dot" : null));
        if (e.hp <= 0) kill(e, h.source);
        return dealt;
    }

    public void applyStatus(Enemy e, StatusType type, double magnitude, double duration, Tower source) {
        if (!e.alive) return;
        double res = e.statusResistance(type);
        if (res >= 1) return;
        double f = 1 - res;
        double mag = type == StatusType.FREEZE ? magnitude : magnitude * f;
        e.status.apply(type, mag, duration * f, source, caps);
    }

    private void kill(Enemy e, Tower killer) {
        for (EnemyAbility ab : e.abilities) {
            if (ab.preventDeath(this, e)) return;
        }
        e.alive = false;
        e.hp = 0;
        aliveByWave[e.wave]--;
        kills++;
        if (e.elite()) elitesKilled++;
        if (e.boss()) bossKilled = true;
        if (killer != null) killer.kills++;
        int reward = (int) Math.round(e.def.mana() * (e.elite() ? 2 : 1) * act.manaMult()) + mods.killMana;
        if (reward > 0) mana += reward;
        emit(Fx.at(Fx.Type.DEATH, e.x, e.y, e.radius(), e.def.color()));
        if (reward > 0) emit(Fx.at(Fx.Type.MANA, e.x, e.y, reward, 0x5AB0FF));
        bus.publish(new BattleEvents.EnemyKilled(e, killer));
        for (EnemyAbility ab : e.abilities) ab.onDeath(this, e);
    }

    /**
     * An enemy reached the base. Bosses are not removed: they deal heavy damage and start
     * another lap, so a boss fight is lost by attrition rather than by a single mistake.
     */
    void leak(Enemy e) {
        if (!e.alive) return;
        leaks++;
        int dmg = Math.max(1, e.def.leak() + (e.elite() ? 1 : 0) + mods.leakDamage);
        BattleEvents.EnemyLeaked ev = new BattleEvents.EnemyLeaked(e, dmg);
        bus.publish(ev);
        hp = Math.max(0, hp - ev.damage);
        emit(Fx.at(Fx.Type.LEAK, e.x, e.y, ev.damage, 0xE05561));
        if (e.boss()) {
            e.dist = 0;
            e.lane.positionAt(0, scratch);
            e.x = scratch[0];
            e.y = scratch[1];
            emit(Fx.text(e.x + 1.2, e.y, e.def.name() + " circles back!", 0xE05561));
        } else {
            e.alive = false;
            e.leaked = true;
            aliveByWave[e.wave]--;
        }
        if (hp <= 0) state = State.LOST;
    }

    public void disableTower(Tower t, double duration) {
        t.disabled = Math.max(t.disabled, duration);
    }

    public void addMana(int amount) {
        mana = Math.max(0, mana + amount);
    }

    public void emit(Fx fx) {
        fxSink.accept(fx);
    }

    // =================================================================== stat computation

    private void recomputeTowers() {
        synergies.recompute(towers);
        for (Tower t : towers) computeStats(t);
    }

    private List<StatMod> collectMods(TowerDef def, int level, TowerDef.Branch branch, boolean onRune) {
        List<StatMod> m = new ArrayList<>();
        for (int i = 0; i < level - 1; i++) m.addAll(def.upgrades().get(i).mods());
        if (branch != null) m.addAll(branch.mods());
        int tier = Math.min(blueprintTier(def.id()), balance.blueprintTiers.size());
        for (int k = 0; k < tier; k++) m.addAll(balance.blueprintTiers.get(k));
        if (onRune) m.addAll(balance.runeTileMods);
        m.addAll(mods.towerModsFor(def));
        m.addAll(synergies.modsFor(def));
        return m;
    }

    private void computeStats(Tower t) {
        t.stats = StatSheet.compute(t.def.base(), collectMods(t.def, t.level, t.branch, t.onRune));
        List<OnHitDef> raw = new ArrayList<>(t.def.onHit());
        if (t.branch != null) raw.addAll(t.branch.onHit());
        double power = t.stats.get(Stat.STATUS_POWER);
        double duration = t.stats.get(Stat.STATUS_DURATION);
        double chance = t.stats.get(Stat.STATUS_CHANCE);
        List<OnHitDef> out = new ArrayList<>(raw.size());
        for (OnHitDef o : raw) {
            double c = o.chance() < 1 ? Math.min(1, o.chance() + chance) : 1;
            out.add(new OnHitDef(o.status(), o.magnitude() * power, o.duration() * duration, c));
        }
        t.onHit = List.copyOf(out);
    }

    // =================================================================== results & verification

    public BattleResult result() {
        return new BattleResult(state == State.WON, hp, kills, leaks, totalDamage, elitesKilled, bossKilled,
                wavesCleared, tick);
    }

    /** Hash of the observable simulation state; equal hashes after a replay prove determinism. */
    public long stateHash() {
        long h = 0x84222325CBF29CE4L;
        h = mixIn(h, tick);
        h = mixIn(h, mana);
        h = mixIn(h, hp);
        h = mixIn(h, state.ordinal());
        h = mixIn(h, kills);
        h = mixIn(h, Double.doubleToLongBits(totalDamage));
        for (Enemy e : enemies) {
            h = mixIn(h, e.id);
            h = mixIn(h, Double.doubleToLongBits(e.hp));
            h = mixIn(h, Double.doubleToLongBits(e.dist));
        }
        for (Tower t : towers) {
            h = mixIn(h, t.id);
            h = mixIn(h, t.level);
            h = mixIn(h, Double.doubleToLongBits(t.damageDealt));
        }
        return h;
    }

    private static long mixIn(long h, long v) {
        return Rng.mix(h ^ (v + 0x9E3779B97F4A7C15L + (h << 6) + (h >>> 2)));
    }

    // =================================================================== accessors

    public Content content() { return content; }
    public BattleConfig config() { return cfg; }
    public ActDef act() { return act; }
    public EncounterKind kind() { return cfg.kind(); }
    public BattleMap map() { return map; }
    public State state() { return state; }
    public long tick() { return tick; }
    public double time() { return tick * DT; }
    public int mana() { return mana; }
    public int hp() { return hp; }
    public int maxHp() { return maxHp; }
    public int wavesStarted() { return wavesStarted; }
    public int wavesCleared() { return wavesCleared; }
    public int totalWaves() { return waves.size(); }
    public List<WavePlan> waves() { return waves; }
    public List<Enemy> enemies() { return enemiesView; }
    public List<Tower> towers() { return towersView; }
    public List<Projectile> projectiles() { return projectilesView; }
    public Synergies synergies() { return synergies; }
    public EventBus bus() { return bus; }
    public Targeting targeting() { return targeting; }
    public Rng combatRng() { return combatRng; }
    public Modifiers mods() { return mods; }
    public List<BattleCommand.Logged> commandLog() { return Collections.unmodifiableList(log); }
    public int freeTowersLeft() { return freeTowersLeft; }
    public String lastRejection() { return lastRejection; }
    public void clearRejection() { lastRejection = null; }

    public void setFxSink(Consumer<Fx> sink) {
        this.fxSink = sink == null ? f -> { } : sink;
    }
}
