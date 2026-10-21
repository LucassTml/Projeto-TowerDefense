package io.github.lucasstml.towerrogue.content;

import io.github.lucasstml.towerrogue.json.JsonArray;
import io.github.lucasstml.towerrogue.json.JsonObject;
import io.github.lucasstml.towerrogue.json.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Loads every JSON file under {@code /data} from the classpath, then cross-validates
 * references (e.g. an act pool naming an enemy that does not exist). Content bugs fail
 * fast at startup with the file name and id, instead of crashing mid-run.
 */
public final class ContentLoader {
    private ContentLoader() {
    }

    public static Content loadDefault() {
        Content c = new Content(new Balance(readObject("balance.json")));
        loadList("towers.json", TowerDef::parse, c::addTower);
        loadList("enemies.json", EnemyDef::parse, c::addEnemy);
        loadList("affixes.json", AffixDef::parse, c::addAffix);
        loadList("relics.json", RelicDef::parse, c::addRelic);
        loadList("events.json", EventDef::parse, c::addEvent);
        loadList("perks.json", PerkDef::parse, c::addPerk);
        loadList("synergies.json", SynergyDef::parse, c::addSynergy);
        loadList("acts.json", ActDef::parse, c::addAct);
        validate(c);
        return c;
    }

    private static <T> void loadList(String file, Function<JsonObject, T> parser, Consumer<T> sink) {
        Object root = readJson(file);
        if (!(root instanceof JsonArray arr)) throw new ContentException(file + ": root must be an array");
        for (int i = 0; i < arr.size(); i++) {
            try {
                sink.accept(parser.apply(arr.getObject(i)));
            } catch (RuntimeException e) {
                throw new ContentException(file + " entry #" + i + ": " + e.getMessage(), e);
            }
        }
    }

    private static JsonObject readObject(String file) {
        Object root = readJson(file);
        if (!(root instanceof JsonObject o)) throw new ContentException(file + ": root must be an object");
        return o;
    }

    private static Object readJson(String file) {
        String path = "/data/" + file;
        try (InputStream in = ContentLoader.class.getResourceAsStream(path)) {
            if (in == null) throw new ContentException("missing resource " + path);
            return JsonParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ContentException("cannot read " + path, e);
        } catch (RuntimeException e) {
            if (e instanceof ContentException) throw e;
            throw new ContentException(file + ": " + e.getMessage(), e);
        }
    }

    private static void validate(Content c) {
        Balance b = c.balance();
        for (String id : b.starterTowers) c.tower(id);
        for (ActDef a : c.acts()) {
            String where = "acts.json act " + a.act() + ": ";
            try {
                for (ActDef.PoolEntry p : a.pool()) c.enemy(p.enemy());
                for (String e : a.elites()) c.enemy(e);
                for (String e : a.bossEscorts()) c.enemy(e);
                if (!c.enemy(a.boss()).boss()) throw new ContentException("'" + a.boss() + "' is not marked as boss");
            } catch (ContentException e) {
                throw new ContentException(where + e.getMessage(), e);
            }
        }
        for (EnemyDef e : c.enemies()) {
            for (EffectDef ab : e.abilities()) {
                String ref = ab.params().getString("enemy", null);
                if (ref != null && !ref.equals(e.id())) {
                    try {
                        c.enemy(ref);
                    } catch (ContentException ex) {
                        throw new ContentException("enemies.json '" + e.id() + "': " + ex.getMessage(), ex);
                    }
                }
            }
        }
        for (TowerDef t : c.towers()) {
            if (!t.branches().isEmpty() && t.branches().size() != 2) {
                throw new ContentException("towers.json '" + t.id() + "': expected exactly 2 branches");
            }
        }
        if (c.acts().isEmpty()) throw new ContentException("acts.json: no acts defined");
    }

    /** Parses "#rrggbb" into 0xRRGGBB. */
    public static int parseColor(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return Integer.parseInt(h, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            throw new ContentException("bad color '" + hex + "'");
        }
    }
}
