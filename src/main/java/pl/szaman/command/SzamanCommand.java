package pl.szaman.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.szaman.SzamanPlugin;

public final class SzamanCommand implements CommandExecutor {

    private final SzamanPlugin plugin;

    public SzamanCommand(SzamanPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg().send(sender, "only-player");
            return true;
        }
        if (!player.hasPermission("szaman.use")) {
            plugin.msg().send(player, "no-permission");
            return true;
        }
        plugin.menus().openMain(player);
        return true;
    }
}
