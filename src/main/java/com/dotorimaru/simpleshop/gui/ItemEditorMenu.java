package com.dotorimaru.simpleshop.gui;

import com.dotorimaru.simpleshop.SimpleShopPlugin;
import com.dotorimaru.simpleshop.model.ShopItem;
import com.dotorimaru.simpleshop.util.Money;
import com.dotorimaru.simpleshop.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Map;

/**
 * 관리자용 상품 상세설정 UI (27칸).
 *  - 구매 가격 / 판매 가격 버튼 클릭 -> UI 닫힘 -> 채팅으로 숫자 입력 -> 설정 후 이 UI 재오픈
 *  - -1 입력 시 해당 거래 비활성 (구매전용/판매전용 상품 구현)
 *  - ESC 로 닫으면 상점 편집 UI 로 복귀
 */
public class ItemEditorMenu extends Menu {
    private final ShopItem item;

    public ItemEditorMenu(SimpleShopPlugin plugin, ShopItem item) {
        super(plugin);
        this.item = item;
        build(plugin.raw("gui.item-editor-title"), 27);
    }

    @Override
    public void render() {
        clear();
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler());

        String disabled = plugin.raw("gui.price-disabled");

        // 상품 미리보기
        inventory.setItem(13, item.display());

        // 구매 가격 버튼
        Map<String, String> buyPh = Map.of(
                "{price}", item.canBuy() ? Money.fmt(item.buyPrice()) : disabled);
        set(11, cfgIcon("item-editor.buy-button", buyPh), e -> promptPrice((Player) e.getWhoClicked(), true));

        // 판매 가격 버튼
        Map<String, String> sellPh = Map.of(
                "{price}", item.canSell() ? Money.fmt(item.sellPrice()) : disabled);
        set(15, cfgIcon("item-editor.sell-button", sellPh), e -> promptPrice((Player) e.getWhoClicked(), false));
    }

    /** 가격 채팅 입력: UI 닫기 -> 숫자 입력 -> 설정 -> 이 UI 재오픈 */
    private void promptPrice(Player p, boolean buy) {
        switching = true; // ESC 복귀 처리 생략 (의도적 닫기)
        // InventoryClickEvent 핸들러 안에서 closeInventory 직접 호출은 금지 -> 다음 틱으로
        plugin.getServer().getScheduler().runTask(plugin, p::closeInventory);
        p.sendMessage(Text.c(plugin.msg(buy ? "input-buy-price" : "input-sell-price")));

        plugin.input().await(p, in -> {
            if (!p.isOnline()) return;

            String msg = in.trim();
            if (msg.equalsIgnoreCase("취소") || msg.equalsIgnoreCase("cancel")) {
                p.sendMessage(Text.c(plugin.msg("input-cancelled")));
                reopen(p);
                return;
            }

            double v;
            try {
                v = Double.parseDouble(msg.replace(",", ""));
            } catch (NumberFormatException ex) {
                p.sendMessage(Text.c(plugin.msg("input-invalid")));
                reopen(p);
                return;
            }
            if (Double.isNaN(v) || Double.isInfinite(v) || v > 1_000_000_000_000L) {
                p.sendMessage(Text.c(plugin.msg("input-invalid")));
                reopen(p);
                return;
            }
            if (v < 0) v = -1; // 음수는 전부 "비활성" 처리

            String disabled = plugin.raw("gui.price-disabled");
            if (buy) {
                item.buyPrice(v);
                p.sendMessage(Text.c(plugin.msg("buy-price-set")
                        .replace("{price}", item.canBuy() ? Money.fmt(item.buyPrice()) : disabled)));
            } else {
                item.sellPrice(v);
                p.sendMessage(Text.c(plugin.msg("sell-price-set")
                        .replace("{price}", item.canSell() ? Money.fmt(item.sellPrice()) : disabled)));
            }
            plugin.saveShop();
            reopen(p);
        });
    }

    private void reopen(Player p) {
        new ItemEditorMenu(plugin, item).open(p);
    }

    /** ESC 로 닫으면 상점 편집 UI 로 복귀 */
    @Override
    public void handleClose(InventoryCloseEvent e) {
        if (switching) return;
        Player p = (Player) e.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (p.isOnline()) new EditorMenu(plugin, item.page()).open(p);
        });
    }
}
