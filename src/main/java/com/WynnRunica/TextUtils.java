package com.WynnRunica;

public class TextUtils {
    private static final char SPECIAL_CHAR = '\uDAFF';

    public static String extractCleanText(String text) {
        if (text.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        boolean skipNext = false;

        for (char c : text.toCharArray()) {
            if (skipNext) { skipNext = false; continue; }
            if (c == SPECIAL_CHAR) { out.append(" "); skipNext = true; continue; }
            if (c >= '\uD800' && c <= '\uDBFF') { skipNext = true; continue; }
            if (c >= '\uDC00' && c <= '\uDFFF') { continue; }
            if (c >= '\uE000' && c <= '\uF8FF') { continue; }
            out.append(c);
        }
        return out.toString();
    }
}
