package com.dotorimaru.simpleshop.command;

import com.dotorimaru.simpleshop.SimpleShopPlugin;
import com.dotorimaru.simpleshop.gui.EditorMenu;
import com.dotorimaru.simpleshop.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /상점관리 - 관리자 명령어
 *  - /상점관리 설정          : 상점 편집 UI 열기
 *  - /상점관리 페이지 <번호>  : 상점 최대 페이지 설정
 *  - /상점관리 리로드        : config.yml + shop.yml 리로드
 */
public class AdminCommand implements CommandExecutor, TabCompleter {
    private final SimpleShopPlugin plugin;

    public AdminCommand(SimpleShopPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("simpleshop.admin")) {
            sender.sendMessage(Text.c(plugin.msg("no-permission")));
            return true;
        }

        if (args.length >= 1) {
            switch (args[0]) {
                case "설정" -> {
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage("플레이어만 사용할 수 있습니다.");
                        return true;
                    }
                    new EditorMenu(plugin, 0).open(p);
                    return true;
                }
                case "페이지" -> {
                    if (args.length < 2) {
                        sender.sendMessage(Text.c(plugin.msg("pages-usage")));
                        return true;
                    }
                    int pages;
                    try {
                        pages = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(Text.c(plugin.msg("pages-usage")));
                        return true;
                    }
                    if (pages < 1 || pages > 100) {
                        sender.sendMessage(Text.c(plugin.msg("pages-usage")));
                        return true;
                    }
                    plugin.shop().maxPages(pages);
                    plugin.saveShop();
                    sender.sendMessage(Text.c(plugin.msg("pages-set")
                            .replace("{page}", String.valueOf(pages))));
                    long hidden = plugin.shop().hiddenItemCount();
                    if (hidden > 0) {
                        sender.sendMessage(Text.c(plugin.msg("pages-hidden-warning")
                                .replace("{count}", String.valueOf(hidden))));
                    }
                    return true;
                }
                case "리로드" -> {
                    plugin.reloadAll();
                    sender.sendMessage(Text.c(plugin.msg("reloaded")));
                    return true;
                }
            }
        }

        for (String line : plugin.getConfig().getStringList("messages.admin-help")) {
            sender.sendMessage(Text.c(line));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("simpleshop.admin")) return List.of();
        if (args.length == 1) {
            return List.of("설정", "페이지", "리로드").stream()
                    .filter(s -> s.startsWith(args[0])).toList();
        }
        return List.of();
    }
}
