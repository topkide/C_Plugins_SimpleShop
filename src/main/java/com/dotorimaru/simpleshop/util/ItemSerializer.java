package com.dotorimaru.simpleshop.util;

import org.bukkit.inventory.ItemStack;

import java.util.Base64;

/**
 * ItemStack <-> Base64. Paper 의 버전 안전 직렬화(serializeAsBytes) 사용.
 * 같은 마인크래프트 버전 내에서 NBT/데이터컴포넌트까지 안전하게 왕복된다.
 */
public final class ItemSerializer {
    private ItemSerializer() {}

    public static String toBase64(ItemStack item) {
        if (item == null) return "";
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public static ItemStack fromBase64(String data) {
        if (data == null || data.isEmpty()) return null;
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(data));
    }
}
