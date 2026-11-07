package pl.szaman.model;

import org.bukkit.Material;
import pl.szaman.util.Txt;

import java.util.Locale;

/** Kategoria perkow = jedna kolumna w menu TWOJE PERKI + jeden rodzaj kamienia. */
public enum Category {
    STRENGTH("strength", "Siły", "&c", Material.RED_DYE, 0),
    RESISTANCE("resistance", "Odporności", "&9", Material.BLUE_DYE, 2),
    DEXTERITY("dexterity", "Zręczności", "&e", Material.YELLOW_DYE, 4),
    INTELLIGENCE("intelligence", "Inteligencji", "&a", Material.LIME_DYE, 6),
    MAGIC("magic", "Magii", "&5", Material.PURPLE_DYE, 8);

    private final String id;
    private final String polishName;
    private final String color;
    private final Material defaultStone;
    private final int column;

    Category(String id, String polishName, String color, Material defaultStone, int column) {
        this.id = id;
        this.polishName = polishName;
        this.color = color;
        this.defaultStone = defaultStone;
        this.column = column;
    }

    public String id() {
        return id;
    }

    /** np. "Siły" */
    public String polishName() {
        return polishName;
    }

    /** kod koloru z ampersandem, np. "&c" */
    public String color() {
        return color;
    }

    public Material defaultStone() {
        return defaultStone;
    }

    /** kolumna w menu perkow (0,2,4,6,8) */
    public int column() {
        return column;
    }

    public String stoneName() {
        return "Kamień " + polishName;
    }

    public static Category byName(String input) {
        if (input == null) {
            return null;
        }
        String n = Txt.norm(input);
        for (Category c : values()) {
            if (c.id.equals(n) || Txt.norm(c.polishName).equals(n)) {
                return c;
            }
        }
        return null;
    }

    public static Category byId(String id) {
        if (id == null) {
            return null;
        }
        String n = id.toLowerCase(Locale.ROOT);
        for (Category c : values()) {
            if (c.id.equals(n)) {
                return c;
            }
        }
        return null;
    }
}
