package com.dotorimaru.simpleshop;

import com.dotorimaru.simpleshop.command.AdminCommand;
import com.dotorimaru.simpleshop.command.ShopCommand;
import com.dotorimaru.simpleshop.gui.InputManager;
import com.dotorimaru.simpleshop.listener.MenuListener;
import com.dotorimaru.simpleshop.model.Shop;
import com.dotorimaru.simpleshop.storage.ShopStorage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * SimpleShop - 단일 서버용 GUI 상점 플러그인. (Paper 1.21.4 / Vault)
 *
 *  - /상점          : 상점 열기 (좌클릭 구매 / 우클릭 판매 / 쉬프트+클릭 대량 거래)
 *  - /상점관리 설정  : 편집 UI (드래그 앤 드롭 배치, 클릭 이동, 쉬프트+클릭 상세설정, Q 제거)
 *  - /상점관리 페이지 <번호> : 최대 페이지 설정
 *  - /상점관리 리로드 : config.yml + shop.yml 리로드
 */
public final class SimpleShopPlugin extends JavaPlugin {

    private Economy economy;
    private Shop shop;
    private ShopStorage storage;
    private InputManager input;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // ── Vault 이코노미 ──
        // 이코노미 플러그인(EssentialsX 등)이 우리보다 늦게 켜질 수 있으므로,
        // 지금 없으면 서버 첫 틱(모든 플러그인 활성화 이후)에 한 번 더 찾아본다.
        this.economy = findEconomy();
        if (economy == null) {
            getServer().getScheduler().runTask(this, () -> {
                this.economy = findEconomy();
                if (economy == null) {
                    getLogger().severe("Vault 이코노미를 찾을 수 없습니다. (이코노미 플러그인 설치 필요) 플러그인을 비활성화합니다.");
                    getServer().getPluginManager().disablePlugin(this);
                }
            });
        }

        // ── 데이터 ──
        this.storage = new ShopStorage(this);
        this.shop = storage.load();

        // ── GUI / 명령어 ──
        this.input = new InputManager(getConfig().getLong("settings.input-timeout-seconds", 60));
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        ShopCommand shopCmd = new ShopCommand(this);
        AdminCommand adminCmd = new AdminCommand(this);
        register("상점", shopCmd);
        register("상점관리", adminCmd);

        getLogger().info("SimpleShop 활성화 완료. (상품 " + shop.all().size() + "개, "
                + shop.maxPages() + "페이지)");
    }

    private void register(String name, org.bukkit.command.CommandExecutor exec) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(exec);
            if (exec instanceof org.bukkit.command.TabCompleter tab) cmd.setTabCompleter(tab);
        }
    }

    private Economy findEconomy() {
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : rsp.getProvider();
    }

    /** config.yml + shop.yml 다시 읽기. 열려 있는 상점/편집 UI 는 강제로 닫는다 (구버전 데이터 참조 방지). */
    public void reloadAll() {
        reloadConfig();
        this.shop = storage.load();
        for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder()
                    instanceof com.dotorimaru.simpleshop.gui.Menu menu) {
                menu.suppressReturn();
                p.closeInventory();
            }
        }
    }

    public void saveShop() { storage.save(shop); }

    // ── getters ──
    public Economy economy() { return economy; }
    public Shop shop() { return shop; }
    public InputManager input() { return input; }

    /** messages.<key> + prefix */
    public String msg(String key) {
        String prefix = getConfig().getString("messages.prefix", "");
        return prefix + getConfig().getString("messages." + key, key);
    }

    /** prefix 없는 config 문자열 */
    public String raw(String path) {
        return getConfig().getString(path, path);
    }

    public int bulkAmount() {
        return Math.max(2, getConfig().getInt("settings.bulk-amount", 64));
    }
}
