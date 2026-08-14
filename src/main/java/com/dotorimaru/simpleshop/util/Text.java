package com.dotorimaru.simpleshop.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** &-컬러코드 / &#RRGGBB 헥스 -> Component 변환. 아이템 기본 이탤릭 제거. */
public final class Text {
    private Text() {}

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");

    /** &#RRGGBB -> &x&R&R&G&G&B&B 로 전처리 */
    private static String preprocess(String s) {
        Matcher m = HEX.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            StringBuilder rep = new StringBuilder("&x");
            for (char c : m.group(1).toCharArray()) rep.append('&').append(c);
            m.appendReplacement(sb, Matcher.quoteReplacement(rep.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static Component c(String s) {
        return LEGACY.deserialize(preprocess(s == null ? "" : s))
                .decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> lore(List<String> lines) {
        List<Component> out = new ArrayList<>();
        if (lines != null) for (String l : lines) out.add(c(l));
        return out;
    }
}
