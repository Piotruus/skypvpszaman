package pl.szaman.service;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.szaman.SzamanPlugin;
import pl.szaman.model.PerkType;
import pl.szaman.model.PlayerData;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Zapis danych graczy: plugins/Szaman/data/<uuid>.yml (z cache w pamieci). */
public final class DataManager {

    private final SzamanPlugin plugin;
    private final File dir;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public DataManager(SzamanPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "data");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Nie można utworzyć folderu data/");
        }
    }

    private File file(UUID id) {
        return new File(dir, id + ".yml");
    }

    public PlayerData get(UUID id) {
        PlayerData d = cache.get(id);
        if (d == null) {
            d = load(id);
            PlayerData prev = cache.putIfAbsent(id, d);
            if (prev != null) {
                d = prev;
            }
        }
        return d;
    }

    private PlayerData load(UUID id) {
        PlayerData d = new PlayerData(id);
        File f = file(id);
        if (f.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            d.setTurbo(y.getDouble("turbo", 0.0));
            ConfigurationSection s = y.getConfigurationSection("perks");
            if (s != null) {
                for (String key : s.getKeys(false)) {
                    PerkType t = PerkType.byId(key);
                    if (t != null) {
                        d.setPerk(t, s.getDouble(key));
                    }
                }
            }
            d.clean();
        }
        return d;
    }

    public void save(PlayerData d) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("turbo", d.turbo());
        for (Map.Entry<PerkType, Double> e : d.perks().entrySet()) {
            y.set("perks." + e.getKey().id(), e.getValue());
        }
        try {
            y.save(file(d.uuid()));
            d.clean();
        } catch (IOException ex) {
            plugin.getLogger().severe("Nie udało się zapisać danych " + d.uuid() + ": " + ex.getMessage());
        }
    }

    public void saveDirty() {
        for (PlayerData d : cache.values()) {
            if (d.dirty()) {
                save(d);
            }
        }
    }
}
