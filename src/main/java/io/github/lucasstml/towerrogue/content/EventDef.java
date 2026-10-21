package io.github.lucasstml.towerrogue.content;

import io.github.lucasstml.towerrogue.json.JsonArray;
import io.github.lucasstml.towerrogue.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A narrative "?" node. Each choice lists outcomes interpreted by {@code run.EventEngine},
 * e.g. {@code {"type":"gold","amount":50}} or a {@code gamble} with win/lose branches.
 *
 * @param special special events (like the run-start blessing) never appear randomly
 */
public record EventDef(String id, String title, String text, List<Integer> acts, boolean special,
                       List<Choice> choices) {

    /**
     * @param requires optional requirements, e.g. {@code {"gold": 40}}; empty object if none
     */
    public record Choice(String label, String description, JsonObject requires, JsonArray outcomes) {
    }

    public static EventDef parse(JsonObject o) {
        List<Choice> choices = new ArrayList<>();
        for (JsonObject c : o.getArray("choices").objects()) {
            choices.add(new Choice(c.getString("label"), c.getString("description", ""),
                    c.optObject("requires"), c.optArray("outcomes")));
        }
        return new EventDef(
                o.getString("id"),
                o.getString("title"),
                o.getString("text"),
                List.copyOf(o.optArray("acts").ints()),
                o.getBool("special", false),
                List.copyOf(choices));
    }
}
