package com.sliit.ems.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small JSON helper for the Employee Management System.
 *
 * Supports:
 * - Writing Map, List, String, Number, Boolean and null
 * - Reading JSON objects
 * - Reading nested JSON objects
 * - Reading JSON arrays
 * - Reading arrays containing objects
 */
public class Json {

    // ============================================================
    // WRITING
    // ============================================================

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(value, sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(Object value, StringBuilder sb) {

        if (value == null) {
            sb.append("null");

        } else if (value instanceof String s) {
            writeString(s, sb);

        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value.toString());

        } else if (value instanceof Map) {
            writeObject((Map<String, Object>) value, sb);

        } else if (value instanceof List) {
            writeArray((List<Object>) value, sb);

        } else {
            writeString(value.toString(), sb);
        }
    }

    private static void writeObject(
            Map<String, Object> map,
            StringBuilder sb) {

        sb.append('{');

        boolean first = true;

        for (Map.Entry<String, Object> entry : map.entrySet()) {

            if (!first) {
                sb.append(',');
            }

            first = false;

            writeString(entry.getKey(), sb);
            sb.append(':');
            writeValue(entry.getValue(), sb);
        }

        sb.append('}');
    }

    private static void writeArray(
            List<Object> list,
            StringBuilder sb) {

        sb.append('[');

        boolean first = true;

        for (Object value : list) {

            if (!first) {
                sb.append(',');
            }

            first = false;

            writeValue(value, sb);
        }

        sb.append(']');
    }

    private static void writeString(
            String s,
            StringBuilder sb) {

        sb.append('"');

        for (int i = 0; i < s.length(); i++) {

            char c = s.charAt(i);

            switch (c) {

                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");

                default -> {

                    if (c < 0x20) {
                        sb.append(
                                String.format(
                                        "\\u%04x",
                                        (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }

        sb.append('"');
    }

    public static Map<String, Object> obj() {
        return new LinkedHashMap<>();
    }

    // ============================================================
    // READING
    // ============================================================

    /**
     * Parses a JSON object into Map<String,Object>.
     *
     * Supports:
     * - String
     * - Number
     * - Boolean
     * - null
     * - nested objects
     * - arrays
     * - arrays containing objects
     */
    public static Map<String, Object> parseObject(String json) {

        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }

        Parser parser = new Parser(json);

        parser.skipWhitespace();

        Map<String, Object> result = parser.parseObject();

        parser.skipWhitespace();

        if (!parser.isEnd()) {
            throw new IllegalArgumentException(
                    "Unexpected characters after JSON object at position "
                            + parser.position());
        }

        return result;
    }

    // ============================================================
    // VALUE HELPERS
    // ============================================================

    public static String getString(
            Map<String, Object> obj,
            String key) {

        Object value = obj.get(key);

        return value == null
                ? null
                : value.toString();
    }

    public static Integer getInt(
            Map<String, Object> obj,
            String key) {

        Object value = obj.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        return Integer.parseInt(value.toString());
    }

    public static Double getDouble(
            Map<String, Object> obj,
            String key) {

        Object value = obj.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        return Double.parseDouble(value.toString());
    }

    // ============================================================
    // JSON PARSER
    // ============================================================

    private static class Parser {

        private final String s;
        private int i = 0;

        Parser(String s) {
            this.s = s;
        }

        // --------------------------------------------------------
        // Basic parser helpers
        // --------------------------------------------------------

        boolean isEnd() {
            return i >= s.length();
        }

        int position() {
            return i;
        }

        char peek() {

            if (i >= s.length()) {
                throw new IllegalArgumentException(
                        "Unexpected end of JSON");
            }

            return s.charAt(i);
        }

        char next() {

            if (i >= s.length()) {
                throw new IllegalArgumentException(
                        "Unexpected end of JSON");
            }

            return s.charAt(i++);
        }

        void expect(char expected) {

            if (i >= s.length() || s.charAt(i) != expected) {

                throw new IllegalArgumentException(
                        "Expected '" + expected +
                                "' at position " + i);
            }

            i++;
        }

        void skipWhitespace() {

            while (i < s.length()
                    && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        // --------------------------------------------------------
        // Parse JSON value
        // --------------------------------------------------------

        Object parseValue() {

            skipWhitespace();

            if (isEnd()) {
                throw new IllegalArgumentException(
                        "Unexpected end of JSON");
            }

            char c = peek();

            switch (c) {

                case '"' -> {
                    return parseString();
                }

                case '{' -> {
                    return parseObject();
                }

                case '[' -> {
                    return parseArray();
                }

                case 't' -> {
                    return parseTrue();
                }

                case 'f' -> {
                    return parseFalse();
                }

                case 'n' -> {
                    return parseNull();
                }

                default -> {
                    return parseNumber();
                }
            }
        }

        // --------------------------------------------------------
        // Parse object
        // --------------------------------------------------------

        Map<String, Object> parseObject() {

            Map<String, Object> result = new LinkedHashMap<>();

            expect('{');

            skipWhitespace();

            // Empty object
            if (peek() == '}') {

                next();

                return result;
            }

            while (true) {

                skipWhitespace();

                // Object key
                if (peek() != '"') {

                    throw new IllegalArgumentException(
                            "Expected JSON object key at position "
                                    + i);
                }

                String key = parseString();

                skipWhitespace();

                expect(':');

                skipWhitespace();

                // Object value
                Object value = parseValue();

                result.put(key, value);

                skipWhitespace();

                char separator = next();

                if (separator == '}') {
                    break;
                }

                if (separator != ',') {

                    throw new IllegalArgumentException(
                            "Expected ',' or '}' at position "
                                    + (i - 1));
                }
            }

            return result;
        }

        // --------------------------------------------------------
        // Parse array
        // --------------------------------------------------------

        List<Object> parseArray() {

            List<Object> result = new ArrayList<>();

            expect('[');

            skipWhitespace();

            // Empty array
            if (peek() == ']') {

                next();

                return result;
            }

            while (true) {

                skipWhitespace();

                Object value = parseValue();

                result.add(value);

                skipWhitespace();

                char separator = next();

                if (separator == ']') {
                    break;
                }

                if (separator != ',') {

                    throw new IllegalArgumentException(
                            "Expected ',' or ']' at position "
                                    + (i - 1));
                }
            }

            return result;
        }

        // --------------------------------------------------------
        // Parse string
        // --------------------------------------------------------

        String parseString() {

            expect('"');

            StringBuilder sb = new StringBuilder();

            while (!isEnd()) {

                char c = next();

                if (c == '"') {
                    return sb.toString();
                }

                if (c == '\\') {

                    if (isEnd()) {

                        throw new IllegalArgumentException(
                                "Invalid escape at end of JSON");
                    }

                    char escaped = next();

                    switch (escaped) {

                        case '"' -> sb.append('"');

                        case '\\' -> sb.append('\\');

                        case '/' -> sb.append('/');

                        case 'b' -> sb.append('\b');

                        case 'f' -> sb.append('\f');

                        case 'n' -> sb.append('\n');

                        case 'r' -> sb.append('\r');

                        case 't' -> sb.append('\t');

                        case 'u' -> {

                            if (i + 4 > s.length()) {

                                throw new IllegalArgumentException(
                                        "Invalid unicode escape");
                            }

                            String hex = s.substring(i, i + 4);

                            try {

                                sb.append(
                                        (char) Integer.parseInt(
                                                hex,
                                                16));

                            } catch (NumberFormatException e) {

                                throw new IllegalArgumentException(
                                        "Invalid unicode escape: "
                                                + hex);
                            }

                            i += 4;
                        }

                        default -> {

                            throw new IllegalArgumentException(
                                    "Invalid escape sequence: \\"
                                            + escaped);
                        }
                    }

                } else {

                    sb.append(c);
                }
            }

            throw new IllegalArgumentException(
                    "Unterminated JSON string");
        }

        // --------------------------------------------------------
        // Parse number
        // --------------------------------------------------------

        Number parseNumber() {

            int start = i;

            if (peek() == '-') {
                i++;
            }

            // Integer part
            while (i < s.length()
                    && Character.isDigit(s.charAt(i))) {
                i++;
            }

            // Decimal part
            if (i < s.length() && s.charAt(i) == '.') {

                i++;

                while (i < s.length()
                        && Character.isDigit(s.charAt(i))) {
                    i++;
                }
            }

            // Exponent
            if (i < s.length()
                    && (s.charAt(i) == 'e'
                            || s.charAt(i) == 'E')) {

                i++;

                if (i < s.length()
                        && (s.charAt(i) == '+'
                                || s.charAt(i) == '-')) {
                    i++;
                }

                while (i < s.length()
                        && Character.isDigit(s.charAt(i))) {
                    i++;
                }
            }

            String number = s.substring(start, i);

            try {

                if (!number.contains(".")
                        && !number.contains("e")
                        && !number.contains("E")) {

                    return Long.parseLong(number);

                } else {

                    return Double.parseDouble(number);
                }

            } catch (NumberFormatException e) {

                throw new IllegalArgumentException(
                        "Invalid JSON number: "
                                + number);
            }
        }

        // --------------------------------------------------------
        // Parse true
        // --------------------------------------------------------

        Boolean parseTrue() {

            expectLiteral("true");

            return Boolean.TRUE;
        }

        // --------------------------------------------------------
        // Parse false
        // --------------------------------------------------------

        Boolean parseFalse() {

            expectLiteral("false");

            return Boolean.FALSE;
        }

        // --------------------------------------------------------
        // Parse null
        // --------------------------------------------------------

        Object parseNull() {

            expectLiteral("null");

            return null;
        }

        // --------------------------------------------------------
        // Parse literal
        // --------------------------------------------------------

        void expectLiteral(String literal) {

            if (i + literal.length() > s.length()
                    || !s.startsWith(literal, i)) {

                throw new IllegalArgumentException(
                        "Expected '" + literal
                                + "' at position "
                                + i);
            }

            i += literal.length();
        }
    }
}