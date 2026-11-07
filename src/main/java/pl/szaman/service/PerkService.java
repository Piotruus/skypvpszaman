package pl.szaman.service;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import pl.szaman.SzamanPlugin;
import pl.szaman.model.Category;
import pl.szaman.model.PerkType;
import pl.szaman.model.PlayerData;
import pl.szaman.util.Txt;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Losowanie perkow z kamieni, animacja oraz stan chwilowych blokad (debuffow). */
public final class PerkService {

    /** Rodzaje blokad nakladanych na przeciwnika. */
    public enum Debuff {
        HEAL, FIREWORK, BREAK, EVENT
    }

    private static final int[] FRAME_DELAYS = {2, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 5, 6};

    private final SzamanPlugin plugin;
    private final Map<UUID, Map<Debuff, Long>> debuffs = new HashMap<>();
    private final Map<UUID, Long> secondLifeReadyAt = new HashMap<>();
    private final Set<UUID> animating = new HashSet<>();

    public PerkService(SzamanPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ wartosci

    public double value(UUID id, PerkType type) {
        return plugin.data().get(id).perk(type);
    }

    public double min(PerkType t) {
        FileConfiguration c = plugin.getConfig();
        return c.getDouble("perks." + t.id() + ".min", c.getDouble("perks.default.min", 3.0));
    }

    public double max(PerkType t) {
        FileConfiguration c = plugin.getConfig();
        return c.getDouble("perks." + t.id() + ".max", c.getDouble("perks.default.max", 10.0));
    }

    public double cap(PerkType t) {
        FileConfiguration c = plugin.getConfig();
        return c.getDouble("perks." + t.id() + ".cap", c.getDouble("perks.default.cap", 70.0));
    }

    /** Czy wypadl proc o zadanej szansie w procentach. */
    public static boolean chance(double percent) {
        return percent > 0 && ThreadLocalRandom.current().nextDouble() * 100.0 < percent;
    }

    // ------------------------------------------------------------------ losowanie perku z kamienia

    public boolean isAnimating(UUID id) {
        return animating.contains(id);
    }

    /**
     * Uzycie kamienia (PPM). Losuje perk z kategorii, dodaje bonus, zabiera kamien z reki i odpala animacje.
     * Wynik jest zapisany od razu - animacja jest tylko pokazem.
     */
    public void useStone(Player player, Category category) {
        UUID id = player.getUniqueId();
        if (animating.contains(id)) {
            plugin.msg().send(player, "animating");
            return;
        }
        PlayerData data = plugin.data().get(id);

        List<PerkType> candidates = new ArrayList<>();
        for (PerkType t : PerkType.of(category)) {
            if (data.perk(t) < cap(t) - 1e-9) {
                candidates.add(t);
            }
        }
        if (candidates.isEmpty()) {
            plugin.msg().send(player, "stone-maxed", "%color%", category.color(), "%category%", category.polishName());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1f);
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (plugin.stones().categoryOf(hand) != category) {
            return;
        }

        PerkType picked = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        double lo = min(picked);
        double hi = Math.max(lo, max(picked));
        double added = lo + ThreadLocalRandom.current().nextDouble() * (hi - lo);
        added = Math.round(added * 10.0) / 10.0;
        double cap = cap(picked);
        double before = data.perk(picked);
        double after = Math.min(cap, before + added);
        added = Math.round((after - before) * 10.0) / 10.0;

        // zabierz kamien
        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
            player.getInventory().setItemInMainHand(hand);
        }

        data.setPerk(picked, after);
        plugin.data().save(data);

        animate(player, category, picked, added, after, cap);
    }

    private void animate(Player player, Category category, PerkType picked, double added, double total, double cap) {
        UUID id = player.getUniqueId();
        animating.add(id);
        step(player, category, picked, added, total, cap, 0, null);
    }

    private void step(Player player, Category category, PerkType picked, double added, double total, double cap,
                      int frame, PerkType last) {
        UUID id = player.getUniqueId();
        if (!player.isOnline()) {
            animating.remove(id);
            return;
        }

        if (frame >= FRAME_DELAYS.length) {
            // wynik
            player.sendTitle(Txt.color(category.color() + picked.title()),
                    Txt.color("&a+" + Txt.pct(added) + "% &8| &f" + Txt.pct(total) + "%&7/&f" + Txt.pct(cap) + "%"),
                    0, 50, 15);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.1f);
            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1.0, 0), 40, 0.5, 0.7, 0.5, 0.2);
            plugin.msg().send(player, "perk-rolled",
                    "%color%", category.color(),
                    "%perk%", picked.title(),
                    "%added%", Txt.pct(added),
                    "%value%", Txt.pct(total),
                    "%cap%", Txt.pct(cap));
            animating.remove(id);
            plugin.menus().refreshIfPerksOpen(player);
            return;
        }

        List<PerkType> pool = PerkType.of(category);
        PerkType shown;
        int guard = 0;
        do {
            shown = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            guard++;
        } while (shown == last && pool.size() > 1 && guard < 10);

        player.sendTitle(Txt.color(category.color() + shown.title()), Txt.color("&7Losowanie..."),
                0, FRAME_DELAYS[frame] + 4, 0);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.8f + frame * 0.05f);

        final PerkType shownFinal = shown;
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> step(player, category, picked, added, total, cap, frame + 1, shownFinal),
                FRAME_DELAYS[frame]);
    }

    // ------------------------------------------------------------------ blokady (debuffy)

    public void applyDebuff(Player victim, Debuff debuff, int seconds) {
        long until = System.currentTimeMillis() + seconds * 1000L;
        debuffs.computeIfAbsent(victim.getUniqueId(), k -> new EnumMap<>(Debuff.class)).put(debuff, until);
    }

    public boolean isDebuffed(UUID id, Debuff debuff) {
        Map<Debuff, Long> m = debuffs.get(id);
        if (m == null) {
            return false;
        }
        Long until = m.get(debuff);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            m.remove(debuff);
            if (m.isEmpty()) {
                debuffs.remove(id);
            }
            return false;
        }
        return true;
    }

    public void clearState(UUID id) {
        debuffs.remove(id);
        animating.remove(id);
    }

    // ------------------------------------------------------------------ drugie zycie

    public boolean secondLifeReady(UUID id) {
        Long at = secondLifeReadyAt.get(id);
        return at == null || System.currentTimeMillis() >= at;
    }

    public void startSecondLifeCooldown(UUID id) {
        int seconds = plugin.getConfig().getInt("effects.second-life.cooldown-seconds", 180);
        secondLifeReadyAt.put(id, System.currentTimeMillis() + seconds * 1000L);
    }

    // ------------------------------------------------------------------ pomocnicze

    /** Efekt mikstury po nazwie nowej lub starej (zmiany nazw w 1.20.5). */
    public static PotionEffectType effect(String... names) {
        for (String n : names) {
            PotionEffectType t = PotionEffectType.getByName(n);
            if (t != null) {
                return t;
            }
        }
        return null;
    }
}
