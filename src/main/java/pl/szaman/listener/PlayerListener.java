package pl.szaman.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.szaman.SzamanPlugin;
import pl.szaman.model.PlayerData;

public final class PlayerListener implements Listener {

    private final SzamanPlugin plugin;

    public PlayerListener(SzamanPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.data().get(e.getPlayer().getUniqueId());
        plugin.turbo().invalidateCache();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        PlayerData d = plugin.data().get(e.getPlayer().getUniqueId());
        if (d.dirty()) {
            plugin.data().save(d);
        }
        plugin.perks().clearState(e.getPlayer().getUniqueId());
    }
}
