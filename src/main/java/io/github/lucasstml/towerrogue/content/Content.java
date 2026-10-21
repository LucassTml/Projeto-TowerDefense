package io.github.lucasstml.towerrogue.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only registry of every definition in the game. Built once by {@link ContentLoader}
 * and passed explicitly to the systems that need it (no global singletons), which keeps
 * the simulation testable with alternative content.
 */
public final class Content {
    private final Map<String, TowerDef> towers = new LinkedHashMap<>();
    private final Map<String, EnemyDef> enemies = new LinkedHashMap<>();
    private final Map<String, AffixDef> affixes = new LinkedHashMap<>();
    private final Map<String, RelicDef> relics = new LinkedHashMap<>();
    private final Map<String, EventDef> events = new LinkedHashMap<>();
    private final Map<String, PerkDef> perks = new LinkedHashMap<>();
    private final List<ActDef> acts = new ArrayList<>();
    private final List<SynergyDef> synergies = new ArrayList<>();
    private final Balance balance;

    Content(Balance balance) {
        this.balance = balance;
    }

    void addTower(TowerDef d) { putUnique(towers, d.id(), d, "tower"); }
    void addEnemy(EnemyDef d) { putUnique(enemies, d.id(), d, "enemy"); }
    void addAffix(AffixDef d) { putUnique(affixes, d.id(), d, "affix"); }
    void addRelic(RelicDef d) { putUnique(relics, d.id(), d, "relic"); }
    void addEvent(EventDef d) { putUnique(events, d.id(), d, "event"); }
    void addPerk(PerkDef d) { putUnique(perks, d.id(), d, "perk"); }
    void addAct(ActDef d) { acts.add(d); }
    void addSynergy(SynergyDef d) { synergies.add(d); }

    private static <T> void putUnique(Map<String, T> map, String id, T value, String kind) {
        if (map.putIfAbsent(id, value) != null) throw new ContentException("duplicate " + kind + " id '" + id + "'");
    }

    public Balance balance() { return balance; }

    public TowerDef tower(String id) { return require(towers, id, "tower"); }
    public EnemyDef enemy(String id) { return require(enemies, id, "enemy"); }
    public AffixDef affix(String id) { return require(affixes, id, "affix"); }
    public RelicDef relic(String id) { return require(relics, id, "relic"); }
    public EventDef event(String id) { return require(events, id, "event"); }
    public PerkDef perk(String id) { return require(perks, id, "perk"); }

    public boolean hasTower(String id) { return towers.containsKey(id); }
    public boolean hasRelic(String id) { return relics.containsKey(id); }

    public Collection<TowerDef> towers() { return towers.values(); }
    public Collection<EnemyDef> enemies() { return enemies.values(); }
    public Collection<AffixDef> affixes() { return affixes.values(); }
    public Collection<RelicDef> relics() { return relics.values(); }
    public Collection<EventDef> events() { return events.values(); }
    public Collection<PerkDef> perks() { return perks.values(); }
    public List<SynergyDef> synergies() { return synergies; }
    public List<ActDef> acts() { return acts; }

    public ActDef act(int number) {
        for (ActDef a : acts) if (a.act() == number) return a;
        throw new ContentException("unknown act " + number);
    }

    private static <T> T require(Map<String, T> map, String id, String kind) {
        T v = map.get(id);
        if (v == null) throw new ContentException("unknown " + kind + " id '" + id + "'");
        return v;
    }
}
