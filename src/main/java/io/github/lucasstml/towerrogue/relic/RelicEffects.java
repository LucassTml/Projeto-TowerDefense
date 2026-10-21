package io.github.lucasstml.towerrogue.relic;

import io.github.lucasstml.towerrogue.battle.BattleEvents;
import io.github.lucasstml.towerrogue.battle.BattleHook;
import io.github.lucasstml.towerrogue.battle.Enemy;
import io.github.lucasstml.towerrogue.battle.Fx;
import io.github.lucasstml.towerrogue.battle.Hit;
import io.github.lucasstml.towerrogue.content.Content;
import io.github.lucasstml.towerrogue.content.ContentException;
import io.github.lucasstml.towerrogue.content.DamageType;
import io.github.lucasstml.towerrogue.content.EffectDef;
import io.github.lucasstml.towerrogue.content.RelicDef;
import io.github.lucasstml.towerrogue.content.StatMod;
import io.github.lucasstml.towerrogue.content.StatusType;
import io.github.lucasstml.towerrogue.json.JsonObject;
import io.github.lucasstml.towerrogue.rules.Modifiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Registry that turns relic {@link EffectDef}s from {@code relics.json} into code.
 * Simple numeric effects are one-line lambdas; reactive effects return {@link BattleHook}s.
 */
public final class RelicEffects {
    private static final Map<String, Function<JsonObject, RelicEffect>> REGISTRY = new HashMap<>();

    static {
        // ---- static rule modifiers ----
        REGISTRY.put("towerStat", p -> {
            String tag = p.getString("tag", null);
            List<StatMod> mods = StatMod.parseList(p.getArray("mods"));
            return contributes(m -> mods.forEach(mod -> m.towerMods.add(new Modifiers.ScopedMod(tag, mod))));
        });
        REGISTRY.put("startMana", p -> contributes(m -> m.startMana += p.getInt("amount")));
        REGISTRY.put("killMana", p -> contributes(m -> m.killMana += p.getInt("amount")));
        REGISTRY.put("earlyCallBonus", p -> contributes(m -> m.earlyCallBonus += p.getInt("amount")));
        REGISTRY.put("goldMult", p -> contributes(m -> m.goldPct += p.getDouble("pct")));
        REGISTRY.put("shopDiscount", p -> contributes(m -> m.shopPricePct -= p.getDouble("pct")));
        REGISTRY.put("restHeal", p -> contributes(m -> m.restHealPct += p.getDouble("pct")));
        REGISTRY.put("healAfterCombat", p -> contributes(m -> m.healAfterCombat += p.getInt("amount")));
        REGISTRY.put("towerCost", p -> contributes(m -> m.towerCostPct += p.getDouble("pct")));
        REGISTRY.put("sellRefund", p -> contributes(m -> m.sellRefund = Math.max(m.sellRefund, p.getDouble("value"))));
        REGISTRY.put("draftChoices", p -> contributes(m -> m.draftChoices += p.getInt("amount")));
        REGISTRY.put("rareChance", p -> contributes(m -> m.rareChance += p.getDouble("amount")));
        REGISTRY.put("enemyHp", p -> contributes(m -> m.enemyHpPct += p.getDouble("pct")));
        REGISTRY.put("enemySpeed", p -> contributes(m -> m.enemySpeedPct += p.getDouble("pct")));
        REGISTRY.put("leakReduce", p -> contributes(m -> m.leakDamage -= p.getInt("amount")));
        REGISTRY.put("burnStacks", p -> contributes(m -> m.burnStacks += p.getInt("amount")));
        REGISTRY.put("poisonStacks", p -> contributes(m -> m.poisonStacks += p.getInt("amount")));
        REGISTRY.put("freeTower", p -> contributes(m -> m.freeTowers += p.getInt("count")));
        REGISTRY.put("maxHp", p -> new RelicEffect() {
            @Override
            public int maxHpOnPickup() {
                return p.getInt("amount");
            }
        });

        // ---- reactive battle hooks ----
        REGISTRY.put("waveMana", p -> hook(() -> b -> b.bus().subscribe(BattleEvents.WaveStarted.class, e -> {
            b.addMana(p.getInt("amount"));
        })));
        REGISTRY.put("interest", p -> hook(() -> b -> b.bus().subscribe(BattleEvents.WaveCleared.class, e -> {
            int gain = (int) Math.min(p.getInt("cap"), Math.floor(b.mana() * p.getDouble("pct")));
            if (gain > 0) {
                b.addMana(gain);
                b.emit(Fx.text(b.map().baseX() + 0.5, b.map().baseY() + 0.3, "Interest +" + gain, 0xF2C14E));
            }
        })));
        REGISTRY.put("aegis", p -> hook(() -> b -> {
            int[] charges = {p.getInt("charges")};
            b.bus().subscribe(BattleEvents.EnemyLeaked.class, e -> {
                if (charges[0] > 0 && !e.enemy.boss() && e.damage > 0) {
                    charges[0]--;
                    e.damage = 0;
                    b.emit(Fx.text(e.enemy.x, e.enemy.y - 0.5, "BLOCKED", 0x5AB0FF));
                }
            });
        }));
        REGISTRY.put("onKillExplode", p -> {
            StatusType status = status(p.getString("status"));
            DamageType type = DamageType.valueOf(p.getString("damageType", "fire").toUpperCase(Locale.ROOT));
            double flat = p.getDouble("damage"), pct = p.getDouble("hpPct", 0), radius = p.getDouble("radius");
            return hook(() -> b -> b.bus().subscribe(BattleEvents.EnemyKilled.class, e -> {
                Enemy v = e.enemy();
                if (!v.status.has(status)) return;
                double dmg = flat + pct * v.maxHp;
                b.emit(Fx.at(Fx.Type.EXPLOSION, v.x, v.y, radius, type.color));
                for (Enemy o : b.targeting().inRadius(v.x, v.y, radius, 2, b.enemies())) {
                    if (o != v) b.damage(new Hit(null, o, type, dmg));
                }
            }));
        });
        REGISTRY.put("onKillSpread", p -> {
            StatusType status = status(p.getString("status"));
            double mag = p.getDouble("magnitude"), dur = p.getDouble("duration"), radius = p.getDouble("radius");
            int stacks = p.getInt("stacks", 1);
            return hook(() -> b -> b.bus().subscribe(BattleEvents.EnemyKilled.class, e -> {
                Enemy v = e.enemy();
                if (!v.status.has(status)) return;
                for (Enemy o : b.targeting().inRadius(v.x, v.y, radius, 2, b.enemies())) {
                    if (o == v) continue;
                    for (int i = 0; i < stacks; i++) b.applyStatus(o, status, mag, dur, null);
                }
                b.emit(Fx.at(Fx.Type.PULSE, v.x, v.y, radius, status.color));
            }));
        });
        REGISTRY.put("damageVs", p -> {
            Predicate<Hit> cond = condition(p.getString("condition"), p.getDouble("threshold", 0.25));
            double bonus = p.getDouble("pct");
            return hook(() -> b -> b.bus().subscribe(Hit.class, h -> {
                if (cond.test(h)) h.multiplier *= 1 + bonus;
            }));
        });
        REGISTRY.put("critApply", p -> {
            StatusType status = status(p.getString("status"));
            double mag = p.getDouble("magnitude"), dur = p.getDouble("duration");
            return hook(() -> b -> b.bus().subscribe(Hit.class, h -> {
                if (h.crit && !h.dot) b.applyStatus(h.target, status, mag, dur, h.source);
            }));
        });
    }

    private RelicEffects() {
    }

    public static List<RelicEffect> create(RelicDef relic) {
        List<RelicEffect> out = new ArrayList<>();
        for (EffectDef e : relic.effects()) {
            Function<JsonObject, RelicEffect> ctor = REGISTRY.get(e.type());
            if (ctor == null) throw new ContentException("relic '" + relic.id() + "': unknown effect '" + e.type() + "'");
            out.add(ctor.apply(e.params()));
        }
        return out;
    }

    /** Fails fast at startup if any relic uses an unknown effect or bad parameters. */
    public static void validate(Content content) {
        for (RelicDef r : content.relics()) create(r);
    }

    public static void contributeAll(Content content, List<String> relicIds, Modifiers m) {
        for (String id : relicIds) for (RelicEffect e : create(content.relic(id))) e.contribute(m);
    }

    public static List<BattleHook> hooksFor(Content content, List<String> relicIds) {
        List<BattleHook> hooks = new ArrayList<>();
        for (String id : relicIds) {
            for (RelicEffect e : create(content.relic(id))) {
                BattleHook h = e.newHook();
                if (h != null) hooks.add(h);
            }
        }
        return hooks;
    }

    public static int maxHpOnPickup(Content content, String relicId) {
        int total = 0;
        for (RelicEffect e : create(content.relic(relicId))) total += e.maxHpOnPickup();
        return total;
    }

    // ------------------------------------------------------------------ helpers

    private static RelicEffect contributes(java.util.function.Consumer<Modifiers> c) {
        return new RelicEffect() {
            @Override
            public void contribute(Modifiers m) {
                c.accept(m);
            }
        };
    }

    private static RelicEffect hook(java.util.function.Supplier<BattleHook> factory) {
        return new RelicEffect() {
            @Override
            public BattleHook newHook() {
                return factory.get();
            }
        };
    }

    private static StatusType status(String s) {
        return StatusType.valueOf(s.toUpperCase(Locale.ROOT));
    }

    private static Predicate<Hit> condition(String name, double threshold) {
        return switch (name) {
            case "chilled" -> h -> h.target.status.has(StatusType.SLOW) || h.target.status.frozen();
            case "burning" -> h -> h.target.status.has(StatusType.BURN);
            case "poisoned" -> h -> h.target.status.has(StatusType.POISON);
            case "flying" -> h -> h.target.flying();
            case "elite" -> h -> h.target.elite() || h.target.boss();
            case "lowHp" -> h -> h.target.hpFraction() < threshold;
            default -> throw new ContentException("unknown damageVs condition '" + name + "'");
        };
    }
}
