package pl.szaman.listener;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import pl.szaman.SzamanPlugin;
import pl.szaman.model.PerkType;
import pl.szaman.service.PerkService;
import pl.szaman.util.Txt;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Turbo Drop: po zniszczeniu bloku doplaca bonus = cena bloku x (bonus gracza + bonus klanu).
 * Cene bierzemy z configu Szamana i/lub z 777-Economy (ktory wyplaca sama podstawe).
 * Dodatkowo: Perk Szczesciarza (dodatkowy lup przy rudach).
 */
public final class EconomyListener implements Listener {

    private final SzamanPlugin plugin;

    public EconomyListener(SzamanPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent e) {
        FileConfiguration cfg = plugin.getConfig();
        boolean cancelled = e.isCancelled();
        if (cancelled && !cfg.getBoolean("turbo-drop.count-cancelled", false)) {
            return;
        }
        Player player = e.getPlayer();
        GameMode gm = player.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) {
            return;
        }

        UUID id = player.getUniqueId();
        Material type = e.getBlock().getType();
        ItemStack tool = player.getInventory().getItemInMainHand();
        double price = plugin.prices().price(type, tool);

        // ---------- Turbo Drop ----------
        if (price > 0) {
            double total = plugin.turbo().total(id);
            if (total > 0) {
                double bonus = price * total;
                if (plugin.deposit(player, bonus) && cfg.getBoolean("turbo-drop.actionbar", false)) {
                    Txt.actionbar(player, plugin.msg().text("turbo-pay",
                            "%money%", Txt.num(bonus), "%total%", Txt.num(total)));
                }
            }
        }

        // ---------- Perk Szczesciarza ----------
        if (cancelled) {
            return;
        }
        double lucky = plugin.perks().value(id, PerkType.LUCKY);
        if (lucky <= 0 || !isLuckyBlock(type) || !PerkService.chance(lucky)) {
            return;
        }
        int times = Math.max(1, cfg.getInt("effects.lucky.extra-drop-multiplier", 1));
        if (e.isDropItems()) {
            Collection<ItemStack> drops = e.getBlock().getDrops(tool, player);
            if (drops.isEmpty()) {
                return;
            }
            Location loc = e.getBlock().getLocation().add(0.5, 0.5, 0.5);
            for (int i = 0; i < times; i++) {
                for (ItemStack drop : drops) {
                    loc.getWorld().dropItemNaturally(loc, drop.clone());
                }
            }
            Txt.actionbar(player, plugin.msg().text("lucky"));
        } else if (price > 0) {
            // drop wylaczony (np. przez 777-Economy) -> dodatkowa wyplata zamiast lupu
            double money = price * times;
            if (plugin.deposit(player, money)) {
                Txt.actionbar(player, plugin.msg().text("lucky-money", "%money%", Txt.num(money)));
            }
        }
    }

    private boolean isLuckyBlock(Material m) {
        List<String> configured = plugin.getConfig().getStringList("effects.lucky.materials");
        if (!configured.isEmpty()) {
            for (String s : configured) {
                if (s.equalsIgnoreCase(m.name())) {
                    return true;
                }
            }
            return false;
        }
        return PerkListener.isOre(m);
    }
}
