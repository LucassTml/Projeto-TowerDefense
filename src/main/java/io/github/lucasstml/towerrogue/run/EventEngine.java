package io.github.lucasstml.towerrogue.run;

import io.github.lucasstml.towerrogue.content.ContentException;
import io.github.lucasstml.towerrogue.content.EncounterKind;
import io.github.lucasstml.towerrogue.content.Rarity;
import io.github.lucasstml.towerrogue.json.JsonArray;
import io.github.lucasstml.towerrogue.json.JsonObject;

import java.util.List;
import java.util.Locale;

/**
 * Interpreter for event outcomes declared in {@code events.json}. Outcomes form a tiny
 * declarative language (gold, hp, relic, gamble with nested branches...), so writers can
 * add events without touching Java. Every outcome appends a human-readable line to the log.
 */
final class EventEngine {
    private static final double[] RANDOM_RARITY = {55, 33, 12};

    private EventEngine() {
    }

    static void resolve(RunController ctl, JsonArray outcomes, List<String> log) {
        for (JsonObject o : outcomes.objects()) apply(ctl, o, log);
    }

    private static void apply(RunController ctl, JsonObject o, List<String> log) {
        RunState run = ctl.state();
        String type = o.getString("type");
        switch (type) {
            case "gold" -> {
                int amt = o.getInt("amount");
                ctl.changeGold(amt);
                log.add(amt >= 0 ? "Gained " + amt + " gold." : "Lost " + (-amt) + " gold.");
            }
            case "hp" -> {
                int amt = o.getInt("amount");
                ctl.changeHp(amt);
                log.add(amt >= 0 ? "Healed " + amt + " HP." : "Lost " + (-amt) + " HP.");
            }
            case "heal" -> log.add("Healed " + ctl.heal(o.getDouble("pct")) + " HP.");
            case "maxHp" -> {
                int amt = o.getInt("amount");
                ctl.changeMaxHp(amt);
                log.add((amt >= 0 ? "+" : "") + amt + " max HP.");
            }
            case "relic" -> {
                String id = ctl.gainRandomRelic(rarity(run, o.getString("rarity", "random")));
                if (id != null) {
                    log.add("Obtained relic: " + ctl.content().relic(id).name() + ".");
                } else {
                    ctl.changeGold(50);
                    log.add("No relics left to find. Gained 50 gold instead.");
                }
            }
            case "blueprint" -> {
                String id = ctl.gainRandomBlueprint(rarity(run, o.getString("rarity", "random")));
                if (id != null) {
                    log.add("New blueprint: " + ctl.content().tower(id).name() + ".");
                } else {
                    Blueprint b = ctl.upgradeRandomBlueprint();
                    log.add(b != null ? "You already know every design. Upgraded " + name(ctl, b) + "."
                            : "Nothing new to learn.");
                }
            }
            case "upgradeRandom" -> {
                int count = o.getInt("count", 1);
                for (int i = 0; i < count; i++) {
                    Blueprint b = ctl.upgradeRandomBlueprint();
                    log.add(b != null ? "Upgraded " + name(ctl, b) + "." : "All blueprints are already fully upgraded.");
                    if (b == null) break;
                }
            }
            case "gamble" -> {
                boolean win = run.eventRng.chance(o.getDouble("chance"));
                log.add(win ? "Fortune smiles on you." : "Luck is not on your side.");
                resolve(ctl, win ? o.optArray("win") : o.optArray("lose"), log);
            }
            case "battle" -> {
                run.battleKind = EncounterKind.valueOf(o.getString("kind", "normal").toUpperCase(Locale.ROOT));
                run.eventBattlePending = true;
                log.add(run.battleKind == EncounterKind.ELITE ? "Elite enemies attack!" : "Enemies attack!");
            }
            case "text" -> log.add(o.getString("text"));
            default -> throw new ContentException("unknown event outcome '" + type + "'");
        }
    }

    private static String name(RunController ctl, Blueprint b) {
        return ctl.content().tower(b.towerId).name() + Blueprint.tierName(b.tier);
    }

    private static Rarity rarity(RunState run, String s) {
        if (s.equals("random")) return Loot.rollRarity(run.eventRng, RANDOM_RARITY);
        return Rarity.valueOf(s.toUpperCase(Locale.ROOT));
    }
}
