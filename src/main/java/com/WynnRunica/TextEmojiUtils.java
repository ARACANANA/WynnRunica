package com.WynnRunica;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TextEmojiUtils {

    public static class Extracted {
        public final String key;
        public final List<Text> icons;
        public final Style contentStyle;
        Extracted(String key, List<Text> icons, Style contentStyle) {
            this.key = key;
            this.icons = icons;
            this.contentStyle = contentStyle;
        }
    }

    public static Extracted extract(Text source) {
        StringBuilder sb = new StringBuilder();
        List<Text> icons = new ArrayList<>();
        Style[] contentStyle = new Style[]{Style.EMPTY};
        walk(source, Style.EMPTY, sb, icons, contentStyle);
        return new Extracted(sb.toString(), icons, contentStyle[0]);
    }

    private static void walk(Text node, Style parentStyle, StringBuilder out,
                              List<Text> icons, Style[] firstContentStyle) {
        Style merged = node.getStyle().withParent(parentStyle);

        StyleSpriteSource font = merged.getFont();
        String fontStr = font == null ? "" : font.toString();

        if (fontStr.contains("minecraft:space")) {
            return;
        }

        boolean isIcon = fontStr.contains("minecraft:common")
                || fontStr.contains("minecraft:keybind")
                || fontStr.contains("minecraft:interface")
                || fontStr.contains("minecraft:tooltip");

        if (isIcon) {
            out.append("<em>");
            MutableText iconCopy = (MutableText) node.copy();
            iconCopy.setStyle(merged);
            icons.add(iconCopy);
            return;
        }

        final Style finalMerged = merged;
        node.getContent().visit(s -> {
            StringBuilder currentText = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);

                boolean isSurrogatePua = false;
                if (Character.isHighSurrogate(c) && i + 1 < s.length()) {
                    char low = s.charAt(i + 1);
                    int codePoint = Character.toCodePoint(c, low);
                    if (codePoint >= 0xF0000 && codePoint <= 0x10FFFD) {
                        isSurrogatePua = true;
                    }
                }

                if ((c >= '\uE000' && c <= '\uF8FF') || isSurrogatePua) {
                    if (currentText.length() > 0) {
                        out.append(currentText.toString());
                        currentText.setLength(0);
                    }
                    out.append("<em>");
                    String iconStr;
                    if (isSurrogatePua) {
                        iconStr = s.substring(i, i + 2);
                        i++;
                    } else {
                        iconStr = String.valueOf(c);
                    }
                    icons.add(Text.literal(iconStr).setStyle(finalMerged));
                } else {
                    currentText.append(c);
                }
            }
            if (currentText.length() > 0) {
                String t = currentText.toString();
                out.append(t);

                if (firstContentStyle[0] == null || firstContentStyle[0] == Style.EMPTY) {
                    String stripped = t.replaceAll("§.", "").trim();

                    if (!stripped.isEmpty()) {
                        firstContentStyle[0] = finalMerged;
                    }
                }
            }
            return java.util.Optional.empty();
        });

        for (Text child : node.getSiblings()) {
            walk(child, merged, out, icons, firstContentStyle);
        }
    }

    public static Text rebuild(String translated, List<Text> icons, Style rootStyle) {
        MutableText result = Text.literal("");
        
        Style baseStyle = rootStyle != null ? rootStyle : Style.EMPTY;
        Style current = baseStyle;
        StringBuilder buf = new StringBuilder();
        int iconIdx = 0;

        for (int i = 0; i < translated.length(); i++) {

            if (translated.startsWith("<em>", i)) {
                if (buf.length() > 0) {
                    result.append(Text.literal(buf.toString()).setStyle(current));
                    buf.setLength(0);
                }
                if (iconIdx < icons.size()) {
                    Text icon = icons.get(iconIdx++);
                    MutableText fixedIcon = icon.copy();
                    Style iconStyle = icon.getStyle();

                    if (iconStyle.getColor() == null) {
                        iconStyle = iconStyle.withColor(current.getColor());
                    }

                    fixedIcon.setStyle(iconStyle);
                    result.append(fixedIcon);
                }

                i += 3;
                continue;
            }

            char c = translated.charAt(i);
            if (c == '§' && i + 1 < translated.length()) {

                if (buf.length() > 0) {
                    result.append(Text.literal(buf.toString()).setStyle(current));
                    buf.setLength(0);
                }

                char code = translated.charAt(i + 1);
                net.minecraft.util.Formatting fmt = net.minecraft.util.Formatting.byCode(code);

                if (fmt != null) {
                    if (fmt == net.minecraft.util.Formatting.RESET) {
                        current = baseStyle;
                    } else if (fmt.isColor()) {
                        current = baseStyle.withColor(fmt);
                    } else {
                        current = current.withFormatting(fmt);
                    }
                }
                i++;
            } else {
                buf.append(c);
            }
        }

        if (buf.length() > 0) {
            result.append(Text.literal(buf.toString()).setStyle(current));
        }
        return result;
    }
}
