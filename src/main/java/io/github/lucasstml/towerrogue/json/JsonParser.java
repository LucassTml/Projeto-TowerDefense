package io.github.lucasstml.towerrogue.json;

/**
 * Small recursive-descent JSON parser (RFC 8259) with one lenient extension:
 * {@code //} line comments are allowed so content files can be annotated.
 *
 * <p>Value mapping: objects become {@link JsonObject}, arrays {@link JsonArray},
 * integers without fraction/exponent become {@link Long} (so 64-bit seeds survive a
 * round trip), other numbers {@link Double}.
 */
public final class JsonParser {
    private final String src;
    private int pos;

    private JsonParser(String src) {
        this.src = src;
    }

    public static Object parse(String text) {
        JsonParser p = new JsonParser(text);
        p.skipWhitespace();
        Object value = p.readValue();
        p.skipWhitespace();
        if (p.pos != p.src.length()) {
            throw p.error("unexpected trailing characters");
        }
        return value;
    }

    private Object readValue() {
        if (pos >= src.length()) throw error("unexpected end of input");
        char c = src.charAt(pos);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> {
                if (c == '-' || (c >= '0' && c <= '9')) yield readNumber();
                throw error("unexpected character '" + c + "'");
            }
        };
    }

    private JsonObject readObject() {
        JsonObject obj = new JsonObject();
        pos++; // '{'
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return obj;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') throw error("expected string key");
            String key = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            obj.put(key, readValue());
            skipWhitespace();
            char c = next();
            if (c == '}') return obj;
            if (c != ',') throw error("expected ',' or '}' in object");
        }
    }

    private JsonArray readArray() {
        JsonArray arr = new JsonArray();
        pos++; // '['
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return arr;
        }
        while (true) {
            skipWhitespace();
            arr.add(readValue());
            skipWhitespace();
            char c = next();
            if (c == ']') return arr;
            if (c != ',') throw error("expected ',' or ']' in array");
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= src.length()) throw error("unterminated string");
            char c = src.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = next();
            switch (e) {
                case '"' -> sb.append('"');
                case '\\' -> sb.append('\\');
                case '/' -> sb.append('/');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    if (pos + 4 > src.length()) throw error("bad unicode escape");
                    sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                    pos += 4;
                }
                default -> throw error("bad escape '\\" + e + "'");
            }
        }
    }

    private Number readNumber() {
        int start = pos;
        boolean fractional = false;
        if (peek() == '-') pos++;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c >= '0' && c <= '9') {
                pos++;
            } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                fractional = true;
                pos++;
            } else {
                break;
            }
        }
        String text = src.substring(start, pos);
        try {
            if (!fractional) return Long.parseLong(text);
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            throw error("bad number '" + text + "'");
        }
    }

    private Object literal(String word, Object value) {
        if (!src.startsWith(word, pos)) throw error("expected '" + word + "'");
        pos += word.length();
        return value;
    }

    private void skipWhitespace() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                pos++;
            } else if (c == '/' && pos + 1 < src.length() && src.charAt(pos + 1) == '/') {
                while (pos < src.length() && src.charAt(pos) != '\n') pos++;
            } else {
                break;
            }
        }
    }

    private char peek() {
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    private char next() {
        if (pos >= src.length()) throw error("unexpected end of input");
        return src.charAt(pos++);
    }

    private void expect(char c) {
        if (next() != c) {
            pos--;
            throw error("expected '" + c + "'");
        }
    }

    private JsonException error(String msg) {
        int line = 1, col = 1;
        for (int i = 0; i < Math.min(pos, src.length()); i++) {
            if (src.charAt(i) == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
        }
        return new JsonException("JSON parse error at line " + line + ", column " + col + ": " + msg);
    }
}
