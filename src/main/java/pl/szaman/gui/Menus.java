package pl.szaman.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.szaman.SzamanPlugin;
import pl.szaman.model.Category;
import pl.szaman.model.PerkType;
import pl.szaman.model.PlayerData;
import pl.szaman.util.Txt;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Menus {

    public static final int MAIN_SIZE = 45;
    public static final int PERKS_SIZE = 54;

    public static final int SLOT_TURBO = 20;
    public static final int SLOT_PERKS = 22;
    public static final int SLOT_STONE = 24;

    public static final int SLOT_HELP = 4;
    public static final int SLOT_BACK = 49;

    private static final String[] MAIN_PATTERN = {
            "YOLLGLLOY",
            "OGGGGGGGO",
            "LGGGGGGGL",
            "OGGGGGGGO",
            "YOLLGLLOY"
    };

    private final SzamanPlugin plugin;

    public Menus(SzamanPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ helpers

    private ItemStack item(Material material, int customModelData, String name, List<String> lore) {
        ItemStack it = new ItemStack(material);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Txt.color(name));
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(Txt.color(lore));
            }
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            it.setItemMeta(meta);
        }
        return it;
    }

    private Material material(String path, Material def) {
        Material m = Material.matchMaterial(plugin.getConfig().getString(path + ".material", def.name()));
        return m == null ? def : m;
    }

    private int cmd(String path) {
        return plugin.getConfig().getInt(path + ".custom-model-data", 0);
    }

    private Material paneFor(char c) {
        return switch (c) {
            case 'Y' -> Material.YELLOW_STAINED_GLASS_PANE;
            case 'O' -> Material.ORANGE_STAINED_GLASS_PANE;
            case 'L' -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            default -> Material.GRAY_STAINED_GLASS_PANE;
        };
    }

    private boolean isMenu(Inventory inv, MenuHolder.Type type) {
        return inv != null && inv.getHolder() instanceof MenuHolder h && h.type() == type;
    }

    // ------------------------------------------------------------------ MAIN

    public void openMain(Player player) {
        MenuHolder holder = new MenuHolder(MenuHolder.Type.MAIN);
        String title = Txt.color(plugin.getConfig().getString("gui.main.title", "&5&lSZAMAN"));
        Inventory inv = Bukkit.createInventory(holder, MAIN_SIZE, title);
        holder.setInventory(inv);
        buildMain(inv, player);
        player.openInventory(inv);
    }

    public void refreshMain(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (isMenu(top, MenuHolder.Type.MAIN)) {
            buildMain(top, player);
        }
    }

    private void buildMain(Inventory inv, Player player) {
        inv.clear();
        for (int row = 0; row < MAIN_PATTERN.length; row++) {
            for (int col = 0; col < 9; col++) {
                inv.setItem(row * 9 + col, item(paneFor(MAIN_PATTERN[row].charAt(col)), 0, " ", null));
            }
        }
        inv.setItem(SLOT_TURBO, turboItem(player));
        inv.setItem(SLOT_PERKS, perksButton());
        inv.setItem(SLOT_STONE, stoneRollButton(player));
    }

    private ItemStack turboItem(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        UUID id = player.getUniqueId();
        double personal = plugin.turbo().personal(id);
        double clan = plugin.turbo().clanBonus(id);
        double total = plugin.turbo().total(id);
        int cost = cfg.getInt("costs.turbo-drop", 5);

        List<String> lore = new ArrayList<>(cfg.getStringList("gui.main.turbo-description"));
        lore.add("");
        lore.add("&8↓ &aTwój bonus: &fx" + Txt.num(personal));
        lore.add("&8↓ &bBonus klanowy: &fx" + Txt.num(clan));
        lore.add("&8  → &fŁącznie: &ax" + Txt.num(total));
        lore.add("");
        lore.add("&8× &7Koszt: &fx" + cost + " &5Smoczy Odłamek");
        lore.add("");
        lore.add("&aKliknij, aby wylosować bonus!");
        return item(material("gui.main.turbo", Material.EMERALD), cmd("gui.main.turbo"), "&a&lPERK TURBO DROPU", lore);
    }

    private ItemStack perksButton() {
        List<String> lore = new ArrayList<>();
        lore.add("&8× &fTutaj znajdziesz swoje perki,");
        lore.add("&8× &fktóre możesz ulepszać!");
        lore.add("");
        lore.add("&aKliknij &faby przejść!");
        return item(material("gui.main.perks", Material.ENCHANTED_BOOK), cmd("gui.main.perks"), "&a&lTWOJE PERKI", lore);
    }

    private ItemStack stoneRollButton(Player player) {
        int cost = plugin.getConfig().getInt("costs.stone-roll", 10);
        List<String> lore = new ArrayList<>();
        lore.add("&8× &7Koszt: &fx" + cost + " &5Smoczy Odłamek");
        lore.add("&8× &7Posiadasz: &fx" + plugin.shards().count(player));
        lore.add("");
        lore.add("&aKliknij &fLPM &aaby wylosować kamień!");
        lore.add("&aKliknij &fLPM + SHIFT &aaby wylosować 10 kamieni!");
        return item(material("gui.main.stone", Material.AMETHYST_SHARD), cmd("gui.main.stone"), "&d&lLOSOWANIE KAMIENIA", lore);
    }

    // ------------------------------------------------------------------ PERKI

    public void openPerks(Player player) {
        MenuHolder holder = new MenuHolder(MenuHolder.Type.PERKS);
        String title = Txt.color(plugin.getConfig().getString("gui.perks.title", "&a&lTWOJE PERKI"));
        Inventory inv = Bukkit.createInventory(holder, PERKS_SIZE, title);
        holder.setInventory(inv);
        buildPerks(inv, player);
        player.openInventory(inv);
    }

    public void refreshIfPerksOpen(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (isMenu(top, MenuHolder.Type.PERKS)) {
            buildPerks(top, player);
        }
    }

    private void buildPerks(Inventory inv, Player player) {
        inv.clear();
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, 0, " ", null);
        for (int i = 0; i < PERKS_SIZE; i++) {
            inv.setItem(i, filler);
        }

        List<String> help = new ArrayList<>();
        help.add("&8× &fKamienie wylosujesz w menu &5/szaman&f,");
        help.add("&8× &fa użyjesz ich klikając &aPPM &fz kamieniem w ręce.");
        help.add("");
        help.add("&8× &fSloty są &cczerwone&f, dopóki nie zdobędziesz perku,");
        help.add("&8× &fpotem zmieniają się na &azielone&f.");
        inv.setItem(SLOT_HELP, item(material("gui.perks.help", Material.PAPER), cmd("gui.perks.help"), "&6&lJAK TO DZIAŁA?", help));

        PlayerData data = plugin.data().get(player.getUniqueId());
        for (Category c : Category.values()) {
            for (PerkType t : PerkType.of(c)) {
                inv.setItem((1 + t.row()) * 9 + c.column(), perkItem(data, t));
            }
            // ikona kamienia pod kolumna
            List<String> lore = new ArrayList<>();
            lore.add("&8× &7Użyj tego kamienia (&aPPM&7), aby");
            lore.add("&8× &7wylosować perk z kategorii " + c.color() + c.polishName() + "&7.");
            ItemStack stone = plugin.stones().createDisplay(c);
            ItemMeta sm = stone.getItemMeta();
            if (sm != null) {
                sm.setLore(Txt.color(lore));
                stone.setItemMeta(sm);
            }
            inv.setItem(4 * 9 + c.column(), stone);
        }

        inv.setItem(SLOT_BACK, item(Material.ARROW, 0, "&cWróć", List.of("&7Powrót do menu Szamana.")));
    }

    private ItemStack perkItem(PlayerData data, PerkType t) {
        Category c = t.category();
        double value = data.perk(t);
        double cap = plugin.perks().cap(t);
        boolean owned = value > 0;

        List<String> lore = new ArrayList<>();
        lore.add("&8× &bMoc tego &dperka &fsprawia,");
        for (String line : t.description()) {
            lore.add("&8× &f" + line.replace("{c}", c.color()));
        }
        lore.add("");
        lore.add("&8↓ &aTwój bonus: &f" + Txt.pct(value) + "%&7/&f" + Txt.pct(cap) + "%");
        lore.add("");
        if (owned) {
            lore.add("&aOdblokowany &7- użyj kolejnych kamieni, aby go ulepszyć.");
        } else {
            lore.add("&cZablokowany &7- użyj &f" + c.stoneName() + "&7, aby go zdobyć.");
        }
        Material pane = owned ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        return item(pane, 0, c.color() + "&l" + t.title().toUpperCase(java.util.Locale.ROOT), lore);
    }
}
