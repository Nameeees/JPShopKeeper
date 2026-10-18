package com.jp.shopkeeper.manager;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.util.LangFile;
import com.jp.shopkeeper.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Todos los textos del plugin viven en messages.yml (nada incrustado en
 * codigo). Aqui se cargan, se aplican placeholders {clave} y el prefijo,
 * y se avisa en consola si falta alguna clave.
 */
public final class MessageService {

    private static final String FALLBACK_PREFIX = "&8[&2JP&aShopKeeper&8] &7» &f";

    private final JPShopKeeper plugin;
    private final Set<String> warned = new HashSet<String>();

    private YamlConfiguration yml;
    private String prefix;
    private String fileName = "messages.yml";

    public MessageService(JPShopKeeper plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        String language = plugin.configManager().language();
        File file = LangFile.resolve(plugin, "messages", language);
        File base = LangFile.baseFile(plugin, "messages");
        fileName = file.getName();
        yml = YamlConfiguration.loadConfiguration(file);
        if (base.getName().equals(fileName)) {
            plugin.getLogger().info("Textos: " + fileName);
            if (language != null && !language.isEmpty() && !"es".equals(language)) {
                plugin.getLogger().warning("No existe messages_" + language
                        + ".yml: se usan los textos de " + fileName + ".");
            }
        } else {
            // Lo que falte en la traduccion se toma del archivo base en vez de
            // mostrar "<falta:clave>" al jugador.
            yml.setDefaults(YamlConfiguration.loadConfiguration(base));
            plugin.getLogger().info("Textos: " + fileName + " (lo que falte se toma de " + base.getName() + ")");
        }
        prefix = yml.getString("prefix", FALLBACK_PREFIX);
        warned.clear();
    }

    /** Archivo de textos en uso (messages.yml, messages_en.yml...), para la consola. */
    public String fileName() {
        return fileName;
    }

    /** Texto de una clave, con prefijo y placeholders, ya coloreado. */
    public String text(String key, Map<String, String> placeholders) {
        String raw = yml.getString(key);
        if (raw == null) {
            warnOnce(key);
            return Text.color("&c<falta:" + key + ">");
        }
        Map<String, String> all = new HashMap<String, String>();
        all.put("prefix", prefix);
        if (placeholders != null) {
            all.putAll(placeholders);
        }
        return Text.color(Text.apply(raw, all));
    }

    public String text(String key) {
        return text(key, null);
    }

    /** Lista de lineas de una clave (para menus o detalles multilinea). */
    public List<String> lines(String key, Map<String, String> placeholders) {
        List<String> raw = yml.getStringList(key);
        if (raw.isEmpty() && yml.isString(key)) {
            List<String> single = new ArrayList<String>();
            String value = yml.getString(key);
            if (value != null) {
                for (String part : value.split("\n")) {
                    single.add(part);
                }
            }
            raw = single;
        }
        if (raw.isEmpty()) {
            warnOnce(key);
            return new ArrayList<String>();
        }
        Map<String, String> all = new HashMap<String, String>();
        all.put("prefix", prefix);
        if (placeholders != null) {
            all.putAll(placeholders);
        }
        return Text.color(Text.apply(raw, all));
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(text(key, placeholders));
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, null);
    }

    public String prefix() {
        return prefix;
    }

    private void warnOnce(String key) {
        if (warned.add(key)) {
            plugin.getLogger().warning("Falta la clave '" + key + "' en " + fileName);
        }
    }
}
