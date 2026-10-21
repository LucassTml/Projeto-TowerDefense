package io.github.lucasstml.towerrogue.run;

import io.github.lucasstml.towerrogue.battle.BattleConfig;
import io.github.lucasstml.towerrogue.battle.BattleResult;
import io.github.lucasstml.towerrogue.content.Balance;
import io.github.lucasstml.towerrogue.content.Content;
import io.github.lucasstml.towerrogue.content.EncounterKind;
import io.github.lucasstml.towerrogue.content.EventDef;
import io.github.lucasstml.towerrogue.content.NodeType;
import io.github.lucasstml.towerrogue.content.PerkDef;
import io.github.lucasstml.towerrogue.content.Rarity;
import io.github.lucasstml.towerrogue.core.Rng;
import io.github.lucasstml.towerrogue.relic.RelicEffects;
import io.github.lucasstml.towerrogue.rules.Modifiers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Game logic of a run: a state machine over {@link RunPhase}. Screens call these methods;
 * nothing here knows about Swing or files. After every transition {@link #onChange} fires,
 * which the application uses to autosave.
 */
public final class RunController {
    public static final String BLESSING_EVENT = "blessing";
    private static final double[] RELIC_WEIGHTS = {50, 35, 15};

    private final Content content;
    private final Balance balance;
    private final RunState run;
    private RunMap map;
    private Modifiers mods;
    private Runnable onChange = () -> { };

    public RunController(Content content, RunState run) {
        this.content = content;
        this.balance = content.balance();
        this.run = run;
        regenerateMap();
        rebuildModifiers();
    }

    /** Builds a fresh run. Unlock pools and perks are passed in so this class stays free of meta code. */
    public static RunState create(Content content, long seed, int ascension, List<String> towerPool,
                                  List<String> relicPool, Map<String, Integer> perks) {
        Balance b = content.balance();
        RunState r = new RunState(seed);
        r.ascension = ascension;
        for (String id : towerPool) if (!b.starterTowers.contains(id)) r.towerPool.add(id);
        r.relicPool.addAll(relicPool);
        r.perks.putAll(perks);
        Modifiers asc = new Modifiers();
        asc.applyAscension(b, ascension);
        r.maxHp = Math.max(10, b.startHp + asc.maxHpBonus + (int) perkTotal(content, perks, "maxHp"));
        r.hp = r.maxHp;
        r.gold = b.startGold + (int) perkTotal(content, perks, "startGold");
        for (String id : b.starterTowers) r.blueprints.add(new Blueprint(id, 0));
        r.phase = RunPhase.EVENT;
        r.eventId = BLESSING_EVENT;
        RunController ctl = new RunController(content, r);
        if (perkTotal(content, perks, "startRelic") > 0) ctl.gainRandomRelic(Rarity.COMMON);
        return r;
    }

    static double perkTotal(Content content, Map<String, Integer> perks, String effect) {
        double total = 0;
        for (Map.Entry<String, Integer> e : perks.entrySet()) {
            PerkDef p = content.perk(e.getKey());
            if (p.effect().equals(effect)) total += p.perLevel() * e.getValue();
        }
        return total;
    }

    private void regenerateMap() {
        map = RunMapGenerator.generate(Rng.derive(run.seed, "map:" + run.act), content.act(run.act).floors());
    }

    /** Recomputes all rule modifiers from Ascension + perks + relics. */
    private void rebuildModifiers() {
        Modifiers m = new Modifiers();
        m.applyAscension(balance, run.ascension);
        m.startMana += (int) perkTotal(content, run.perks, "startMana");
        m.rareChance += perkTotal(content, run.perks, "rareChance");
        m.restHealPct += perkTotal(content, run.perks, "restHeal");
        m.draftChoices += (int) perkTotal(content, run.perks, "draftChoices");
        RelicEffects.contributeAll(content, run.relics, m);
        mods = m;
    }

    private void changed() {
        onChange.run();
    }

    // ================================================================== map

    public List<RunMap.Node> reachableNodes() {
        List<RunMap.Node> out = new ArrayList<>();
        if (run.phase != RunPhase.MAP) return out;
        if (run.currentNode < 0) {
            for (RunMap.Node n : map.nodes()) if (n.floor == 0) out.add(n);
        } else {
            for (int id : map.node(run.currentNode).next) out.add(map.node(id));
        }
        return out;
    }

    public boolean chooseNode(int id) {
        boolean ok = false;
        for (RunMap.Node n : reachableNodes()) if (n.id == id) ok = true;
        if (!ok) return false;
        run.currentNode = id;
        run.visited.add(id);
        RunMap.Node n = map.node(id);
        switch (n.type) {
            case COMBAT -> startBattle(EncounterKind.NORMAL);
            case ELITE -> startBattle(EncounterKind.ELITE);
            case BOSS -> startBattle(EncounterKind.BOSS);
            case EVENT -> startEvent();
            case SHOP -> {
                run.shop = generateShop();
                run.phase = RunPhase.SHOP;
            }
            case REST -> run.phase = RunPhase.REST;
            case TREASURE -> {
                run.rewards.clear();
                int gold = (int) Math.round(run.rewardRng.range(20, 35) * mods.goldMult());
                run.gold += gold;
                run.lastGoldReward = gold;
                addRelicReward(Loot.rollRarity(run.rewardRng, RELIC_WEIGHTS));
                run.phase = RunPhase.REWARD;
            }
        }
        changed();
        return true;
    }

    private void startBattle(EncounterKind kind) {
        run.battleKind = kind;
        run.phase = RunPhase.BATTLE;
    }

    private void startEvent() {
        List<EventDef> pool = new ArrayList<>();
        for (EventDef e : content.events()) {
            if (e.special() || run.seenEvents.contains(e.id())) continue;
            if (e.acts().isEmpty() || e.acts().contains(run.act)) pool.add(e);
        }
        if (pool.isEmpty()) {
            for (EventDef e : content.events()) if (!e.special()) pool.add(e);
        }
        EventDef chosen = run.eventRng.pick(pool);
        run.seenEvents.add(chosen.id());
        run.eventId = chosen.id();
        run.eventResolved = false;
        run.eventBattlePending = false;
        run.eventLog.clear();
        run.phase = RunPhase.EVENT;
    }

    // ================================================================== battle

    /** Battle seed depends on run seed, act, node and a counter, so a reloaded battle is identical. */
    public BattleConfig battleConfig() {
        long seed = Rng.deriveSeed(run.seed, "battle:" + run.act + ":" + run.currentNode + ":" + run.battleCounter);
        Map<String, Integer> bp = new LinkedHashMap<>();
        for (Blueprint b : run.blueprints) bp.put(b.towerId, b.tier);
        int floor = run.currentNode >= 0 ? map.node(run.currentNode).floor : 0;
        return new BattleConfig(seed, run.act, run.battleKind, floor, bp, mods,
                RelicEffects.hooksFor(content, run.relics), run.hp, run.maxHp);
    }

    public void finishBattle(BattleResult r) {
        if (run.phase != RunPhase.BATTLE) return;
        run.battleCounter++;
        run.kills += r.kills();
        run.leaks += r.leaks();
        run.damage += (long) r.damage();
        run.elitesKilled += r.elitesKilled();
        run.hp = r.hp();
        if (!r.won()) {
            run.hp = 0;
            run.phase = RunPhase.DEFEAT;
            changed();
            return;
        }
        if (run.battleKind == EncounterKind.BOSS) run.bossesKilled++;
        run.hp = Math.min(run.maxHp, run.hp + mods.healAfterCombat);
        generateRewards(run.battleKind);
        run.phase = RunPhase.REWARD;
        changed();
    }

    // ================================================================== rewards

    private void generateRewards(EncounterKind kind) {
        run.rewards.clear();
        int[] g = balance.goldReward.get(kind);
        int gold = (int) Math.round(run.rewardRng.range(g[0], g[1]) * mods.goldMult());
        run.gold += gold;
        run.lastGoldReward = gold;
        draft(kind);
        if (kind == EncounterKind.ELITE) {
            addRelicReward(Loot.rollRarity(run.rewardRng, RELIC_WEIGHTS));
        } else if (kind == EncounterKind.BOSS) {
            Set<String> offered = new HashSet<>();
            for (int i = 0; i < 3; i++) {
                String id = Loot.pickRelic(run, content, Rarity.BOSS, offered, run.rewardRng);
                if (id == null) break;
                offered.add(id);
                run.rewards.add(new RewardOption(RewardOption.Kind.RELIC, id, 0, RewardOption.GROUP_RELIC,
                        content.relic(id).rarity()));
            }
        }
    }

    private void addRelicReward(Rarity rarity) {
        String id = Loot.pickRelic(run, content, rarity, Set.of(), run.rewardRng);
        if (id != null) {
            run.rewards.add(new RewardOption(RewardOption.Kind.RELIC, id, 0, RewardOption.GROUP_RELIC,
                    content.relic(id).rarity()));
        } else {
            run.rewards.add(new RewardOption(RewardOption.Kind.GOLD, "gold", 50, RewardOption.GROUP_RELIC, Rarity.COMMON));
        }
    }

    /** "Pick 1 of N" tower draft with rarity weights and bad-luck protection for rares. */
    private void draft(EncounterKind kind) {
        int n = balance.draftChoices + mods.draftChoices;
        double[] w = balance.rarityWeights.get(kind).clone();
        w[2] += run.rarePity + mods.rareChance;
        Set<String> offered = new HashSet<>();
        Set<String> upgradesOffered = new HashSet<>();
        boolean rareOffered = false;
        for (int i = 0; i < n; i++) {
            List<Blueprint> up = new ArrayList<>();
            for (Blueprint b : run.upgradeableBlueprints()) if (!upgradesOffered.contains(b.towerId)) up.add(b);
            if (!up.isEmpty() && upgradesOffered.isEmpty() && run.rewardRng.chance(balance.upgradeOfferChance)) {
                addUpgradeOption(run.rewardRng.pick(up), upgradesOffered);
                continue;
            }
            Rarity r = Loot.rollRarity(run.rewardRng, w);
            String id = Loot.pickTower(run, content, r, offered, run.rewardRng);
            if (id != null) {
                offered.add(id);
                Rarity actual = content.tower(id).rarity();
                if (actual == Rarity.RARE) rareOffered = true;
                run.rewards.add(new RewardOption(RewardOption.Kind.NEW_TOWER, id, 0, RewardOption.GROUP_DRAFT, actual));
            } else if (!up.isEmpty()) {
                addUpgradeOption(run.rewardRng.pick(up), upgradesOffered);
            } else {
                run.rewards.add(new RewardOption(RewardOption.Kind.GOLD, "gold", 25, RewardOption.GROUP_DRAFT, Rarity.COMMON));
            }
        }
        run.rarePity = rareOffered ? 0 : run.rarePity + balance.rarePityStep;
    }

    private void addUpgradeOption(Blueprint b, Set<String> upgradesOffered) {
        upgradesOffered.add(b.towerId);
        run.rewards.add(new RewardOption(RewardOption.Kind.UPGRADE_TOWER, b.towerId, b.tier + 1,
                RewardOption.GROUP_DRAFT, content.tower(b.towerId).rarity()));
    }

    public boolean groupResolved(int group) {
        for (RewardOption o : run.rewards) if (o.group == group && o.taken) return true;
        return false;
    }

    public boolean takeReward(int index) {
        if (run.phase != RunPhase.REWARD || index < 0 || index >= run.rewards.size()) return false;
        RewardOption o = run.rewards.get(index);
        if (o.taken || groupResolved(o.group)) return false;
        switch (o.kind) {
            case NEW_TOWER -> run.blueprints.add(new Blueprint(o.id, 0));
            case UPGRADE_TOWER -> {
                Blueprint b = run.blueprint(o.id);
                if (b != null && b.canUpgrade()) b.tier++;
            }
            case RELIC -> gainRelic(o.id);
            case GOLD -> run.gold += o.amount;
        }
        o.taken = true;
        changed();
        return true;
    }

    public void continueFromRewards() {
        if (run.phase == RunPhase.REWARD) finishNode();
    }

    // ================================================================== shop

    private ShopStock generateShop() {
        ShopStock s = new ShopStock();
        double pm = mods.shopPriceMult();
        Set<String> ex = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            String id = Loot.pickTower(run, content, Loot.rollRarity(run.shopRng, new double[]{50, 35, 15}), ex, run.shopRng);
            if (id == null) break;
            ex.add(id);
            int base = balance.towerPrice.getOrDefault(content.tower(id).rarity(), 100);
            s.items.add(new ShopStock.Item(ShopStock.Kind.TOWER, id, (int) Math.round(base * pm * run.shopRng.range(0.9, 1.1))));
        }
        for (int i = 0; i < 3; i++) {
            String id = Loot.pickRelic(run, content, Loot.rollRarity(run.shopRng, new double[]{45, 38, 17}), ex, run.shopRng);
            if (id == null) break;
            ex.add(id);
            int base = balance.relicPrice.getOrDefault(content.relic(id).rarity(), 200);
            s.items.add(new ShopStock.Item(ShopStock.Kind.RELIC, id, (int) Math.round(base * pm * run.shopRng.range(0.9, 1.1))));
        }
        s.forgePrice = (int) Math.round((balance.forgePriceBase + run.forgeUses * balance.forgePriceStep) * pm);
        s.healPrice = (int) Math.round(balance.healPrice * pm);
        return s;
    }

    public boolean buyItem(int index) {
        ShopStock s = run.shop;
        if (run.phase != RunPhase.SHOP || s == null || index < 0 || index >= s.items.size()) return false;
        ShopStock.Item it = s.items.get(index);
        if (it.sold || run.gold < it.price) return false;
        run.gold -= it.price;
        it.sold = true;
        if (it.kind == ShopStock.Kind.TOWER) run.blueprints.add(new Blueprint(it.id, 0));
        else gainRelic(it.id);
        changed();
        return true;
    }

    public boolean buyForge(String towerId) {
        ShopStock s = run.shop;
        Blueprint b = run.blueprint(towerId);
        if (run.phase != RunPhase.SHOP || s == null || s.forgeUsed || b == null || !b.canUpgrade()) return false;
        if (run.gold < s.forgePrice) return false;
        run.gold -= s.forgePrice;
        b.tier++;
        run.forgeUses++;
        s.forgeUsed = true;
        changed();
        return true;
    }

    public boolean buyHeal() {
        ShopStock s = run.shop;
        if (run.phase != RunPhase.SHOP || s == null || s.healUsed || run.gold < s.healPrice || run.hp >= run.maxHp) return false;
        run.gold -= s.healPrice;
        heal(balance.healServicePct);
        s.healUsed = true;
        changed();
        return true;
    }

    public void leaveShop() {
        if (run.phase == RunPhase.SHOP) finishNode();
    }

    // ================================================================== rest

    public int restHealAmount() {
        return (int) Math.round(run.maxHp * Math.max(0, balance.restHealPct + mods.restHealPct));
    }

    public void rest() {
        if (run.phase != RunPhase.REST) return;
        run.hp = Math.min(run.maxHp, run.hp + restHealAmount());
        finishNode();
    }

    public boolean restForge(String towerId) {
        Blueprint b = run.blueprint(towerId);
        if (run.phase != RunPhase.REST || b == null || !b.canUpgrade()) return false;
        b.tier++;
        finishNode();
        return true;
    }

    // ================================================================== events

    public EventDef currentEvent() {
        return run.eventId == null ? null : content.event(run.eventId);
    }

    public boolean choiceAvailable(EventDef.Choice c) {
        return run.gold >= c.requires().getInt("gold", 0) && run.hp >= c.requires().getInt("hp", 0);
    }

    public void chooseEvent(int index) {
        EventDef e = currentEvent();
        if (run.phase != RunPhase.EVENT || e == null || run.eventResolved) return;
        if (index < 0 || index >= e.choices().size()) return;
        EventDef.Choice c = e.choices().get(index);
        if (!choiceAvailable(c)) return;
        run.eventLog.clear();
        EventEngine.resolve(this, c.outcomes(), run.eventLog);
        if (run.eventLog.isEmpty()) run.eventLog.add("You move on.");
        run.eventResolved = true;
        changed();
    }

    public void continueEvent() {
        if (run.phase != RunPhase.EVENT || !run.eventResolved) return;
        if (run.eventBattlePending) {
            run.eventBattlePending = false;
            startBattle(run.battleKind);
            changed();
        } else {
            finishNode();
        }
    }

    // ================================================================== progression

    private void finishNode() {
        run.rewards.clear();
        run.shop = null;
        run.eventResolved = false;
        run.eventLog.clear();
        if (run.currentNode < 0) {
            run.eventId = null;
            run.phase = RunPhase.MAP;
            changed();
            return;
        }
        run.eventId = null;
        run.floorsCleared++;
        RunMap.Node n = map.node(run.currentNode);
        if (n.type == NodeType.BOSS) {
            if (run.act < content.acts().size()) {
                run.act++;
                run.currentNode = -1;
                run.visited.clear();
                regenerateMap();
                int missing = run.maxHp - run.hp;
                run.hp += (int) Math.round(missing * balance.actHealPct);
                run.phase = RunPhase.MAP;
            } else {
                run.phase = RunPhase.VICTORY;
            }
        } else {
            run.phase = RunPhase.MAP;
        }
        changed();
    }

    public void abandon() {
        if (!run.phase.finished()) {
            run.phase = RunPhase.DEFEAT;
            changed();
        }
    }

    // ================================================================== mutations used by events

    void changeGold(int amount) {
        run.gold = Math.max(0, run.gold + amount);
    }

    /** Events never kill: HP loss stops at 1. */
    void changeHp(int amount) {
        run.hp = Math.max(1, Math.min(run.maxHp, run.hp + amount));
    }

    int heal(double pct) {
        int before = run.hp;
        run.hp = Math.min(run.maxHp, run.hp + (int) Math.round(run.maxHp * pct));
        return run.hp - before;
    }

    void changeMaxHp(int amount) {
        run.maxHp = Math.max(5, run.maxHp + amount);
        if (amount > 0) run.hp += amount;
        run.hp = Math.max(1, Math.min(run.hp, run.maxHp));
    }

    public void gainRelic(String id) {
        if (run.relics.contains(id)) return;
        run.relics.add(id);
        int hpChange = RelicEffects.maxHpOnPickup(content, id);
        if (hpChange != 0) changeMaxHp(hpChange);
        rebuildModifiers();
    }

    String gainRandomRelic(Rarity rarity) {
        String id = Loot.pickRelic(run, content, rarity, Set.of(), run.eventRng);
        if (id != null) gainRelic(id);
        return id;
    }

    String gainRandomBlueprint(Rarity rarity) {
        String id = Loot.pickTower(run, content, rarity, Set.of(), run.eventRng);
        if (id != null) run.blueprints.add(new Blueprint(id, 0));
        return id;
    }

    Blueprint upgradeRandomBlueprint() {
        List<Blueprint> up = run.upgradeableBlueprints();
        if (up.isEmpty()) return null;
        Blueprint b = run.eventRng.pick(up);
        b.tier++;
        return b;
    }

    // ================================================================== accessors

    public Content content() { return content; }
    public RunState state() { return run; }
    public RunMap map() { return map; }
    public Modifiers modifiers() { return mods; }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange == null ? () -> { } : onChange;
    }
}
