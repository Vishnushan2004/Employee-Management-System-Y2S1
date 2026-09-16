package com.sliit.ems.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small JSON utility for this university project.
 *
 * Supports:
 *
 * WRITING:
 * - Map<String, Object>
 * - List<Object>
 * - String
 * - Number
 * - Boolean
 * - null
 *
 * READING:
 * - Objects
 * - Arrays
 * - Strings
 * - Numbers
 * - Booleans
 * - null
 *
 * Unlike the previous version, nested arrays and objects are parsed
 * into real Java Lists and Maps.
 *
 * This is important for payroll requests such as:
 *
 * "allowances": [
 * {
 * "type": "Transport",
 * "amount": 500
 * }
 * ]
 *
 * and:
 *
 * "deductions": [
 * {
 * "type": "EPF Employee Share",
 * "amount": 500
 * }
 * ]
 */
public class Json {

    // ============================================================
    // WRITING JSON
    // ============================================================

    public static String write(Object value) {

        StringBuilder sb = new StringBuilder();

        writeValue(value, sb);

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(
            Object value,
            StringBuilder sb) {

        if (value == null) {

            sb.append("null");

        } else if (value instanceof String s) {

            writeString(s, sb);

        } else if (value instanceof Number
                || value instanceof Boolean) {

            sb.append(value);

        } else if (value instanceof Map) {

            writeObject(
                    (Map<String, Object>) value,
                    sb);

        } else if (value instanceof List) {

            writeArray(
                    (List<Object>) value,
                    sb);

        } else {

            /*
             * Fallback for dates, enums, etc.
             */
            writeString(
                    value.toString(),
                    sb);
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

            writeString(
                    entry.getKey(),
                    sb);

            sb.append(':');

            writeValue(
                    entry.getValue(),
                    sb);
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

            writeValue(
                    value,
                    sb);
        }

        sb.append(']');
    }

    private static void writeString(
            String value,
            StringBuilder sb) {

        sb.append('"');

        for (int i = 0; i < value.length(); i++) {

            char c = value.charAt(i);

            switch (c) {

                case '"' ->
                    sb.append("\\\"");

                case '\\' ->
                    sb.append("\\\\");

                case '\n' ->
                    sb.append("\\n");

                case '\r' ->
                    sb.append("\\r");

                case '\t' ->
                    sb.append("\\t");

                case '\b' ->
                    sb.append("\\b");

                case '\f' ->
                    sb.append("\\f");

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

    // ============================================================
    // OBJECT HELPER
    // ============================================================

    public static Map<String, Object> obj() {

        return new LinkedHashMap<>();
    }

    // ============================================================
    // PARSE OBJECT
    // ============================================================

    /**
     * Parses a JSON object into a Java Map.
     */
    public static Map<String, Object> parseObject(
            String json) {

        if (json == null || json.isBlank()) {

            return new LinkedHashMap<>();
        }

        Parser parser = new Parser(json);

        Object value = parser.parse();

        if (!(value instanceof Map)) {

            throw new IllegalArgumentException(
                    "Expected a JSON object.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) value;

        return result;
    }

    // ============================================================
    // VALUE HELPERS
    // ============================================================

    public static String getString(
            Map<String, Object> obj,
            String key) {

        Object value = obj.get(key);

        if (value == null) {
            return null;
        }

        return value.toString();
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

        return Integer.parseInt(
                value.toString());
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

        return Double.parseDouble(
                value.toString());
    }

    public static Boolean getBoolean(
            Map<String, Object> obj,
            String key) {

        Object value = obj.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof Boolean b) {

            return b;
        }

        return Boolean.parseBoolean(
                value.toString());
    }

    /**
     * Returns a JSON array as a Java List.
     */
    public static List<Object> getList(
            Map<String, Object> obj,
            String key) {

        Object value = obj.get(key);

        if (value == null) {

            return new ArrayList<>();
        }

        if (!(value instanceof List)) {

            throw new IllegalArgumentException(
                    "'" + key + "' must be a JSON array.");
        }

        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) value;

        return list;
    }

    /**
     * Returns a JSON array containing objects as:
     *
     * List<Map<String,Object>>
     */
    public static List<Map<String, Object>> getObjectList(
            Map<String, Object> obj,
            String key) {

        List<Object> rawList = getList(obj, key);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object item : rawList) {

            if (!(item instanceof Map)) {

                throw new IllegalArgumentException(
                        "Every item in '" + key
                                + "' must be a JSON object.");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) item;

            result.add(map);
        }

        return result;
    }

    // ============================================================
    // JSON PARSER
    // ============================================================

    private static class Parser {

        private final String input;

        private int position = 0;

        Parser(String input) {

            this.input = input;
        }

        // --------------------------------------------------------
        // Main parser
        // --------------------------------------------------------

        Object parse() {

            skipWhitespace();

            Object value = parseValue();

            skipWhitespace();

            if (position != input.length()) {

                throw error(
                        "Unexpected characters after JSON value.");
            }

            return value;
        }

        // --------------------------------------------------------
        // Parse value
        // --------------------------------------------------------

        Object parseValue() {

            skipWhitespace();

            if (position >= input.length()) {

                throw error(
                        "Unexpected end of JSON.");
            }

            char c = input.charAt(position);

            switch (c) {

                case '{':
                    return parseObjectValue();

                case '[':
                    return parseArrayValue();

                case '"':
                    return parseString();

                case 't':
                    parseLiteral("true");
                    return Boolean.TRUE;

                case 'f':
                    parseLiteral("false");
                    return Boolean.FALSE;

                case 'n':
                    parseLiteral("null");
                    return null;

                default:

                    if (c == '-'
                            || c == '+'
                            || Character.isDigit(c)) {

                        return parseNumber();
                    }

                    throw error(
                            "Unexpected character '" + c + "'.");
            }
        }

        // --------------------------------------------------------
        // Parse object
        // --------------------------------------------------------

        private Map<String, Object> parseObjectValue() {

            expect('{');

            Map<String, Object> result = new LinkedHashMap<>();

            skipWhitespace();

            // Empty object
            if (peek('}')) {

                position++;

                return result;
            }

            while (true) {

                skipWhitespace();

                if (!peek('"')) {

                    throw error(
                            "Object key must be a string.");
                }

                String key = parseString();

                skipWhitespace();

                expect(':');

                skipWhitespace();

                Object value = parseValue();

                result.put(
                        key,
                        value);

                skipWhitespace();

                if (peek('}')) {

                    position++;

                    break;
                }

                expect(',');

                skipWhitespace();
            }

            return result;
        }

        // --------------------------------------------------------
        // Parse array
        // --------------------------------------------------------

        private List<Object> parseArrayValue() {

            expect('[');

            List<Object> result = new ArrayList<>();

            skipWhitespace();

            // Empty array
            if (peek(']')) {

                position++;

                return result;
            }

            while (true) {

                skipWhitespace();

                Object value = parseValue();

                result.add(value);

                skipWhitespace();

                if (peek(']')) {

                    position++;

                    break;
                }

                expect(',');

                skipWhitespace();
            }

            return result;
        }

        // --------------------------------------------------------
        // Parse string
        // --------------------------------------------------------

        private String parseString() {

            expect('"');

            StringBuilder result = new StringBuilder();

            while (position < input.length()) {

                char c = input.charAt(position++);

                if (c == '"') {

                    return result.toString();
                }

                if (c == '\\') {

                    if (position >= input.length()) {

                        throw error(
                                "Invalid escape sequence.");
                    }

                    char escaped = input.charAt(position++);

                    switch (escaped) {

                        case '"' ->
                            result.append('"');

                        case '\\' ->
                            result.append('\\');

                        case '/' ->
                            result.append('/');

                        case 'b' ->
                            result.append('\b');

                        case 'f' ->
                            result.append('\f');

                        case 'n' ->
                            result.append('\n');

                        case 'r' ->
                            result.append('\r');

                        case 't' ->
                            result.append('\t');

                        case 'u' -> {

                            if (position + 4 > input.length()) {

                                throw error(
                                        "Invalid Unicode escape.");
                            }

                            String hex = input.substring(
                                    position,
                                    position + 4);

                            try {

                                char unicode = (char) Integer.parseInt(
                                        hex,
                                        16);

                                result.append(
                                        unicode);

                            } catch (NumberFormatException e) {

                                throw error(
                                        "Invalid Unicode escape: "
                                                + hex);
                            }

                            position += 4;
                        }

                        default ->

                            throw error(
                                    "Invalid escape character: "
                                            + escaped);
                    }

                } else {

                    if (c < 0x20) {

                        throw error(
                                "Control character inside string.");
                    }

                    result.append(c);
                }
            }

            throw error(
                    "Unterminated JSON string.");
        }

        // --------------------------------------------------------
        // Parse number
        // --------------------------------------------------------

        private Number parseNumber() {

            int start = position;

            // Sign
            if (position < input.length()
                    && (input.charAt(position) == '-'
                            || input.charAt(position) == '+')) {

                position++;
            }

            // Integer part
            while (position < input.length()
                    && Character.isDigit(
                            input.charAt(position))) {

                position++;
            }

            // Decimal part
            if (position < input.length()
                    && input.charAt(position) == '.') {

                position++;

                while (position < input.length()
                        && Character.isDigit(
                                input.charAt(position))) {

                    position++;
                }
            }

            // Exponent
            if (position < input.length()
                    && (input.charAt(position) == 'e'
                            || input.charAt(position) == 'E')) {

                position++;

                if (position < input.length()
                        && (input.charAt(position) == '+'
                                || input.charAt(position) == '-')) {

                    position++;
                }

                while (position < input.length()
                        && Character.isDigit(
                                input.charAt(position))) {

                    position++;
                }
            }

            String numberText = input.substring(
                    start,
                    position);

            try {

                /*
                 * Return Integer/Long where possible,
                 * otherwise Double.
                 */
                if (!numberText.contains(".")
                        && !numberText.contains("e")
                        && !numberText.contains("E")) {

                    try {

                        return Integer.parseInt(
                                numberText);

                    } catch (NumberFormatException ignored) {

                        return Long.parseLong(
                                numberText);
                    }
                }

                return Double.parseDouble(
                        numberText);

            } catch (NumberFormatException e) {

                throw error(
                        "Invalid number: "
                                + numberText);
            }
        }

        // --------------------------------------------------------
        // Literal
        // --------------------------------------------------------

        private void parseLiteral(
                String literal) {

            if (position + literal.length() > input.length()) {

                throw error(
                        "Invalid literal.");
            }

            String actual = input.substring(
                    position,
                    position + literal.length());

            if (!actual.equals(literal)) {

                throw error(
                        "Expected '" + literal + "'.");
            }

            position += literal.length();
        }

        // --------------------------------------------------------
        // Helpers
        // --------------------------------------------------------

        private void expect(char expected) {

            if (position >= input.length()
                    || input.charAt(position) != expected) {

                throw error(
                        "Expected '"
                                + expected
                                + "'.");
            }

            position++;
        }

        private boolean peek(char expected) {

            return position < input.length()
                    && input.charAt(position) == expected;
        }

        private void skipWhitespace() {

            while (position < input.length()
                    && Character.isWhitespace(
                            input.charAt(position))) {

                position++;
            }
        }

        private IllegalArgumentException error(
                String message) {

            return new IllegalArgumentException(
                    message
                            + " Position: "
                            + position);
        }
    }
}