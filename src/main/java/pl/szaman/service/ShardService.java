package pl.szaman.service;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.szaman.SzamanPlugin;
import pl.szaman.util.Txt;

import java.util.Locale;

/** Smoczy Odlamek - rozpoznawanie, liczenie, pobieranie i tworzenie itemu. */
public final class ShardService {

    private final SzamanPlugin plugin;

    public ShardService(SzamanPlugin plugin) {
        this.plugin = plugin;
    }

    private Material material() {
        Material m = Material.matchMaterial(plugin.getConfig().getString("dragon-shard.material", "AMETHYST_SHARD"));
        return m == null ? Material.AMETHYST_SHARD : m;
    }

    public boolean isShard(ItemStack item) {
        if (item == null || item.getType() != material()) {
            return false;
        }
        FileConfiguration cfg = plugin.getConfig();
        int cmd = cfg.getInt("dragon-shard.custom-model-data", 0);
        String nameContains = cfg.getString("dragon-shard.name-contains", "");
        ItemMeta meta = item.getItemMeta();

        if (cmd > 0) {
            if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != cmd) {
                return false;
            }
        }
        if (nameContains != null && !nameContains.isEmpty()) {
            if (meta == null || !meta.hasDisplayName()) {
                return false;
            }
            String name = Txt.strip(meta.getDisplayName()).toLowerCase(Locale.ROOT);
            return name.contains(Txt.strip(nameContains).toLowerCase(Locale.ROOT));
        }
        return true;
    }

    public int count(Player player) {
        int total = 0;
        for (ItemStack it : player.getInventory().getStorageContents()) {
            if (isShard(it)) {
                total += it.getAmount();
            }
        }
        return total;
    }

    /** Zabiera dokladnie amount odlamkow. Zwraca false (i nic nie zabiera), jesli gracz ma za malo. */
    public boolean take(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (count(player) < amount) {
            return false;
        }
        ItemStack[] contents = player.getInventory().getStorageContents();
        int left = amount;
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack it = contents[i];
            if (!isShard(it)) {
                continue;
            }
            int take = Math.min(left, it.getAmount());
            if (take >= it.getAmount()) {
                contents[i] = null;
            } else {
                it.setAmount(it.getAmount() - take);
            }
            left -= take;
        }
        player.getInventory().setStorageContents(contents);
        return true;
    }

    public ItemStack create(int amount) {
        FileConfiguration cfg = plugin.getConfig();
        ItemStack item = new ItemStack(material(), Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Txt.color(cfg.getString("dragon-shard.display-name", "&5Smoczy Odłamek")));
            meta.setLore(Txt.color(cfg.getStringList("dragon-shard.lore")));
            int cmd = cfg.getInt("dragon-shard.custom-model-data", 0);
            if (cmd > 0) {
                meta.setCustomModelData(cmd);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
