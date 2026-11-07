package pl.szaman.listener;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.szaman.SzamanPlugin;
import pl.szaman.model.PerkType;
import pl.szaman.service.PerkService;
import pl.szaman.service.PerkService.Debuff;
import pl.szaman.util.Txt;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Dzialanie wszystkich perkow z kamieni (walka, obrona, blokady, wytrzymalosc). */
public final class PerkListener implements Listener {

    private final SzamanPlugin plugin;

    public PerkListener(SzamanPlugin plugin) {
        this.plugin = plugin;
    }

    private Player attackerOf(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }

    // ====================================================================== WALKA

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        Player attacker = attackerOf(e.getDamager());
        Player victimPlayer = victim instanceof Player vp ? vp : null;
        boolean pvp = attacker != null && victimPlayer != null && !attacker.equals(victimPlayer);

        FileConfiguration cfg = plugin.getConfig();
        PerkService ps = plugin.perks();
        boolean playersOnly = cfg.getBoolean("effects.players-only", true);

        // 1. Perk Blokady Uderzenia (obronca)
        if (pvp) {
            double block = ps.value(victimPlayer.getUniqueId(), PerkType.HIT_BLOCK);
            if (PerkService.chance(block)) {
                e.setCancelled(true);
                Txt.actionbar(victimPlayer, plugin.msg().text("hit-blocked"));
                Txt.actionbar(attacker, plugin.msg().text("hit-blocked-attacker"));
                victimPlayer.playSound(victimPlayer.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
                return;
            }
        }

        double dmg = e.getDamage();
        boolean offensive = attacker != null && !attacker.equals(victim) && (!playersOnly || victimPlayer != null);

        // 2. Perki ofensywne: Extra Hit + Zwiekszone Obrazenia
        if (offensive) {
            UUID aid = attacker.getUniqueId();
            if (PerkService.chance(ps.value(aid, PerkType.EXTRA_HIT))) {
                dmg *= Math.max(1.0, cfg.getDouble("effects.extra-hit-multiplier", 2.0));
                Txt.actionbar(attacker, plugin.msg().text("extra-hit"));
                victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1.0, 0), 25, 0.3, 0.5, 0.3, 0.15);
            }
            double inc = ps.value(aid, PerkType.INCREASED_DAMAGE);
            if (inc > 0) {
                dmg *= 1.0 + inc / 100.0;
            }
        }

        // 3. Perk Odpornosci (obronca)
        if (pvp) {
            double res = ps.value(victimPlayer.getUniqueId(), PerkType.RESISTANCE);
            if (res > 0) {
                dmg *= Math.max(0.0, 1.0 - Math.min(res, 90.0) / 100.0);
            }
        }
        e.setDamage(dmg);

        // 4. Perk Odbicia (obronca) - czesc obrazen wraca do atakujacego
        if (pvp && dmg > 0) {
            double refl = ps.value(victimPlayer.getUniqueId(), PerkType.REFLECT);
            if (refl > 0) {
                double back = dmg * Math.min(refl, 100.0) / 100.0;
                if (back > 0.01) {
                    attacker.damage(back);
                }
            }
        }

        // 5. Proc'y atakujacego
        if (offensive) {
            UUID aid = attacker.getUniqueId();
            if (PerkService.chance(ps.value(aid, PerkType.LIFESTEAL))) {
                double heal = cfg.getDouble("effects.lifesteal-heal", 2.0);
                double max = attacker.getMaxHealth();
                if (attacker.getHealth() < max) {
                    attacker.setHealth(Math.min(max, attacker.getHealth() + heal));
                    Txt.actionbar(attacker, plugin.msg().text("lifesteal", "%hp%", Txt.num(heal)));
                }
            }
        }

        // 6. Blokady/debuffy - tylko na graczach (PvP)
        if (pvp) {
            UUID aid = attacker.getUniqueId();

            if (PerkService.chance(ps.value(aid, PerkType.LIFE_BLOCK))) {
                int sec = cfg.getInt("effects.heal-block-seconds", 5);
                ps.applyDebuff(victimPlayer, Debuff.HEAL, sec);
                Txt.actionbar(victimPlayer, plugin.msg().text("heal-blocked", "%seconds%", String.valueOf(sec)));
            }
            if (PerkService.chance(ps.value(aid, PerkType.ENTANGLE))) {
                int sec = cfg.getInt("effects.entangle-seconds", 3);
                int level = Math.max(1, cfg.getInt("effects.entangle-slow-level", 3));
                PotionEffectType slow = PerkService.effect("SLOWNESS", "SLOW");
                PotionEffectType jump = PerkService.effect("JUMP_BOOST", "JUMP");
                if (slow != null) {
                    victimPlayer.addPotionEffect(new PotionEffect(slow, sec * 20, level - 1, false, true));
                }
                if (jump != null) {
                    victimPlayer.addPotionEffect(new PotionEffect(jump, sec * 20, 128, false, false));
                }
            }
            if (PerkService.chance(ps.value(aid, PerkType.HIGHLIGHT))) {
                int sec = cfg.getInt("effects.glow-seconds", 6);
                PotionEffectType glow = PerkService.effect("GLOWING");
                if (glow != null) {
                    victimPlayer.addPotionEffect(new PotionEffect(glow, sec * 20, 0, false, false));
                }
            }
            if (PerkService.chance(ps.value(aid, PerkType.FIREWORK_BLOCK))) {
                int sec = cfg.getInt("effects.firework-block-seconds", 6);
                ps.applyDebuff(victimPlayer, Debuff.FIREWORK, sec);
                Txt.actionbar(victimPlayer, plugin.msg().text("firework-blocked", "%seconds%", String.valueOf(sec)));
            }
            if (PerkService.chance(ps.value(aid, PerkType.BLOCK_BREAK_BLOCK))) {
                int sec = cfg.getInt("effects.block-break-block-seconds", 6);
                ps.applyDebuff(victimPlayer, Debuff.BREAK, sec);
                Txt.actionbar(victimPlayer, plugin.msg().text("break-blocked", "%seconds%", String.valueOf(sec)));
            }
            if (PerkService.chance(ps.value(aid, PerkType.EVENT_BLOCK))) {
                int sec = cfg.getInt("effects.event-block-seconds", 6);
                ps.applyDebuff(victimPlayer, Debuff.EVENT, sec);
                Txt.actionbar(victimPlayer, plugin.msg().text("event-blocked", "%seconds%", String.valueOf(sec)));
            }
        }
    }

    /** Perk Drugiego Zycia - smiertelny cios moze nie zabic. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) {
            return;
        }
        PerkService ps = plugin.perks();
        UUID id = player.getUniqueId();
        double value = ps.value(id, PerkType.SECOND_LIFE);
        if (value <= 0) {
            return;
        }
        if (e.getFinalDamage() < player.getHealth()) {
            return;
        }
        if (!ps.secondLifeReady(id) || !PerkService.chance(value)) {
            return;
        }

        FileConfiguration cfg = plugin.getConfig();
        e.setCancelled(true);
        ps.startSecondLifeCooldown(id);

        double hp = Math.max(1.0, Math.min(player.getMaxHealth(), cfg.getDouble("effects.second-life.health", 6.0)));
        player.setHealth(hp);
        player.setFireTicks(0);
        PotionEffectType resistance = PerkService.effect("RESISTANCE", "DAMAGE_RESISTANCE");
        if (resistance != null) {
            int sec = cfg.getInt("effects.second-life.resistance-seconds", 4);
            player.addPotionEffect(new PotionEffect(resistance, sec * 20, 2, false, true));
        }
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1.0, 0), 60, 0.6, 0.9, 0.6, 0.3);
        Txt.actionbar(player, plugin.msg().text("second-life"));
    }

    // ====================================================================== BLOKADY

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent e) {
        if (e.getEntity() instanceof Player p && plugin.perks().isDebuffed(p.getUniqueId(), Debuff.HEAL)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = e.getItem();
        if (item == null) {
            return;
        }
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        PerkService ps = plugin.perks();

        if (item.getType() == Material.FIREWORK_ROCKET && p.isGliding() && ps.isDebuffed(id, Debuff.FIREWORK)) {
            e.setCancelled(true);
            Txt.actionbar(p, plugin.msg().text("still-blocked"));
            return;
        }
        if (ps.isDebuffed(id, Debuff.EVENT) && isEventItem(item)) {
            e.setCancelled(true);
            Txt.actionbar(p, plugin.msg().text("still-blocked"));
        }
    }

    private boolean isEventItem(ItemStack item) {
        FileConfiguration cfg = plugin.getConfig();
        List<String> materials = cfg.getStringList("effects.eventowka.materials");
        for (String m : materials) {
            if (m.equalsIgnoreCase(item.getType().name())) {
                return true;
            }
        }
        String contains = cfg.getString("effects.eventowka.name-contains", "");
        if (contains != null && !contains.isEmpty() && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return Txt.norm(meta.getDisplayName()).contains(Txt.norm(contains));
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreakBlocked(BlockBreakEvent e) {
        if (plugin.perks().isDebuffed(e.getPlayer().getUniqueId(), Debuff.BREAK)) {
            e.setCancelled(true);
            Txt.actionbar(e.getPlayer(), plugin.msg().text("still-blocked"));
        }
    }

    // ====================================================================== WYTRZYMALOSC

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent e) {
        double v = plugin.perks().value(e.getPlayer().getUniqueId(), PerkType.ENDURANCE);
        if (PerkService.chance(v)) {
            e.setCancelled(true);
        }
    }

    /** Pomocnicze: czy nazwa materialu to ruda (do Szczesciarza). */
    static boolean isOre(Material m) {
        String n = m.name().toUpperCase(Locale.ROOT);
        return n.endsWith("_ORE") || n.equals("ANCIENT_DEBRIS");
    }
}
