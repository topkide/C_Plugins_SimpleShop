package com.dotorimaru.simpleshop.gui;

import com.dotorimaru.simpleshop.SimpleShopPlugin;
import com.dotorimaru.simpleshop.model.Shop;
import com.dotorimaru.simpleshop.model.ShopItem;
import com.dotorimaru.simpleshop.util.Money;
import com.dotorimaru.simpleshop.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 유저용 상점 화면 (54칸).
 *  - 좌클릭: 1개 구매 / 우클릭: 1개 판매
 *  - 쉬프트+좌클릭: 대량 구매 / 쉬프트+우클릭: 대량 판매 (기본 64개, config 조절)
 */
public class ShopMenu extends Menu {
    private final Shop shop;
    private final int page;

    public ShopMenu(SimpleShopPlugin plugin, int page) {
        super(plugin);
        this.shop = plugin.shop();
        this.page = Math.max(0, Math.min(page, shop.maxPages() - 1));
        build(plugin.raw("gui.shop-title")
                .replace("{page}", String.valueOf(this.page + 1))
                .replace("{max}", String.valueOf(shop.maxPages())), 54);
    }

    @Override
    public void render() {
        clear();

        // 상품 영역 (0 ~ 35)
        for (int slot = 0; slot < Shop.PRODUCT_SLOTS; slot++) {
            ShopItem it = shop.itemAt(page, slot);
            if (it == null || it.display() == null) continue;
            final ShopItem item = it;
            set(slot, decorate(item), e -> {
                Player p = (Player) e.getWhoClicked();
                // 편집으로 사라진 상품 클릭 방지
                if (plugin.shop().itemAt(page, e.getRawSlot()) != item) { render(); return; }
                int bulk = plugin.bulkAmount();
                switch (e.getClick()) {
                    case LEFT -> buy(p, item, 1);
                    case SHIFT_LEFT -> buy(p, item, bulk);
                    case RIGHT -> sell(p, item, 1);
                    case SHIFT_RIGHT -> sell(p, item, bulk);
                    default -> {}
                }
            });
        }

        // 5번 줄 채우기 (36 ~ 44)
        for (int i = 36; i < 45; i++) inventory.setItem(i, filler());
        // 하단 네비게이션 (45 ~ 53)
        for (int i = 45; i < 54; i++) inventory.setItem(i, filler());

        Map<String, String> ph = Map.of(
                "{page}", String.valueOf(page + 1),
                "{max}", String.valueOf(shop.maxPages()),
                "{player}", viewer != null ? viewer.getName() : "",
                "{money}", viewer != null ? Money.fmt(plugin.economy().getBalance(viewer)) : "0");

        if (page > 0) {
            set(45, cfgIcon("buttons.prev-page", ph),
                    e -> switchTo(new ShopMenu(plugin, page - 1), (Player) e.getWhoClicked()));
        }
        set(49, cfgIcon("buttons.info", ph), null);
        if (page < shop.maxPages() - 1) {
            set(53, cfgIcon("buttons.next-page", ph),
                    e -> switchTo(new ShopMenu(plugin, page + 1), (Player) e.getWhoClicked()));
        }
    }

    /** 상품 로어에 가격/조작법 안내를 덧붙인다. (config 로 커스텀 가능) */
    private ItemStack decorate(ShopItem it) {
        ItemStack display = it.display();
        ItemMeta meta = display.getItemMeta();

        String disabled = plugin.raw("gui.price-disabled");
        String buyStr = it.canBuy() ? Money.fmt(it.buyPrice()) : null;
        String sellStr = it.canSell() ? Money.fmt(it.sellPrice()) : null;
        String bulk = String.valueOf(plugin.bulkAmount());

        List<String> lines = new ArrayList<>(plugin.getConfig().getStringList("shop-item-lore.header"));
        lines.add(it.canBuy()
                ? plugin.raw("shop-item-lore.buy").replace("{buy}", buyStr)
                : plugin.raw("shop-item-lore.buy-disabled").replace("{disabled}", disabled));
        lines.add(it.canSell()
                ? plugin.raw("shop-item-lore.sell").replace("{sell}", sellStr)
                : plugin.raw("shop-item-lore.sell-disabled").replace("{disabled}", disabled));
        for (String l : plugin.getConfig().getStringList("shop-item-lore.footer")) {
            lines.add(l.replace("{bulk}", bulk));
        }

        List<Component> merged = new ArrayList<>();
        if (meta.lore() != null) merged.addAll(meta.lore());
        merged.addAll(Text.lore(lines));
        meta.lore(merged);
        display.setItemMeta(meta);
        return display;
    }

    // ────────────────────────── 거래 ──────────────────────────

    private void buy(Player p, ShopItem it, int amount) {
        if (!it.canBuy()) { p.sendMessage(Text.c(plugin.msg("cannot-buy"))); return; }

        ItemStack proto = it.display();
        int capacity = capacity(p.getInventory(), proto);
        int actual = Math.min(amount, capacity);
        if (actual <= 0) { p.sendMessage(Text.c(plugin.msg("no-space"))); return; }

        double total = it.buyPrice() * actual;
        if (plugin.economy().getBalance(p) < total) {
            p.sendMessage(Text.c(plugin.msg("no-money").replace("{price}", Money.fmt(total))));
            return;
        }
        if (!plugin.economy().withdrawPlayer(p, total).transactionSuccess()) {
            p.sendMessage(Text.c(plugin.msg("no-money").replace("{price}", Money.fmt(total))));
            return;
        }

        give(p, proto, actual);
        p.sendMessage(Text.c(plugin.msg("bought")
                .replace("{item}", itemName(it))
                .replace("{amount}", String.valueOf(actual))
                .replace("{price}", Money.fmt(total))));
        render(); // 보유 금액 갱신
    }

    private void sell(Player p, ShopItem it, int amount) {
        if (!it.canSell()) { p.sendMessage(Text.c(plugin.msg("cannot-sell"))); return; }

        ItemStack proto = it.display();
        int owned = count(p.getInventory(), proto);
        int actual = Math.min(amount, owned);
        if (actual <= 0) { p.sendMessage(Text.c(plugin.msg("no-items"))); return; }

        remove(p.getInventory(), proto, actual);
        double total = it.sellPrice() * actual;
        plugin.economy().depositPlayer(p, total);
        p.sendMessage(Text.c(plugin.msg("sold")
                .replace("{item}", itemName(it))
                .replace("{amount}", String.valueOf(actual))
                .replace("{price}", Money.fmt(total))));
        render();
    }

    // ────────────────────────── 인벤토리 헬퍼 ──────────────────────────

    /** 이 아이템을 몇 개까지 더 담을 수 있는지 (메인 인벤토리 36칸 기준) */
    private int capacity(PlayerInventory inv, ItemStack proto) {
        int max = proto.getMaxStackSize();
        int cap = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack cur = inv.getItem(i);
            if (cur == null || cur.getType().isAir()) cap += max;
            else if (cur.isSimilar(proto)) cap += Math.max(0, max - cur.getAmount());
        }
        return cap;
    }

    private int count(PlayerInventory inv, ItemStack proto) {
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack cur = inv.getItem(i);
            if (cur != null && cur.isSimilar(proto)) total += cur.getAmount();
        }
        return total;
    }

    private void give(Player p, ItemStack proto, int amount) {
        while (amount > 0) {
            int n = Math.min(amount, proto.getMaxStackSize());
            ItemStack s = proto.clone();
            s.setAmount(n);
            p.getInventory().addItem(s);
            amount -= n;
        }
    }

    private void remove(PlayerInventory inv, ItemStack proto, int amount) {
        for (int i = 0; i < 36 && amount > 0; i++) {
            ItemStack cur = inv.getItem(i);
            if (cur == null || !cur.isSimilar(proto)) continue;
            int take = Math.min(amount, cur.getAmount());
            amount -= take;
            if (take >= cur.getAmount()) inv.setItem(i, null);
            else cur.setAmount(cur.getAmount() - take);
        }
    }

    private String itemName(ShopItem it) {
        ItemStack d = it.display();
        if (d.hasItemMeta() && d.getItemMeta().hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(d.getItemMeta().displayName());
        }
        return d.getType().name();
    }
}
