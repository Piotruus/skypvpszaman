package pl.szaman.service;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.szaman.SzamanPlugin;
import pl.szaman.model.PlayerData;
import pl.szaman.util.Txt;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Perk klanowy TURBO DROP: losowanie mnoznika i liczenie bonusu (twoj + klanowy). */
public final class TurboService {

    private static final long CACHE_MS = 3000L;

    private record Cached(double value, long at) {
    }

    private final SzamanPlugin plugin;
    private final Map<UUID, Cached> clanCache = new HashMap<>();

    public TurboService(SzamanPlugin plugin) {
        this.plugin = plugin;
    }

    public double personal(UUID id) {
        return plugin.data().get(id).turbo();
    }

    /** Suma bonusow wszystkich POZOSTALYCH czlonkow klanu (bez gracza). */
    public double clanBonus(UUID id) {
        long now = System.currentTimeMillis();
        Cached c = clanCache.get(id);
        if (c != null && now - c.at() < CACHE_MS) {
            return c.value();
        }
        double sum = 0;
        Set<UUID> members = plugin.clans().getClanMembers(id);
        for (UUID m : members) {
            if (!m.equals(id)) {
                sum += plugin.data().get(m).turbo();
            }
        }
        clanCache.put(id, new Cached(sum, now));
        return sum;
    }

    public double total(UUID id) {
        double total = personal(id) + clanBonus(id);
        double max = plugin.getConfig().getDouble("turbo-drop.max-total", 0);
        if (max > 0) {
            total = Math.min(total, max);
        }
        return total;
    }

    public void invalidateCache() {
        clanCache.clear();
    }

    /** Losuje mnoznik wg wag z configu. */
    public double randomMultiplier() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("turbo-drop.chances");
        double sum = 0;
        Map<Double, Double> table = new java.util.LinkedHashMap<>();
        if (sec != null) {
            for (String k : sec.getKeys(false)) {
                try {
                    double mult = Double.parseDouble(k.replace(',', '.'));
                    double w = Math.max(0, sec.getDouble(k));
                    if (w > 0 && mult > 0) {
                        table.put(mult, w);
                        sum += w;
                    }
                } catch (NumberFormatException ignored) {
                    plugin.getLogger().warning("Niepoprawny mnożnik w turbo-drop.chances: " + k);
                }
            }
        }
        if (table.isEmpty()) {
            return 0.25;
        }
        double r = ThreadLocalRandom.current().nextDouble() * sum;
        for (Map.Entry<Double, Double> e : table.entrySet()) {
            r -= e.getValue();
            if (r < 0) {
                return e.getKey();
            }
        }
        return table.keySet().iterator().next();
    }

    /** Klikniecie koniczynki. Zwraca true, jesli losowanie sie odbylo (odlamki pobrane). */
    public boolean roll(Player player) {
        int cost = Math.max(0, plugin.getConfig().getInt("costs.turbo-drop", 5));
        ShardService shards = plugin.shards();
        int have = shards.count(player);
        if (have < cost || !shards.take(player, cost)) {
            plugin.msg().send(player, "not-enough-shards", "%cost%", String.valueOf(cost), "%have%", String.valueOf(have));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1f);
            return false;
        }

        double rolled = randomMultiplier();
        PlayerData data = plugin.data().get(player.getUniqueId());
        double old = data.turbo();
        String mode = plugin.getConfig().getString("turbo-drop.reroll-mode", "BEST").toUpperCase(Locale.ROOT);
        double updated;
        switch (mode) {
            case "REPLACE" -> updated = rolled;
            case "ADD" -> updated = old + rolled;
            default -> updated = Math.max(old, rolled);
        }
        boolean better = updated != old;
        if (better) {
            data.setTurbo(updated);
            plugin.data().save(data);
        }
        invalidateCache();

        UUID id = player.getUniqueId();
        if (better) {
            plugin.msg().send(player, "turbo-rolled",
                    "%rolled%", Txt.num(rolled),
                    "%personal%", Txt.num(personal(id)),
                    "%clan%", Txt.num(clanBonus(id)),
                    "%total%", Txt.num(total(id)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
        } else {
            plugin.msg().send(player, "turbo-not-better",
                    "%rolled%", Txt.num(rolled),
                    "%personal%", Txt.num(old));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
        }
        return true;
    }
}
