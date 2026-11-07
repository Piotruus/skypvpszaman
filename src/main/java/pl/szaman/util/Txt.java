package pl.szaman.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Txt {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private Txt() {
    }

    /** Zamienia &a oraz &#RRGGBB na kody kolorow. */
    public static String color(String s) {
        if (s == null) {
            return "";
        }
        Matcher m = HEX.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            StringBuilder r = new StringBuilder("\u00A7x");
            for (char c : m.group(1).toCharArray()) {
                r.append('\u00A7').append(c);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(r.toString()));
        }
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static List<String> color(List<String> lines) {
        List<String> out = new ArrayList<>(lines.size());
        for (String l : lines) {
            out.add(color(l));
        }
        return out;
    }

    /** Usuwa wszystkie kolory z tekstu. */
    public static String strip(String s) {
        if (s == null) {
            return "";
        }
        return ChatColor.stripColor(color(s));
    }

    /** Male litery, bez polskich znakow i kolorow - do porownywania nazw. */
    public static String norm(String s) {
        String t = strip(s).toLowerCase(Locale.ROOT).replace('ł', 'l');
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return t.trim();
    }

    public static void actionbar(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(color(msg)));
    }

    /** 2 miejsca po przecinku, np. 0.50 */
    public static String num(double d) {
        return String.format(Locale.US, "%.2f", d);
    }

    /** 1 miejsce po przecinku, np. 12.5 */
    public static String pct(double d) {
        return String.format(Locale.US, "%.1f", d);
    }
}
