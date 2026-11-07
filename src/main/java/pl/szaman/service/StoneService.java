package pl.szaman.service;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import pl.szaman.SzamanPlugin;
import pl.szaman.model.Category;
import pl.szaman.util.Txt;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Kamienie: tworzenie itemow, rozpoznawanie oraz losowanie z menu (LOSOWANIE KAMIENIA). */
public final class StoneService {

    private final SzamanPlugin plugin;
    private final NamespacedKey key;

    public StoneService(SzamanPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "stone");
    }

    public ItemStack create(Category category, int amount) {
        return build(category, amount, true);
    }

    /** Wersja tylko do wyswietlania w menu (bez znacznika kamienia - nie da sie jej uzyc). */
    public ItemStack createDisplay(Category category) {
        return build(category, 1, false);
    }

    private ItemStack build(Category category, int amount, boolean tagged) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("stones." + category.id());
        Material mat = category.defaultStone();
        int cmd = 0;
        if (sec != null) {
            Material m = Material.matchMaterial(sec.getString("material", category.defaultStone().name()));
            if (m != null) {
                mat = m;
            }
            cmd = sec.getInt("custom-model-data", 0);
        }
        ItemStack item = new ItemStack(mat, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = plugin.getConfig().getString("stones.name", "%color%Kamień %category%")
                    .replace("%color%", category.color())
                    .replace("%category%", category.polishName());
            meta.setDisplayName(Txt.color(name));
            List<String> lore = new ArrayList<>();
            for (String line : plugin.getConfig().getStringList("stones.lore")) {
                lore.add(Txt.color(line.replace("%color%", category.color()).replace("%category%", category.polishName())));
            }
            meta.setLore(lore);
            if (cmd > 0) {
                meta.setCustomModelData(cmd);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            if (tagged) {
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, category.id());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Kategoria kamienia lub null, jesli to nie jest kamien Szamana. */
    public Category categoryOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(key, PersistentDataType.STRING);
        return id == null ? null : Category.byId(id);
    }

    public Category randomCategory() {
        Map<Category, Double> weights = new EnumMap<>(Category.class);
        double sum = 0;
        for (Category c : Category.values()) {
            double w = Math.max(0, plugin.getConfig().getDouble("stones.weights." + c.id(), 20));
            weights.put(c, w);
            sum += w;
        }
        if (sum <= 0) {
            return Category.values()[ThreadLocalRandom.current().nextInt(Category.values().length)];
        }
        double r = ThreadLocalRandom.current().nextDouble() * sum;
        for (Map.Entry<Category, Double> e : weights.entrySet()) {
            r -= e.getValue();
            if (r < 0) {
                return e.getKey();
            }
        }
        return Category.MAGIC;
    }

    /** LOSOWANIE KAMIENIA z menu: kosztuje stone-roll odlamkow za kazdy kamien. */
    public void rollStones(Player player, int times) {
        int cost = Math.max(0, plugin.getConfig().getInt("costs.stone-roll", 10));
        int total = cost * times;
        ShardService shards = plugin.shards();
        int have = shards.count(player);
        if (have < total) {
            plugin.msg().send(player, "not-enough-shards", "%cost%", String.valueOf(total), "%have%", String.valueOf(have));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1f);
            return;
        }
        if (!shards.take(player, total)) {
            plugin.msg().send(player, "not-enough-shards", "%cost%", String.valueOf(total), "%have%", String.valueOf(shards.count(player)));
            return;
        }

        Map<Category, Integer> won = new EnumMap<>(Category.class);
        for (int i = 0; i < times; i++) {
            won.merge(randomCategory(), 1, Integer::sum);
        }

        boolean dropped = false;
        List<String> parts = new ArrayList<>();
        for (Map.Entry<Category, Integer> e : won.entrySet()) {
            Category c = e.getKey();
            int amount = e.getValue();
            parts.add(c.color() + c.stoneName() + " &7x" + amount);
            int left = amount;
            while (left > 0) {
                int stack = Math.min(left, 64);
                left -= stack;
                Map<Integer, ItemStack> rest = player.getInventory().addItem(create(c, stack));
                for (ItemStack r : rest.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), r);
                    dropped = true;
                }
            }
        }
        plugin.msg().send(player, "stone-rolled", "%list%", String.join("&7, ", parts));
        if (dropped) {
            plugin.msg().send(player, "stone-no-room");
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
    }
}
