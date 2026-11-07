package pl.szaman.service;

import org.bukkit.command.CommandSender;
import pl.szaman.SzamanPlugin;
import pl.szaman.util.Txt;

/** Wiadomosci z configu (sekcja messages). Uzycie: Msg.send(p, "key", "%placeholder%", "wartosc", ...) */
public final class Msg {

    private final SzamanPlugin plugin;

    public Msg(SzamanPlugin plugin) {
        this.plugin = plugin;
    }

    public String raw(String key, String... replacements) {
        String s = plugin.getConfig().getString("messages." + key, "&c[brak wiadomosci: " + key + "]");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            s = s.replace(replacements[i], replacements[i + 1]);
        }
        return s;
    }

    public String text(String key, String... replacements) {
        return Txt.color(raw(key, replacements));
    }

    /** Wiadomosc na czacie z prefiksem. */
    public void send(CommandSender to, String key, String... replacements) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8» ");
        to.sendMessage(Txt.color(prefix + raw(key, replacements)));
    }
}
