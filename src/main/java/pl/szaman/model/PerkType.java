package pl.szaman.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 15 perkow - po 3 w kazdej kategorii (kolejnosc = rzad w menu). */
public enum PerkType {
    // ---- SILY (czerwony) ----
    EXTRA_HIT("extra_hit", Category.STRENGTH, "Perk Extra Hit",
            "że każde {c}trafienie&f ma szansę", "na {c}podwójne uderzenie"),
    INCREASED_DAMAGE("increased_damage", Category.STRENGTH, "Perk Zwiększonych Obrażeń",
            "że każde {c}trafienie&f w walce", "zadaje {c}zwiększone obrażenia"),
    LIFESTEAL("lifesteal", Category.STRENGTH, "Perk Lifesteal",
            "że każde {c}trafienie&f ma szansę", "na {c}przywrócenie zdrowia"),

    // ---- ODPORNOSCI (niebieski) ----
    RESISTANCE("resistance", Category.RESISTANCE, "Perk Odporności",
            "że {c}otrzymujesz mniejsze obrażenia&f", "od {c}innych graczy"),
    LIFE_BLOCK("life_block", Category.RESISTANCE, "Perk Blokady Życia",
            "że każde {c}trafienie&f ma szansę", "{c}zablokować leczenie&f przeciwnika"),
    HIT_BLOCK("hit_block", Category.RESISTANCE, "Perk Blokady Uderzenia",
            "że uderzenie przeciwnika ma szansę", "zostać {c}całkowicie zablokowane"),

    // ---- ZRECZNOSCI (zolty) ----
    ENTANGLE("entangle", Category.DEXTERITY, "Perk Spętania",
            "że każde {c}trafienie&f ma szansę", "{c}spętać&f przeciwnika"),
    ENDURANCE("endurance", Category.DEXTERITY, "Perk Wytrzymałości",
            "że Twój ekwipunek", "{c}zużywa się wolniej"),
    HIGHLIGHT("highlight", Category.DEXTERITY, "Perk Podświetlenia",
            "że każde {c}trafienie&f ma szansę", "{c}podświetlić&f przeciwnika"),

    // ---- INTELIGENCJI (zielony) ----
    FIREWORK_BLOCK("firework_block", Category.INTELLIGENCE, "Perk Blokady Fajerwerki",
            "że każde {c}trafienie&f ma szansę", "{c}zablokować fajerwerki&f przeciwnika"),
    LUCKY("lucky", Category.INTELLIGENCE, "Perk Szczęściarza",
            "że podczas {c}kopania&f masz szansę", "na {c}dodatkowy łup"),
    BLOCK_BREAK_BLOCK("block_break_block", Category.INTELLIGENCE, "Perk Blokady Niszczenia Bloków",
            "że każde {c}trafienie&f ma szansę", "{c}zablokować niszczenie bloków&f"),

    // ---- MAGII (fioletowy) ----
    SECOND_LIFE("second_life", Category.MAGIC, "Perk Drugiego Życia",
            "że {c}śmiertelny cios&f ma szansę", "nie być {c}śmiertelny"),
    REFLECT("reflect", Category.MAGIC, "Perk Odbicia",
            "że część {c}otrzymanych obrażeń", "{c}odbija się&f w przeciwnika"),
    EVENT_BLOCK("event_block", Category.MAGIC, "Perk Blokady Eventówki",
            "że każde {c}trafienie&f ma szansę", "{c}zablokować eventówkę&f przeciwnika");

    private final String id;
    private final Category category;
    private final String title;
    private final String[] description;

    PerkType(String id, Category category, String title, String... description) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public Category category() {
        return category;
    }

    /** np. "Perk Extra Hit" */
    public String title() {
        return title;
    }

    /** linie opisu; {c} = kolor kategorii */
    public String[] description() {
        return description;
    }

    /** rzad w menu (0..2) w obrebie kategorii */
    public int row() {
        return of(category).indexOf(this);
    }

    public static List<PerkType> of(Category c) {
        List<PerkType> out = new ArrayList<>(3);
        for (PerkType p : values()) {
            if (p.category == c) {
                out.add(p);
            }
        }
        return out;
    }

    public static PerkType byId(String id) {
        if (id == null) {
            return null;
        }
        String n = id.toLowerCase(Locale.ROOT).replace('-', '_');
        for (PerkType p : values()) {
            if (p.id.equals(n)) {
                return p;
            }
        }
        return null;
    }
}
