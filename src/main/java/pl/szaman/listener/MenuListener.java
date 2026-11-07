package pl.szaman.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import pl.szaman.SzamanPlugin;
import pl.szaman.gui.MenuHolder;
import pl.szaman.gui.Menus;

public final class MenuListener implements Listener {

    private final SzamanPlugin plugin;

    public MenuListener(SzamanPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder holder)) {
            return;
        }
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (e.getClickedInventory() != top) {
            return;
        }
        int slot = e.getRawSlot();
        Menus menus = plugin.menus();

        if (holder.type() == MenuHolder.Type.MAIN) {
            switch (slot) {
                case Menus.SLOT_TURBO -> {
                    plugin.turbo().roll(player);
                    menus.refreshMain(player);
                }
                case Menus.SLOT_PERKS -> menus.openPerks(player);
                case Menus.SLOT_STONE -> {
                    if (e.isLeftClick()) {
                        plugin.stones().rollStones(player, e.isShiftClick() ? 10 : 1);
                        menus.refreshMain(player);
                    }
                }
                default -> {
                }
            }
        } else if (holder.type() == MenuHolder.Type.PERKS) {
            if (slot == Menus.SLOT_BACK) {
                menus.openMain(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            e.setCancelled(true);
        }
    }
}
