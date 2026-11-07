package pl.szaman.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import pl.szaman.SzamanPlugin;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

/** Ceny blokow: z configu Szamana (turbo-drop.prices) i/lub z pluginu 777-Economy. */
public final class PriceService {

    private final SzamanPlugin plugin;
    private Plugin economy;
    private Method getConfigurationService;
    private Method getBlockEntry;
    private Method resolveFortunePrice;
    private Method getBasePrice;
    private Method isToolAllowed;
    private boolean warned;

    public PriceService(SzamanPlugin plugin) {
        this.plugin = plugin;
    }

    /** Cena za zniszczenie bloku (0 = brak ceny / nie dotyczy). */
    public double price(Material material, ItemStack tool) {
        FileConfiguration cfg = plugin.getConfig();
        String source = cfg.getString("turbo-drop.price-source", "AUTO").toUpperCase(Locale.ROOT);

        if (!source.equals("ECONOMY")) {
            String key = "turbo-drop.prices." + material.name();
            if (cfg.contains(key)) {
                return Math.max(0.0, cfg.getDouble(key));
            }
        }
        if (source.equals("CONFIG")) {
            return 0.0;
        }
        return economyPrice(material, tool);
    }

    private boolean initEconomy() {
        if (getConfigurationService != null && economy != null && economy.isEnabled()) {
            return true;
        }
        Plugin eco = Bukkit.getPluginManager().getPlugin("777-Economy");
        if (eco == null || !eco.isEnabled()) {
            return false;
        }
        try {
            Method m = eco.getClass().getMethod("getConfigurationService");
            Object cfg = m.invoke(eco);
            if (cfg == null) {
                return false;
            }
            getBlockEntry = cfg.getClass().getMethod("getBlockEntry", Material.class);
            resolveFortunePrice = cfg.getClass().getMethod("resolveFortunePrice", Material.class, int.class);
            economy = eco;
            getConfigurationService = m;
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                plugin.getLogger().warning("Nie udało się odczytać cen z 777-Economy (" + e + "). Używam tylko cen z configu.");
            }
            return false;
        }
    }

    private double economyPrice(Material material, ItemStack tool) {
        if (!initEconomy()) {
            return 0.0;
        }
        try {
            Object cfg = getConfigurationService.invoke(economy);
            Object result = getBlockEntry.invoke(cfg, material);
            if (!(result instanceof Optional<?> opt) || opt.isEmpty()) {
                return 0.0;
            }
            Object entry = opt.get();
            if (isToolAllowed == null) {
                isToolAllowed = entry.getClass().getMethod("isToolAllowed", ItemStack.class);
                getBasePrice = entry.getClass().getMethod("getBasePrice");
            }
            ItemStack hand = tool == null ? new ItemStack(Material.AIR) : tool;
            Object allowed = isToolAllowed.invoke(entry, hand);
            if (allowed instanceof Boolean b && !b) {
                return 0.0;
            }
            double price = 0.0;
            Object byFortune = resolveFortunePrice.invoke(cfg, material, fortuneLevel(hand));
            if (byFortune instanceof Number n) {
                price = n.doubleValue();
            }
            if (price <= 0) {
                Object base = getBasePrice.invoke(entry);
                if (base instanceof Number n) {
                    price = n.doubleValue();
                }
            }
            return Math.max(0.0, price);
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                plugin.getLogger().warning("Błąd odczytu ceny z 777-Economy: " + e);
            }
            return 0.0;
        }
    }

    private int fortuneLevel(ItemStack tool) {
        if (tool == null || tool.getType() == Material.AIR) {
            return 0;
        }
        Enchantment fortune = Enchantment.getByKey(NamespacedKey.minecraft("fortune"));
        return fortune == null ? 0 : tool.getEnchantmentLevel(fortune);
    }
}
