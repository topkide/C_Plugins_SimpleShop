package com.dotorimaru.simpleshop.command;

import com.dotorimaru.simpleshop.SimpleShopPlugin;
import com.dotorimaru.simpleshop.gui.ShopMenu;
import com.dotorimaru.simpleshop.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /상점 - 상점 UI 열기 */
public class ShopCommand implements CommandExecutor {
    private final SimpleShopPlugin plugin;

    public ShopCommand(SimpleShopPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (!p.hasPermission("simpleshop.use")) {
            p.sendMessage(Text.c(plugin.msg("no-permission")));
            return true;
        }
        new ShopMenu(plugin, 0).open(p);
        return true;
    }
}
