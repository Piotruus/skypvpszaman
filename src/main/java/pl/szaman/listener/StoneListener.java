package pl.szaman.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import pl.szaman.SzamanPlugin;
import pl.szaman.model.Category;

/** PPM z kamieniem w rece = losowanie perku. */
public final class StoneListener implements Listener {

    private final SzamanPlugin plugin;

    public StoneListener(SzamanPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = e.getItem();
        Category category = plugin.stones().categoryOf(item);
        if (category == null) {
            return;
        }
        e.setCancelled(true);
        plugin.perks().useStone(e.getPlayer(), category);
    }
}
