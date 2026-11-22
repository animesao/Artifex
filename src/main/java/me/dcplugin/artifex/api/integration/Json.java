package me.dcplugin.artifex.api.integration;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

final class Json {
    private Json() {
    }

    static String string(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }

    static String number(Number value) {
        return value == null ? "null" : value.toString();
    }

    static String strings(Collection<String> values) {
        if (values == null) return "[]";
        return "[" + values.stream().map(Json::string).collect(Collectors.joining(",")) + "]";
    }

    static String object(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(entry -> "\"" + escape(entry.getKey()) + "\":" + entry.getValue())
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }
}
