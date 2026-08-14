package com.dotorimaru.simpleshop.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 단일 상점. 페이지당 상품 슬롯 36칸(1~4번 줄) + 하단 네비게이션.
 * (확장 대비: 상점 ID 를 들고 있어 나중에 다중 상점으로 확장 가능)
 */
public class Shop {
    public static final int PRODUCT_SLOTS = 36; // 1~4번 줄

    private final String id;
    private int maxPages;

    // key = page * 54 + slot
    private final Map<Integer, ShopItem> items = new HashMap<>();

    public Shop(String id, int maxPages) {
        this.id = id;
        this.maxPages = Math.max(1, maxPages);
    }

    private static int key(int page, int slot) { return page * 54 + slot; }

    public String id() { return id; }
    public int maxPages() { return maxPages; }
    public void maxPages(int v) { this.maxPages = Math.max(1, v); }

    public ShopItem itemAt(int page, int slot) { return items.get(key(page, slot)); }
    public void put(ShopItem item) { items.put(key(item.page(), item.slot()), item); }
    public ShopItem removeAt(int page, int slot) { return items.remove(key(page, slot)); }

    public List<ShopItem> all() { return new ArrayList<>(items.values()); }

    /** maxPages 이후 페이지에 남아있는 (보이지 않게 된) 상품 수 */
    public long hiddenItemCount() {
        return items.values().stream().filter(i -> i.page() >= maxPages).count();
    }

    public void clear() { items.clear(); }
}
