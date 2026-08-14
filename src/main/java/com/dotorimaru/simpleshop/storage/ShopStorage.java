package com.dotorimaru.simpleshop.storage;

import com.dotorimaru.simpleshop.model.Shop;
import com.dotorimaru.simpleshop.model.ShopItem;
import com.dotorimaru.simpleshop.util.ItemSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * 상점 데이터 저장소 (plugins/SimpleShop/shop.yml).
 * 편집 즉시 save() 가 호출되어 서버가 꺼져도 데이터가 안전하다.
 */
public class ShopStorage {
    private final JavaPlugin plugin;
    private final File file;

    public ShopStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shop.yml");
    }

    public Shop load() {
        Shop shop = new Shop("main", 1);
        if (!file.exists()) return shop;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        shop.maxPages(yml.getInt("max-pages", 1));

        ConfigurationSection items = yml.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection s = items.getConfigurationSection(key);
                if (s == null) continue;
                try {
                    ItemStack display = ItemSerializer.fromBase64(s.getString("item"));
                    if (display == null) continue;
                    ShopItem item = new ShopItem(
                            s.getInt("page"), s.getInt("slot"), display,
                            s.getDouble("buy", -1), s.getDouble("sell", -1));
                    shop.put(item);
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "상품 로드 실패 (" + key + "): " + ex.getMessage());
                }
            }
        }
        return shop;
    }

    public void save(Shop shop) {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("max-pages", shop.maxPages());
        for (ShopItem it : shop.all()) {
            String key = "p" + it.page() + "s" + it.slot();
            yml.set("items." + key + ".page", it.page());
            yml.set("items." + key + ".slot", it.slot());
            yml.set("items." + key + ".buy", it.buyPrice());
            yml.set("items." + key + ".sell", it.sellPrice());
            yml.set("items." + key + ".item", ItemSerializer.toBase64(it.display()));
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "shop.yml 저장 실패", e);
        }
    }
}
