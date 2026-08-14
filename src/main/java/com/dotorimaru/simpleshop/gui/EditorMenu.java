package com.dotorimaru.simpleshop.gui;

import com.dotorimaru.simpleshop.SimpleShopPlugin;
import com.dotorimaru.simpleshop.model.Shop;
import com.dotorimaru.simpleshop.model.ShopItem;
import com.dotorimaru.simpleshop.util.Money;
import com.dotorimaru.simpleshop.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 관리자용 상점 편집 UI (54칸).
 *  - 1~4번 줄(0~35): 인벤토리에서 아이템을 집어 놓으면 상품 등록 (원본 아이템은 소모되지 않음, 복사 등록)
 *  - 배치된 상품 클릭: 위치 이동 (빈 칸에 놓기 / 다른 상품 칸에 놓으면 자리 교환)
 *  - 쉬프트+클릭: 상품 상세설정 UI
 *  - Q(버리기 키): 상품 제거
 */
public class EditorMenu extends Menu {
    private final Shop shop;
    private final int page;

    // 위치 이동 중인 상품 (커서에 가상으로 들려 있음)
    private ShopItem carrying = null;
    private int carryOriginSlot = -1;

    public EditorMenu(SimpleShopPlugin plugin, int page) {
        super(plugin);
        this.shop = plugin.shop();
        this.page = Math.max(0, Math.min(page, shop.maxPages() - 1));
        build(plugin.raw("gui.editor-title")
                .replace("{page}", String.valueOf(this.page + 1))
                .replace("{max}", String.valueOf(shop.maxPages())), 54);
    }

    @Override
    public void render() {
        clear();

        for (int slot = 0; slot < Shop.PRODUCT_SLOTS; slot++) {
            ShopItem it = shop.itemAt(page, slot);
            if (it != null && it.display() != null) inventory.setItem(slot, decorate(it));
        }

        for (int i = 36; i < 45; i++) inventory.setItem(i, filler());
        for (int i = 45; i < 54; i++) inventory.setItem(i, filler());

        Map<String, String> ph = Map.of(
                "{page}", String.valueOf(page + 1),
                "{max}", String.valueOf(shop.maxPages()),
                "{player}", viewer != null ? viewer.getName() : "",
                "{money}", viewer != null ? Money.fmt(plugin.economy().getBalance(viewer)) : "0");

        if (page > 0) {
            set(45, cfgIcon("buttons.prev-page", ph),
                    e -> switchTo(new EditorMenu(plugin, page - 1), (Player) e.getWhoClicked()));
        }
        set(49, cfgIcon("buttons.info", ph), null);
        if (page < shop.maxPages() - 1) {
            set(53, cfgIcon("buttons.next-page", ph),
                    e -> switchTo(new EditorMenu(plugin, page + 1), (Player) e.getWhoClicked()));
        }
    }

    @Override
    public void handleClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        int raw = e.getRawSlot();
        boolean topClick = raw >= 0 && raw < inventory.getSize();

        // ── 플레이어 인벤토리 영역 ──
        if (!topClick) {
            if (carrying != null) { e.setCancelled(true); return; } // 상점 아이템 반출 금지
            // 쉬프트 이동/더블클릭 수집은 상단 GUI 를 오염시키므로 차단, 그 외(집기/놓기)는 허용
            if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    || e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                e.setCancelled(true);
            }
            return;
        }

        e.setCancelled(true);

        // ── 네비게이션 / 채우기 줄 ──
        if (raw >= Shop.PRODUCT_SLOTS) {
            Consumer<InventoryClickEvent> h = handlers.get(raw);
            if (h != null) h.accept(e);
            return;
        }

        // ── 상품 영역 (0~35) ──
        ShopItem existing = shop.itemAt(page, raw);
        ClickType click = e.getClick();

        // Q / Ctrl+Q : 상품 제거
        if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
            if (existing != null) {
                shop.removeAt(page, raw);
                plugin.saveShop();
                inventory.setItem(raw, null);
                p.sendMessage(Text.c(plugin.msg("item-removed")));
            }
            return;
        }

        // 쉬프트+클릭 : 상세설정
        if (click.isShiftClick()) {
            if (existing != null) {
                finishCarry(p); // 이동 중이었다면 원위치
                switchTo(new ItemEditorMenu(plugin, existing), p);
            }
            return;
        }

        ItemStack cursor = e.getCursor();
        boolean cursorEmpty = cursor == null || cursor.getType().isAir();

        // ── 이동 중인 상품 놓기 ──
        if (carrying != null) {
            if (existing == null) {
                carrying.page(page);
                carrying.slot(raw);
                shop.put(carrying);
            } else {
                // 자리 교환
                shop.removeAt(page, raw);
                existing.slot(carryOriginSlot);
                shop.put(existing);
                inventory.setItem(carryOriginSlot, decorate(existing));
                carrying.page(page);
                carrying.slot(raw);
                shop.put(carrying);
            }
            inventory.setItem(raw, decorate(carrying));
            carrying = null;
            carryOriginSlot = -1;
            p.setItemOnCursor(null);
            plugin.saveShop();
            return;
        }

        // ── 배치된 상품 집기 (위치 이동 시작) ──
        if (existing != null) {
            if (!cursorEmpty) { p.sendMessage(Text.c(plugin.msg("slot-occupied"))); return; }
            carrying = existing;
            carryOriginSlot = raw;
            shop.removeAt(page, raw); // 메모리에서만 제거 (저장은 놓을 때)
            inventory.setItem(raw, null);
            p.setItemOnCursor(existing.display());
            return;
        }

        // ── 빈 슬롯에 새 상품 등록 (커서에 든 실제 아이템 복사) ──
        if (!cursorEmpty) {
            registerItem(p, raw, cursor);
        }
    }

    /** 드래그로 한 칸에 놓은 경우도 등록으로 처리 */
    @Override
    public void handleDrag(InventoryDragEvent e) {
        int topSize = inventory.getSize();
        boolean touchesTop = e.getRawSlots().stream().anyMatch(s -> s < topSize);
        if (carrying != null) { e.setCancelled(true); return; }
        if (!touchesTop) return;
        e.setCancelled(true);

        if (e.getRawSlots().size() != 1) return;
        int slot = e.getRawSlots().iterator().next();
        if (slot >= Shop.PRODUCT_SLOTS) return;
        if (shop.itemAt(page, slot) != null) return;
        ItemStack dragged = e.getOldCursor();
        if (dragged == null || dragged.getType().isAir()) return;
        registerItem((Player) e.getWhoClicked(), slot, dragged);
    }

    /** 닫힐 때: 이동 중이던 상품 원위치 + 가상 커서 제거 (복제 방지) */
    @Override
    public void handleClose(InventoryCloseEvent e) {
        if (carrying != null) {
            finishCarry((Player) e.getPlayer());
        }
    }

    private void finishCarry(Player p) {
        if (carrying == null) return;
        carrying.slot(carryOriginSlot);
        carrying.page(page);
        shop.put(carrying);
        inventory.setItem(carryOriginSlot, decorate(carrying));
        carrying = null;
        carryOriginSlot = -1;
        p.setItemOnCursor(null);
    }

    /** 상품 등록: 아이템 복사 (관리자의 원본 아이템은 소모되지 않는다) */
    private void registerItem(Player p, int slot, ItemStack template) {
        ShopItem item = new ShopItem(page, slot, template.clone(), -1, -1);
        shop.put(item);
        plugin.saveShop();
        inventory.setItem(slot, decorate(item));
        p.sendMessage(Text.c(plugin.msg("item-registered")));
    }

    /** 편집기에서 상품 위에 붙는 안내 lore (config 커스텀 가능) */
    private ItemStack decorate(ShopItem it) {
        ItemStack d = it.display();
        ItemMeta meta = d.getItemMeta();

        String disabled = plugin.raw("gui.price-disabled");
        String buy = it.canBuy() ? Money.fmt(it.buyPrice()) : disabled;
        String sell = it.canSell() ? Money.fmt(it.sellPrice()) : disabled;

        List<String> lines = new ArrayList<>();
        for (String l : plugin.getConfig().getStringList("editor-item-lore")) {
            lines.add(l.replace("{buy}", buy).replace("{sell}", sell));
        }

        List<Component> merged = new ArrayList<>();
        if (meta.lore() != null) merged.addAll(meta.lore());
        merged.addAll(Text.lore(lines));
        meta.lore(merged);
        d.setItemMeta(meta);
        return d;
    }
}
