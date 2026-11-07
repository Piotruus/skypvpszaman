package pl.szaman;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.szaman.command.AdminCommand;
import pl.szaman.command.SzamanCommand;
import pl.szaman.gui.Menus;
import pl.szaman.hook.ClanHook;
import pl.szaman.hook.VaultHook;
import pl.szaman.listener.EconomyListener;
import pl.szaman.listener.MenuListener;
import pl.szaman.listener.PerkListener;
import pl.szaman.listener.PlayerListener;
import pl.szaman.listener.StoneListener;
import pl.szaman.service.DataManager;
import pl.szaman.service.Msg;
import pl.szaman.service.PerkService;
import pl.szaman.service.PriceService;
import pl.szaman.service.ShardService;
import pl.szaman.service.StoneService;
import pl.szaman.service.TurboService;

import java.util.Objects;

public final class SzamanPlugin extends JavaPlugin {

    private Msg msg;
    private DataManager data;
    private ClanHook clans;
    private VaultHook vault;
    private PriceService prices;
    private ShardService shards;
    private StoneService stones;
    private TurboService turbo;
    private PerkService perks;
    private Menus menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        msg = new Msg(this);
        data = new DataManager(this);
        clans = new ClanHook(this);
        prices = new PriceService(this);
        shards = new ShardService(this);
        stones = new StoneService(this);
        turbo = new TurboService(this);
        perks = new PerkService(this);
        menus = new Menus(this);

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            vault = new VaultHook(this);
        } else {
            getLogger().warning("Brak pluginu Vault - Turbo Drop nie będzie mógł wypłacać bonusu!");
        }

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new StoneListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new PerkListener(this), this);
        getServer().getPluginManager().registerEvents(new EconomyListener(this), this);

        Objects.requireNonNull(getCommand("szaman")).setExecutor(new SzamanCommand(this));
        AdminCommand admin = new AdminCommand(this);
        Objects.requireNonNull(getCommand("szamanadmin")).setExecutor(admin);
        Objects.requireNonNull(getCommand("szamanadmin")).setTabCompleter(admin);

        // autozapis co 5 minut
        Bukkit.getScheduler().runTaskTimer(this, () -> data.saveDirty(), 20L * 300, 20L * 300);

        for (Player p : Bukkit.getOnlinePlayers()) {
            data.get(p.getUniqueId());
        }

        getLogger().info("Szaman włączony. Klany: " + (clans.isAvailable() ? "777-Clans OK" : "brak 777-Clans")
                + ", Vault: " + (vault != null && vault.isReady() ? "OK" : "czeka na ekonomię"));
    }

    @Override
    public void onDisable() {
        if (data != null) {
            data.saveDirty();
        }
    }

    /** Wyplata przez Vault; false, jesli Vault/ekonomia niedostepne. */
    public boolean deposit(OfflinePlayer player, double amount) {
        return vault != null && vault.deposit(player, amount);
    }

    public Msg msg() {
        return msg;
    }

    public DataManager data() {
        return data;
    }

    public ClanHook clans() {
        return clans;
    }

    public PriceService prices() {
        return prices;
    }

    public ShardService shards() {
        return shards;
    }

    public StoneService stones() {
        return stones;
    }

    public TurboService turbo() {
        return turbo;
    }

    public PerkService perks() {
        return perks;
    }

    public Menus menus() {
        return menus;
    }
}
