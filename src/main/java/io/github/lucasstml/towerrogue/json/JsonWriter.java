package io.github.lucasstml.towerrogue.json;

import java.util.Collection;
import java.util.Map;

/** Serializes {@link JsonObject}/{@link JsonArray} trees (and plain maps/collections) to text. */
public final class JsonWriter {
    private final StringBuilder out = new StringBuilder();
    private final boolean pretty;

    private JsonWriter(boolean pretty) {
        this.pretty = pretty;
    }

    public static String write(Object value, boolean pretty) {
        JsonWriter w = new JsonWriter(pretty);
        w.value(value, 0);
        return w.out.toString();
    }

    private void value(Object v, int depth) {
        if (v == null) {
            out.append("null");
        } else if (v instanceof JsonObject o) {
            object(o.entries(), o.size(), depth);
        } else if (v instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> mm = (Map<Object, Object>) m;
            JsonObject o = new JsonObject();
            mm.forEach((k, val) -> o.put(String.valueOf(k), val));
            object(o.entries(), o.size(), depth);
        } else if (v instanceof JsonArray a) {
            array(a, a.size(), depth);
        } else if (v instanceof Collection<?> c) {
            array(c, c.size(), depth);
        } else if (v instanceof String s) {
            string(s);
        } else if (v instanceof Enum<?> e) {
            string(e.name());
        } else if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                out.append("0");
            } else if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                out.append((long) d);
            } else {
                out.append(d);
            }
        } else if (v instanceof Number || v instanceof Boolean) {
            out.append(v);
        } else {
            string(v.toString());
        }
    }

    private void object(Iterable<Map.Entry<String, Object>> entries, int size, int depth) {
        if (size == 0) {
            out.append("{}");
            return;
        }
        out.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : entries) {
            if (!first) out.append(',');
            first = false;
            newline(depth + 1);
            string(e.getKey());
            out.append(pretty ? ": " : ":");
            value(e.getValue(), depth + 1);
        }
        newline(depth);
        out.append('}');
    }

    private void array(Iterable<?> items, int size, int depth) {
        if (size == 0) {
            out.append("[]");
            return;
        }
        out.append('[');
        boolean first = true;
        for (Object item : items) {
            if (!first) out.append(',');
            first = false;
            newline(depth + 1);
            value(item, depth + 1);
        }
        newline(depth);
        out.append(']');
    }

    private void newline(int depth) {
        if (!pretty) return;
        out.append('\n');
        out.append("  ".repeat(depth));
    }

    private void string(String s) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        out.append('"');
    }
}
