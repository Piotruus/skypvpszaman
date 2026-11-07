package pl.szaman.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.szaman.SzamanPlugin;
import pl.szaman.model.Category;
import pl.szaman.model.PerkType;
import pl.szaman.model.PlayerData;
import pl.szaman.util.Txt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** /szamanadmin ... */
public final class AdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT = List.of("odlamki", "kamien", "perk", "turbo", "drewno", "reset", "info", "reload");

    private final SzamanPlugin plugin;

    public AdminCommand(SzamanPlugin plugin) {
        this.plugin = plugin;
    }

    private void usage(CommandSender s, String usage) {
        plugin.msg().send(s, "admin-usage", "%usage%", usage);
    }

    private void info(CommandSender s, String text) {
        s.sendMessage(Txt.color(plugin.getConfig().getString("messages.prefix", "&8» ") + text));
    }

    private OfflinePlayer target(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    private static Double parseDouble(String s) {
        try {
            double d = Double.parseDouble(s.replace(',', '.'));
            return Double.isNaN(d) || Double.isInfinite(d) ? null : d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("szaman.admin")) {
            plugin.msg().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            usage(sender, "/" + label + " <" + String.join("|", ROOT) + ">");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.turbo().invalidateCache();
                plugin.msg().send(sender, "reloaded");
            }
            case "odlamki" -> shards(sender, label, args);
            case "kamien" -> stone(sender, label, args);
            case "perk" -> perk(sender, label, args);
            case "turbo" -> turbo(sender, label, args);
            case "drewno" -> wood(sender, label, args);
            case "reset" -> reset(sender, label, args);
            case "info" -> playerInfo(sender, label, args);
            default -> usage(sender, "/" + label + " <" + String.join("|", ROOT) + ">");
        }
        return true;
    }

    // ------------------------------------------------------------------ odlamki

    private void shards(CommandSender s, String label, String[] a) {
        String u = "/" + label + " odlamki give <gracz> <ilość>";
        if (a.length < 4 || !a[1].equalsIgnoreCase("give")) {
            usage(s, u);
            return;
        }
        Player p = Bukkit.getPlayerExact(a[2]);
        Integer amount = parseInt(a[3]);
        if (p == null) {
            plugin.msg().send(s, "player-not-found");
            return;
        }
        if (amount == null || amount < 1 || amount > 100000) {
            usage(s, u);
            return;
        }
        giveItems(p, amount, false, null);
        info(s, "&aDano &fx" + amount + " &5Smoczy Odłamek &agraczowi &f" + p.getName() + "&a.");
    }

    private void giveItems(Player p, int amount, boolean stone, Category category) {
        int left = amount;
        while (left > 0) {
            int stack = Math.min(left, 64);
            left -= stack;
            ItemStack it = stone ? plugin.stones().create(category, stack) : plugin.shards().create(stack);
            for (ItemStack rest : p.getInventory().addItem(it).values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), rest);
            }
        }
    }

    // ------------------------------------------------------------------ kamien

    private void stone(CommandSender s, String label, String[] a) {
        String u = "/" + label + " kamien give <gracz> <siły|odporności|zręczności|inteligencji|magii> [ilość]";
        if (a.length < 4 || !a[1].equalsIgnoreCase("give")) {
            usage(s, u);
            return;
        }
        Player p = Bukkit.getPlayerExact(a[2]);
        if (p == null) {
            plugin.msg().send(s, "player-not-found");
            return;
        }
        Category c = Category.byName(a[3]);
        if (c == null) {
            usage(s, u);
            return;
        }
        int amount = 1;
        if (a.length >= 5) {
            Integer parsed = parseInt(a[4]);
            if (parsed == null || parsed < 1 || parsed > 10000) {
                usage(s, u);
                return;
            }
            amount = parsed;
        }
        giveItems(p, amount, true, c);
        info(s, "&aDano &fx" + amount + " " + c.color() + c.stoneName() + " &agraczowi &f" + p.getName() + "&a.");
    }

    // ------------------------------------------------------------------ perk

    private void perk(CommandSender s, String label, String[] a) {
        String u = "/" + label + " perk <set|add|remove> <gracz> <perk> [wartość]";
        if (a.length < 4) {
            usage(s, u);
            return;
        }
        String op = a[1].toLowerCase(Locale.ROOT);
        OfflinePlayer t = target(a[2]);
        if (t == null) {
            plugin.msg().send(s, "player-not-found");
            return;
        }
        PerkType type = PerkType.byId(a[3]);
        if (type == null) {
            usage(s, u + " &7(perki: " + Arrays.stream(PerkType.values()).map(PerkType::id).collect(Collectors.joining(", ")) + ")");
            return;
        }
        PlayerData d = plugin.data().get(t.getUniqueId());
        double cap = plugin.perks().cap(type);

        switch (op) {
            case "remove" -> d.setPerk(type, 0);
            case "set", "add" -> {
                if (a.length < 5) {
                    usage(s, u);
                    return;
                }
                Double v = parseDouble(a[4]);
                if (v == null || v < 0) {
                    usage(s, u);
                    return;
                }
                double nv = op.equals("set") ? v : d.perk(type) + v;
                d.setPerk(type, Math.min(cap, nv));
            }
            default -> {
                usage(s, u);
                return;
            }
        }
        plugin.data().save(d);
        Player onlineTarget = Bukkit.getPlayer(t.getUniqueId());
        if (onlineTarget != null) {
            plugin.menus().refreshIfPerksOpen(onlineTarget);
        }
        info(s, "&aPerk &f" + type.title() + " &agracza &f" + a[2] + "&a: &f" + Txt.pct(d.perk(type)) + "%&7/&f" + Txt.pct(cap) + "%");
    }

    // ------------------------------------------------------------------ turbo

    private void turbo(CommandSender s, String label, String[] a) {
        String u = "/" + label + " turbo <set <gracz> <mnożnik>|clear <gracz>>";
        if (a.length < 3) {
            usage(s, u);
            return;
        }
        OfflinePlayer t = target(a[2]);
        if (t == null) {
            plugin.msg().send(s, "player-not-found");
            return;
        }
        PlayerData d = plugin.data().get(t.getUniqueId());
        String op = a[1].toLowerCase(Locale.ROOT);
        if (op.equals("clear")) {
            d.setTurbo(0);
        } else if (op.equals("set") && a.length >= 4) {
            Double v = parseDouble(a[3]);
            if (v == null || v < 0) {
                usage(s, u);
                return;
            }
            d.setTurbo(v);
        } else {
            usage(s, u);
            return;
        }
        plugin.data().save(d);
        plugin.turbo().invalidateCache();
        info(s, "&aTurbo Drop gracza &f" + a[2] + "&a: &fx" + Txt.num(d.turbo()));
    }

    // ------------------------------------------------------------------ drewno / ceny

    private void wood(CommandSender s, String label, String[] a) {
        String u = "/" + label + " drewno <set <MATERIAŁ> <kwota>|remove <MATERIAŁ>|list>";
        if (a.length < 2) {
            usage(s, u);
            return;
        }
        String op = a[1].toLowerCase(Locale.ROOT);
        if (op.equals("list")) {
            var sec = plugin.getConfig().getConfigurationSection("turbo-drop.prices");
            if (sec == null || sec.getKeys(false).isEmpty()) {
                info(s, "&7Brak własnych cen.");
                return;
            }
            for (String k : sec.getKeys(false)) {
                info(s, "&f" + k + " &8→ &a" + Txt.num(sec.getDouble(k)) + "$");
            }
            return;
        }
        if (a.length < 3) {
            usage(s, u);
            return;
        }
        Material m = Material.matchMaterial(a[2]);
        if (m == null || !m.isBlock()) {
            info(s, "&cNieznany blok: &f" + a[2]);
            return;
        }
        if (op.equals("remove")) {
            plugin.getConfig().set("turbo-drop.prices." + m.name(), null);
            plugin.saveConfig();
            info(s, "&aUsunięto cenę dla &f" + m.name() + "&a.");
        } else if (op.equals("set") && a.length >= 4) {
            Double v = parseDouble(a[3]);
            if (v == null || v < 0) {
                usage(s, u);
                return;
            }
            plugin.getConfig().set("turbo-drop.prices." + m.name(), v);
            plugin.saveConfig();
            info(s, "&aUstawiono &f" + m.name() + " &8→ &a" + Txt.num(v) + "$");
        } else {
            usage(s, u);
        }
    }

    // ------------------------------------------------------------------ reset / info

    private void reset(CommandSender s, String label, String[] a) {
        if (a.length < 2) {
            usage(s, "/" + label + " reset <gracz>");
            return;
        }
        OfflinePlayer t = target(a[1]);
        if (t == null) {
            plugin.msg().send(s, "player-not-found");
            return;
        }
        PlayerData d = plugin.data().get(t.getUniqueId());
        d.reset();
        plugin.data().save(d);
        plugin.turbo().invalidateCache();
        Player online = Bukkit.getPlayer(t.getUniqueId());
        if (online != null) {
            plugin.menus().refreshIfPerksOpen(online);
        }
        info(s, "&aWyczyszczono perki i bonusy gracza &f" + a[1] + "&a.");
    }

    private void playerInfo(CommandSender s, String label, String[] a) {
        if (a.length < 2) {
            usage(s, "/" + label + " info <gracz>");
            return;
        }
        OfflinePlayer t = target(a[1]);
        if (t == null) {
            plugin.msg().send(s, "player-not-found");
            return;
        }
        UUID id = t.getUniqueId();
        PlayerData d = plugin.data().get(id);
        info(s, "&6Szaman &8- &f" + a[1]);
        info(s, "&7Turbo Drop: twój &fx" + Txt.num(plugin.turbo().personal(id))
                + " &7| klanowy &fx" + Txt.num(plugin.turbo().clanBonus(id))
                + " &7| łącznie &ax" + Txt.num(plugin.turbo().total(id)));
        List<String> lines = new ArrayList<>();
        for (Map.Entry<PerkType, Double> e : d.perks().entrySet()) {
            lines.add(e.getKey().category().color() + e.getKey().title() + " &f" + Txt.pct(e.getValue()) + "%");
        }
        info(s, lines.isEmpty() ? "&7Brak perków." : String.join("&7, ", lines));
    }

    // ------------------------------------------------------------------ tab

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("szaman.admin")) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(ROOT);
        } else {
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "odlamki" -> {
                    if (args.length == 2) out.add("give");
                    else if (args.length == 3) out.addAll(onlineNames());
                    else if (args.length == 4) out.addAll(List.of("16", "64", "128"));
                }
                case "kamien" -> {
                    if (args.length == 2) out.add("give");
                    else if (args.length == 3) out.addAll(onlineNames());
                    else if (args.length == 4) {
                        for (Category c : Category.values()) out.add(c.polishName().toLowerCase(Locale.ROOT));
                    } else if (args.length == 5) out.addAll(List.of("1", "5", "10"));
                }
                case "perk" -> {
                    if (args.length == 2) out.addAll(List.of("set", "add", "remove"));
                    else if (args.length == 3) out.addAll(onlineNames());
                    else if (args.length == 4) {
                        for (PerkType t : PerkType.values()) out.add(t.id());
                    }
                }
                case "turbo" -> {
                    if (args.length == 2) out.addAll(List.of("set", "clear"));
                    else if (args.length == 3) out.addAll(onlineNames());
                    else if (args.length == 4) out.addAll(List.of("0.25", "0.5", "0.75", "1", "2"));
                }
                case "drewno" -> {
                    if (args.length == 2) out.addAll(List.of("set", "remove", "list"));
                    else if (args.length == 3) {
                        for (Material m : Material.values()) {
                            String n = m.name();
                            if (n.endsWith("_LOG") || n.endsWith("_STEM") || n.endsWith("_WOOD")) out.add(n);
                        }
                    }
                }
                case "reset", "info" -> {
                    if (args.length == 2) out.addAll(onlineNames());
                }
                default -> {
                }
            }
        }
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        return out.stream().filter(x -> x.toLowerCase(Locale.ROOT).startsWith(last)).sorted().collect(Collectors.toList());
    }

    private List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }
}
