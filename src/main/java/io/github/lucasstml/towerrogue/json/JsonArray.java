package io.github.lucasstml.towerrogue.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** JSON array with typed accessors; iterating yields raw values. */
public final class JsonArray implements Iterable<Object> {
    private final List<Object> values = new ArrayList<>();

    public JsonArray add(Object value) {
        values.add(value);
        return this;
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Object get(int i) {
        return values.get(i);
    }

    public JsonObject getObject(int i) {
        Object v = values.get(i);
        if (v instanceof JsonObject o) return o;
        throw new JsonException("array element " + i + " should be an object");
    }

    public String getString(int i) {
        Object v = values.get(i);
        if (v instanceof String s) return s;
        throw new JsonException("array element " + i + " should be a string");
    }

    public double getDouble(int i) {
        Object v = values.get(i);
        if (v instanceof Number n) return n.doubleValue();
        throw new JsonException("array element " + i + " should be a number");
    }

    public int getInt(int i) {
        return (int) getDouble(i);
    }

    public long getLong(int i) {
        Object v = values.get(i);
        if (v instanceof Number n) return n.longValue();
        throw new JsonException("array element " + i + " should be a number");
    }

    /** All elements as objects (throws if any element is not an object). */
    public List<JsonObject> objects() {
        List<JsonObject> out = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) out.add(getObject(i));
        return out;
    }

    public List<String> strings() {
        List<String> out = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) out.add(getString(i));
        return out;
    }

    public List<Integer> ints() {
        List<Integer> out = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) out.add(getInt(i));
        return out;
    }

    @Override
    public Iterator<Object> iterator() {
        return values.iterator();
    }

    @Override
    public String toString() {
        return JsonWriter.write(this, false);
    }
}
