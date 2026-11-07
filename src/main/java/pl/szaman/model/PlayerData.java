package pl.szaman.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerData {

    private final UUID uuid;
    private double turbo;
    private final Map<PerkType, Double> perks = new EnumMap<>(PerkType.class);
    private boolean dirty;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public double turbo() {
        return turbo;
    }

    public void setTurbo(double v) {
        this.turbo = Math.max(0, v);
        this.dirty = true;
    }

    public double perk(PerkType t) {
        Double v = perks.get(t);
        return v == null ? 0.0 : v;
    }

    public void setPerk(PerkType t, double v) {
        if (v <= 0) {
            perks.remove(t);
        } else {
            perks.put(t, v);
        }
        this.dirty = true;
    }

    public boolean has(PerkType t) {
        return perk(t) > 0;
    }

    public Map<PerkType, Double> perks() {
        return perks;
    }

    public void reset() {
        turbo = 0;
        perks.clear();
        dirty = true;
    }

    public boolean dirty() {
        return dirty;
    }

    public void clean() {
        dirty = false;
    }
}
