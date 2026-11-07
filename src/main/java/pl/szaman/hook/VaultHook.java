package pl.szaman.hook;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import pl.szaman.SzamanPlugin;

/** Wyplaty przez Vault (to samo zrodlo salda, z ktorego korzysta 777-Economy). */
public final class VaultHook {

    private final SzamanPlugin plugin;
    private Economy economy;

    public VaultHook(SzamanPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private boolean setup() {
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean isReady() {
        return economy != null || setup();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0 || !isReady()) {
            return false;
        }
        EconomyResponse r = economy.depositPlayer(player, amount);
        return r != null && r.transactionSuccess();
    }
}
