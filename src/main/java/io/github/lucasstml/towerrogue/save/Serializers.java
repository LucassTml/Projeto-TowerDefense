package io.github.lucasstml.towerrogue.save;

import io.github.lucasstml.towerrogue.content.Content;
import io.github.lucasstml.towerrogue.content.EncounterKind;
import io.github.lucasstml.towerrogue.content.Rarity;
import io.github.lucasstml.towerrogue.json.JsonArray;
import io.github.lucasstml.towerrogue.json.JsonException;
import io.github.lucasstml.towerrogue.json.JsonObject;
import io.github.lucasstml.towerrogue.meta.Profile;
import io.github.lucasstml.towerrogue.run.Blueprint;
import io.github.lucasstml.towerrogue.run.RewardOption;
import io.github.lucasstml.towerrogue.run.RunPhase;
import io.github.lucasstml.towerrogue.run.RunState;
import io.github.lucasstml.towerrogue.run.ShopStock;

import java.util.Collection;

/**
 * Hand-written JSON mapping for saves. Explicit mapping (instead of reflection) keeps the
 * file format stable and readable, and makes version migrations a matter of reading
 * {@code version} and branching.
 */
final class Serializers {
    static final int RUN_VERSION = 1;
    static final int PROFILE_VERSION = 1;

    private Serializers() {
    }

    // ================================================================== run

    static JsonObject writeRun(RunState r) {
        JsonObject o = new JsonObject()
                .put("version", RUN_VERSION)
                .put("seed", r.seed)
                .put("ascension", r.ascension)
                .put("act", r.act)
                .put("currentNode", r.currentNode)
                .put("visited", ints(r.visited))
                .put("hp", r.hp)
                .put("maxHp", r.maxHp)
                .put("gold", r.gold)
                .put("phase", r.phase.name())
                .put("rng", new JsonObject()
                        .put("rewards", r.rewardRng.getState())
                        .put("shop", r.shopRng.getState())
                        .put("events", r.eventRng.getState()))
                .put("rarePity", r.rarePity)
                .put("forgeUses", r.forgeUses)
                .put("battleCounter", r.battleCounter)
                .put("towerPool", strings(r.towerPool))
                .put("relicPool", strings(r.relicPool))
                .put("relics", strings(r.relics))
                .put("seenEvents", strings(r.seenEvents))
                .put("battleKind", r.battleKind.name())
                .put("lastGoldReward", r.lastGoldReward)
                .put("eventId", r.eventId)
                .put("eventResolved", r.eventResolved)
                .put("eventBattlePending", r.eventBattlePending)
                .put("eventLog", strings(r.eventLog));
        JsonObject perks = new JsonObject();
        r.perks.forEach(perks::put);
        o.put("perks", perks);

        JsonArray bps = new JsonArray();
        for (Blueprint b : r.blueprints) bps.add(new JsonObject().put("id", b.towerId).put("tier", b.tier));
        o.put("blueprints", bps);

        JsonArray rewards = new JsonArray();
        for (RewardOption ro : r.rewards) {
            rewards.add(new JsonObject().put("kind", ro.kind.name()).put("id", ro.id).put("amount", ro.amount)
                    .put("group", ro.group).put("rarity", ro.rarity.name()).put("taken", ro.taken));
        }
        o.put("rewards", rewards);

        if (r.shop != null) {
            JsonArray items = new JsonArray();
            for (ShopStock.Item it : r.shop.items) {
                items.add(new JsonObject().put("kind", it.kind.name()).put("id", it.id).put("price", it.price).put("sold", it.sold));
            }
            o.put("shop", new JsonObject().put("items", items).put("forgePrice", r.shop.forgePrice)
                    .put("healPrice", r.shop.healPrice).put("forgeUsed", r.shop.forgeUsed).put("healUsed", r.shop.healUsed));
        }
        o.put("stats", new JsonObject().put("floorsCleared", r.floorsCleared).put("kills", r.kills)
                .put("elitesKilled", r.elitesKilled).put("bossesKilled", r.bossesKilled)
                .put("leaks", r.leaks).put("damage", r.damage));
        return o;
    }

    static RunState readRun(JsonObject o, Content content) {
        int version = o.getInt("version", 0);
        if (version != RUN_VERSION) throw new JsonException("unsupported run save version " + version);
        RunState r = new RunState(o.getLong("seed"));
        r.ascension = o.getInt("ascension");
        r.act = o.getInt("act");
        content.act(r.act);
        r.currentNode = o.getInt("currentNode");
        r.visited.addAll(o.getArray("visited").ints());
        r.hp = o.getInt("hp");
        r.maxHp = o.getInt("maxHp");
        r.gold = o.getInt("gold");
        r.phase = RunPhase.valueOf(o.getString("phase"));
        JsonObject rng = o.getObject("rng");
        r.rewardRng.setState(rng.getLong("rewards"));
        r.shopRng.setState(rng.getLong("shop"));
        r.eventRng.setState(rng.getLong("events"));
        r.rarePity = o.getDouble("rarePity", 0);
        r.forgeUses = o.getInt("forgeUses", 0);
        r.battleCounter = o.getInt("battleCounter", 0);
        r.towerPool.addAll(o.getArray("towerPool").strings());
        r.relicPool.addAll(o.getArray("relicPool").strings());
        for (String id : o.getArray("relics").strings()) {
            content.relic(id);
            r.relics.add(id);
        }
        r.seenEvents.addAll(o.optArray("seenEvents").strings());
        r.battleKind = EncounterKind.valueOf(o.getString("battleKind", "NORMAL"));
        r.lastGoldReward = o.getInt("lastGoldReward", 0);
        r.eventId = o.getString("eventId", null);
        if (r.eventId != null) content.event(r.eventId);
        r.eventResolved = o.getBool("eventResolved", false);
        r.eventBattlePending = o.getBool("eventBattlePending", false);
        r.eventLog.addAll(o.optArray("eventLog").strings());
        JsonObject perks = o.optObject("perks");
        for (String k : perks.keys()) r.perks.put(k, perks.getInt(k));

        for (JsonObject b : o.getArray("blueprints").objects()) {
            String id = b.getString("id");
            content.tower(id);
            r.blueprints.add(new Blueprint(id, b.getInt("tier")));
        }
        for (JsonObject ro : o.optArray("rewards").objects()) {
            RewardOption opt = new RewardOption(RewardOption.Kind.valueOf(ro.getString("kind")), ro.getString("id"),
                    ro.getInt("amount"), ro.getInt("group"), Rarity.valueOf(ro.getString("rarity")));
            opt.taken = ro.getBool("taken", false);
            r.rewards.add(opt);
        }
        if (o.has("shop")) {
            JsonObject s = o.getObject("shop");
            ShopStock shop = new ShopStock();
            for (JsonObject it : s.getArray("items").objects()) {
                ShopStock.Item item = new ShopStock.Item(ShopStock.Kind.valueOf(it.getString("kind")), it.getString("id"),
                        it.getInt("price"));
                item.sold = it.getBool("sold", false);
                shop.items.add(item);
            }
            shop.forgePrice = s.getInt("forgePrice");
            shop.healPrice = s.getInt("healPrice");
            shop.forgeUsed = s.getBool("forgeUsed", false);
            shop.healUsed = s.getBool("healUsed", false);
            r.shop = shop;
        }
        JsonObject st = o.optObject("stats");
        r.floorsCleared = st.getInt("floorsCleared", 0);
        r.kills = st.getInt("kills", 0);
        r.elitesKilled = st.getInt("elitesKilled", 0);
        r.bossesKilled = st.getInt("bossesKilled", 0);
        r.leaks = st.getInt("leaks", 0);
        r.damage = st.getLong("damage", 0);
        return r;
    }

    // ================================================================== profile

    static JsonObject writeProfile(Profile p) {
        JsonObject perks = new JsonObject();
        p.perks.forEach(perks::put);
        JsonArray history = new JsonArray();
        for (Profile.HistoryEntry h : p.history) {
            history.add(new JsonObject().put("seed", h.seed()).put("ascension", h.ascension()).put("won", h.won())
                    .put("act", h.act()).put("floors", h.floors()).put("essence", h.essence()).put("date", h.date()));
        }
        return new JsonObject()
                .put("version", PROFILE_VERSION)
                .put("essence", p.essence)
                .put("totalEssence", p.totalEssence)
                .put("unlocked", strings(p.unlocked))
                .put("perks", perks)
                .put("maxAscension", p.maxAscension)
                .put("runs", p.runs)
                .put("wins", p.wins)
                .put("bestFloor", p.bestFloor)
                .put("totalKills", p.totalKills)
                .put("bossesKilled", p.bossesKilled)
                .put("history", history);
    }

    static Profile readProfile(JsonObject o) {
        Profile p = new Profile();
        p.essence = o.getInt("essence", 0);
        p.totalEssence = o.getInt("totalEssence", 0);
        p.unlocked.addAll(o.optArray("unlocked").strings());
        JsonObject perks = o.optObject("perks");
        for (String k : perks.keys()) p.perks.put(k, perks.getInt(k));
        p.maxAscension = o.getInt("maxAscension", 0);
        p.runs = o.getInt("runs", 0);
        p.wins = o.getInt("wins", 0);
        p.bestFloor = o.getInt("bestFloor", 0);
        p.totalKills = o.getInt("totalKills", 0);
        p.bossesKilled = o.getInt("bossesKilled", 0);
        for (JsonObject h : o.optArray("history").objects()) {
            p.history.add(new Profile.HistoryEntry(h.getString("seed"), h.getInt("ascension"), h.getBool("won", false),
                    h.getInt("act"), h.getInt("floors"), h.getInt("essence"), h.getString("date", "")));
        }
        return p;
    }

    private static JsonArray strings(Collection<String> values) {
        JsonArray a = new JsonArray();
        for (String s : values) a.add(s);
        return a;
    }

    private static JsonArray ints(Collection<Integer> values) {
        JsonArray a = new JsonArray();
        for (int v : values) a.add((long) v);
        return a;
    }
}
