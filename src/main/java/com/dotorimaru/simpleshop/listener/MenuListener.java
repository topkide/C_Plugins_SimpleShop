package com.dotorimaru.simpleshop.listener;

import com.dotorimaru.simpleshop.SimpleShopPlugin;
import com.dotorimaru.simpleshop.gui.Menu;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.function.Consumer;

/** GUI 이벤트 디스패치 + 채팅 입력 캡처. */
public class MenuListener implements Listener {
    private final SimpleShopPlugin plugin;

    public MenuListener(SimpleShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof Menu menu) {
            menu.handleClick(e);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Menu menu) {
            menu.handleDrag(e);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Menu menu) {
            menu.handleClose(e);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        if (!plugin.input().isWaiting(p.getUniqueId())) return;
        e.setCancelled(true); // 가격 입력이 채팅에 노출되지 않도록
        String msg = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        Consumer<String> cb = plugin.input().consume(p.getUniqueId());
        if (cb == null) return;
        // 콜백은 메인스레드에서 실행 (인벤토리 조작 포함)
        plugin.getServer().getScheduler().runTask(plugin, () -> cb.accept(msg));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.input().cancel(e.getPlayer().getUniqueId());
    }
}
