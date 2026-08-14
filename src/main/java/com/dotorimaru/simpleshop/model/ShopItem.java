package com.dotorimaru.simpleshop.model;

import org.bukkit.inventory.ItemStack;

/**
 * 상점에 진열된 상품 하나.
 * 가격은 아이템 "1개" 기준. -1 이면 해당 거래(구매/판매) 비활성.
 */
public class ShopItem {
    private int page;              // 0-base
    private int slot;              // 0 ~ 35 (1~4번 줄)
    private ItemStack display;     // 진열/지급 기준 아이템 (수량은 진열용)
    private double buyPrice;       // 유저가 "구매"할 때 1개당 가격, -1 = 구매 불가
    private double sellPrice;      // 유저가 "판매"할 때 1개당 가격, -1 = 판매 불가

    public ShopItem(int page, int slot, ItemStack display, double buyPrice, double sellPrice) {
        this.page = page;
        this.slot = slot;
        this.display = display;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

    public int page() { return page; }
    public int slot() { return slot; }
    public ItemStack display() { return display == null ? null : display.clone(); }
    public double buyPrice() { return buyPrice; }
    public double sellPrice() { return sellPrice; }

    public boolean canBuy() { return buyPrice >= 0; }
    public boolean canSell() { return sellPrice >= 0; }

    public void page(int v) { this.page = v; }
    public void slot(int v) { this.slot = v; }
    public void display(ItemStack v) { this.display = v; }
    public void buyPrice(double v) { this.buyPrice = v < 0 ? -1 : v; }
    public void sellPrice(double v) { this.sellPrice = v < 0 ? -1 : v; }
}
