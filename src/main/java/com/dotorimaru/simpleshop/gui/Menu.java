package com.dotorimaru.simpleshop.gui;

import com.dotorimaru.simpleshop.SimpleShopPlugin;
import com.dotorimaru.simpleshop.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** 경량 GUI 베이스. 슬롯별 클릭 핸들러 맵을 들고, 커스텀 InventoryHolder 로 식별한다. */
public abstract class Menu implements InventoryHolder {
    protected final SimpleShopPlugin plugin;
    protected Inventory inventory;
    protected Player viewer;
    protected final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();

    /** 우리가 의도적으로 닫는 중(다른 메뉴 열기/채팅 입력)이면 true -> ESC 복귀 처리 생략 */
    protected boolean switching = false;

    protected Menu(SimpleShopPlugin plugin) { this.plugin = plugin; }

    protected void build(String title, int size) {
        this.inventory = Bukkit.createInventory(this, size, Text.c(title));
    }

    @Override public Inventory getInventory() { return inventory; }

    public void open(Player p) {
        this.viewer = p;
        plugin.input().cancel(p.getUniqueId()); // 메뉴를 새로 열면 대기 중인 채팅 입력은 무효
        render();
        p.openInventory(inventory);
    }

    /** ESC 복귀 등 닫힘 후처리를 생략하게 한다 (리로드 등으로 강제 닫을 때 사용) */
    public void suppressReturn() { this.switching = true; }

    /** 다른 메뉴로 전환 (현재 메뉴의 ESC 복귀 처리를 건너뛴다) */
    protected void switchTo(Menu next, Player p) {
        this.switching = true;
        next.open(p);
    }

    public abstract void render();

    /** 클릭 처리 기본: 전부 취소 + 상단 슬롯 핸들러 실행 */
    public void handleClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getRawSlot() < inventory.getSize()) {
            Consumer<InventoryClickEvent> h = handlers.get(e.getRawSlot());
            if (h != null) h.accept(e);
        }
    }

    /** 드래그 처리 기본: 상단을 건드리면 취소 */
    public void handleDrag(InventoryDragEvent e) {
        int topSize = inventory.getSize();
        if (e.getRawSlots().stream().anyMatch(s -> s < topSize)) e.setCancelled(true);
    }

    /** 인벤토리가 닫힐 때 (ESC 포함). 기본 no-op. */
    public void handleClose(InventoryCloseEvent e) {}

    protected void clear() { handlers.clear(); if (inventory != null) inventory.clear(); }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> onClick) {
        inventory.setItem(slot, item);
        if (onClick != null) handlers.put(slot, onClick);
    }

    protected ItemStack icon(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (name != null) meta.displayName(Text.c(name));
        if (lore != null && !lore.isEmpty()) meta.lore(Text.lore(lore));
        it.setItemMeta(meta);
        return it;
    }

    /** config 의 buttons.* 섹션에서 아이콘 생성. PLAYER_HEAD 면 viewer 머리 사용. */
    protected ItemStack cfgIcon(String path, Map<String, String> placeholders) {
        String matName = plugin.getConfig().getString(path + ".material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null || mat.isAir()) mat = Material.STONE;

        String name = apply(plugin.getConfig().getString(path + ".name", ""), placeholders);
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList(path + ".lore")) {
            lore.add(apply(line, placeholders));
        }

        ItemStack it = icon(mat, name, lore);
        if (mat == Material.PLAYER_HEAD && viewer != null && it.getItemMeta() instanceof SkullMeta skull) {
            skull.setOwningPlayer(viewer);
            skull.displayName(Text.c(name));
            if (!lore.isEmpty()) skull.lore(Text.lore(lore));
            it.setItemMeta(skull);
        }
        return it;
    }

    protected String apply(String s, Map<String, String> placeholders) {
        if (s == null) return "";
        if (placeholders != null) {
            for (Map.Entry<String, String> en : placeholders.entrySet()) {
                s = s.replace(en.getKey(), en.getValue());
            }
        }
        return s;
    }

    protected ItemStack filler() {
        String mat = plugin.getConfig().getString("gui.filler-material", "GRAY_STAINED_GLASS_PANE");
        Material m = Material.matchMaterial(mat);
        if (m == null || m.isAir()) m = Material.GRAY_STAINED_GLASS_PANE;
        return icon(m, " ", null);
    }
}
