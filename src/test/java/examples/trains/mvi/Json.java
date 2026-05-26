package examples.trains.mvi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *  A tiny, dependency-free recursive-descent JSON parser.
 *  <p>
 *  It exists so the train-schedule example can talk to the
 *  <a href="https://transitous.org">Transitous</a> / MOTIS REST API without
 *  pulling Jackson or Gson onto the example classpath. It parses a JSON
 *  document into plain Java values:
 *  <ul>
 *      <li>object &rarr; {@link Map}&lt;String,Object&gt; (insertion ordered)</li>
 *      <li>array  &rarr; {@link List}&lt;Object&gt;</li>
 *      <li>string &rarr; {@link String}</li>
 *      <li>number &rarr; {@link Double}</li>
 *      <li>true/false &rarr; {@link Boolean}</li>
 *      <li>null   &rarr; {@code null}</li>
 *  </ul>
 *  The small typed accessors ({@link #obj}, {@link #arr}, {@link #str},
 *  {@link #optStr}) keep the {@code TransitClient} mapping code readable.
 */
final class Json {

    private final String s;
    private int i;

    private Json(String text) { this.s = text; }

    /** Parse a JSON document into nested {@code Map}/{@code List}/{@code String}/... values. */
    static Object parse(String text) {
        Json p = new Json(text);
        p.skipWs();
        Object value = p.readValue();
        p.skipWs();
        return value;
    }

    // ---- typed accessors used by the mapping layer ---------------------------

    @SuppressWarnings("unchecked")
    static Map<String,Object> obj(Object o) {
        return o instanceof Map ? (Map<String,Object>) o : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    static List<Object> arr(Object o) {
        return o instanceof List ? (List<Object>) o : new ArrayList<>();
    }

    /** A required string field, or {@code ""} if missing/null. */
    static String str(Map<String,Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString();
    }

    /** An optional string field, or {@code null} if missing/blank. */
    static String optStr(Map<String,Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        String t = v.toString();
        return t.isEmpty() ? null : t;
    }

    static boolean bool(Map<String,Object> m, String key) {
        return Boolean.TRUE.equals(m.get(key));
    }

    // ---- parser internals ----------------------------------------------------

    private Object readValue() {
        char c = peek();
        switch (c) {
            case '{': return readObject();
            case '[': return readArray();
            case '"': return readString();
            case 't': case 'f': return readBoolean();
            case 'n': expect("null"); return null;
            default:  return readNumber();
        }
    }

    private Map<String,Object> readObject() {
        Map<String,Object> map = new LinkedHashMap<>();
        i++; // consume '{'
        skipWs();
        if (peek() == '}') { i++; return map; }
        while (true) {
            skipWs();
            String key = readString();
            skipWs();
            if (next() != ':') throw err("expected ':'");
            skipWs();
            map.put(key, readValue());
            skipWs();
            char c = next();
            if (c == '}') break;
            if (c != ',') throw err("expected ',' or '}'");
        }
        return map;
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<>();
        i++; // consume '['
        skipWs();
        if (peek() == ']') { i++; return list; }
        while (true) {
            skipWs();
            list.add(readValue());
            skipWs();
            char c = next();
            if (c == ']') break;
            if (c != ',') throw err("expected ',' or ']'");
        }
        return list;
    }

    private String readString() {
        if (next() != '"') throw err("expected string");
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') break;
            if (c == '\\') {
                char e = s.charAt(i++);
                switch (e) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/');  break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'u':
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    default: sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Double readNumber() {
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        return Double.valueOf(s.substring(start, i));
    }

    private Boolean readBoolean() {
        if (peek() == 't') { expect("true");  return Boolean.TRUE; }
        expect("false");
        return Boolean.FALSE;
    }

    private void expect(String word) {
        if (!s.startsWith(word, i)) throw err("expected '" + word + "'");
        i += word.length();
    }

    private char peek() { return s.charAt(i); }
    private char next() { return s.charAt(i++); }

    private void skipWs() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private RuntimeException err(String msg) {
        return new IllegalStateException("JSON parse error at " + i + ": " + msg);
    }
}