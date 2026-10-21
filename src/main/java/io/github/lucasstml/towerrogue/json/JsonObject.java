package io.github.lucasstml.towerrogue.json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Insertion-ordered JSON object with typed accessors.
 *
 * <p>Strict getters ({@code getInt("cost")}) throw a {@link JsonException} naming the
 * missing key, which turns typos in content files into readable errors at startup.
 * Getters with a default value are used for optional fields.
 */
public final class JsonObject {
    private final Map<String, Object> values = new LinkedHashMap<>();

    public JsonObject put(String key, Object value) {
        values.put(key, value);
        return this;
    }

    public boolean has(String key) {
        return values.containsKey(key) && values.get(key) != null;
    }

    public Object get(String key) {
        return values.get(key);
    }

    public Set<String> keys() {
        return values.keySet();
    }

    public Set<Map.Entry<String, Object>> entries() {
        return values.entrySet();
    }

    public int size() {
        return values.size();
    }

    // ---- strings ----

    public String getString(String key) {
        Object v = require(key);
        if (v instanceof String s) return s;
        throw typeError(key, "string", v);
    }

    public String getString(String key, String def) {
        Object v = values.get(key);
        return v instanceof String s ? s : def;
    }

    // ---- numbers ----

    public double getDouble(String key) {
        return asNumber(key, require(key)).doubleValue();
    }

    public double getDouble(String key, double def) {
        Object v = values.get(key);
        return v == null ? def : asNumber(key, v).doubleValue();
    }

    public int getInt(String key) {
        return asNumber(key, require(key)).intValue();
    }

    public int getInt(String key, int def) {
        Object v = values.get(key);
        return v == null ? def : asNumber(key, v).intValue();
    }

    public long getLong(String key) {
        return asNumber(key, require(key)).longValue();
    }

    public long getLong(String key, long def) {
        Object v = values.get(key);
        return v == null ? def : asNumber(key, v).longValue();
    }

    // ---- booleans ----

    public boolean getBool(String key, boolean def) {
        Object v = values.get(key);
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        throw typeError(key, "boolean", v);
    }

    // ---- nested ----

    public JsonObject getObject(String key) {
        Object v = require(key);
        if (v instanceof JsonObject o) return o;
        throw typeError(key, "object", v);
    }

    /** Returns the nested object, or an empty object when the key is absent. */
    public JsonObject optObject(String key) {
        Object v = values.get(key);
        if (v == null) return new JsonObject();
        if (v instanceof JsonObject o) return o;
        throw typeError(key, "object", v);
    }

    public JsonArray getArray(String key) {
        Object v = require(key);
        if (v instanceof JsonArray a) return a;
        throw typeError(key, "array", v);
    }

    /** Returns the nested array, or an empty array when the key is absent. */
    public JsonArray optArray(String key) {
        Object v = values.get(key);
        if (v == null) return new JsonArray();
        if (v instanceof JsonArray a) return a;
        throw typeError(key, "array", v);
    }

    private Object require(String key) {
        Object v = values.get(key);
        if (v == null) throw new JsonException("missing required key '" + key + "' in " + preview());
        return v;
    }

    private Number asNumber(String key, Object v) {
        if (v instanceof Number n) return n;
        throw typeError(key, "number", v);
    }

    private JsonException typeError(String key, String expected, Object actual) {
        return new JsonException("key '" + key + "' should be a " + expected + " but was "
                + (actual == null ? "null" : actual.getClass().getSimpleName()) + " in " + preview());
    }

    private String preview() {
        Object id = values.get("id");
        return id != null ? "object with id '" + id + "'" : "object " + values.keySet();
    }

    @Override
    public String toString() {
        return JsonWriter.write(this, false);
    }
}
