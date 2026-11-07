package pl.szaman.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import pl.szaman.SzamanPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Integracja z 777-Clans przez refleksje (plugin nie udostepnia publicznego API).
 * Czytamy prywatne pole "clanService" i wolamy getClanByMember(UUID).
 */
public final class ClanHook {

    private final SzamanPlugin plugin;
    private Object service;
    private Method getClanByMember;
    private Method clanMembers;
    private Method memberUuid;
    private boolean warned;

    public ClanHook(SzamanPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean init() {
        if (service != null && getClanByMember != null) {
            return true;
        }
        Plugin clans = Bukkit.getPluginManager().getPlugin("777-Clans");
        if (clans == null || !clans.isEnabled()) {
            return false;
        }
        try {
            Field f = clans.getClass().getDeclaredField("clanService");
            f.setAccessible(true);
            Object s = f.get(clans);
            if (s == null) {
                return false;
            }
            Method m = s.getClass().getMethod("getClanByMember", UUID.class);
            m.setAccessible(true);
            service = s;
            getClanByMember = m;
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                plugin.getLogger().warning("Nie udało się połączyć z 777-Clans (" + e + "). Bonus klanowy będzie wyłączony.");
            }
            return false;
        }
    }

    public boolean isAvailable() {
        return init();
    }

    /** Wszyscy czlonkowie klanu gracza (razem z nim). Pusty zbior, gdy gracz nie ma klanu. */
    public Set<UUID> getClanMembers(UUID player) {
        if (!init()) {
            return Collections.emptySet();
        }
        try {
            Object result = getClanByMember.invoke(service, player);
            if (!(result instanceof Optional<?> opt) || opt.isEmpty()) {
                return Collections.emptySet();
            }
            Object clan = opt.get();
            if (clanMembers == null) {
                clanMembers = clan.getClass().getMethod("members");
                clanMembers.setAccessible(true);
            }
            Object list = clanMembers.invoke(clan);
            if (!(list instanceof List<?> members)) {
                return Collections.emptySet();
            }
            Set<UUID> out = new HashSet<>();
            for (Object member : members) {
                if (memberUuid == null) {
                    memberUuid = member.getClass().getMethod("uuid");
                    memberUuid.setAccessible(true);
                }
                Object u = memberUuid.invoke(member);
                if (u instanceof UUID id) {
                    out.add(id);
                }
            }
            return out;
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                plugin.getLogger().warning("Błąd odczytu klanu z 777-Clans: " + e);
            }
            return Collections.emptySet();
        }
    }
}
