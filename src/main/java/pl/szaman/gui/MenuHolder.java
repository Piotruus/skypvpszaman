package pl.szaman.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Znacznik naszych menu - po nim rozpoznajemy inventory w listenerach. */
public final class MenuHolder implements InventoryHolder {

    public enum Type {
        MAIN, PERKS
    }

    private final Type type;
    private Inventory inventory;

    public MenuHolder(Type type) {
        this.type = type;
    }

    public Type type() {
        return type;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
