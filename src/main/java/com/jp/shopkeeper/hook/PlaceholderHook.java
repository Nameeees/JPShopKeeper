package com.jp.shopkeeper.hook;

import com.jp.shopkeeper.JPShopKeeper;
import org.bukkit.Bukkit;

/**
 * Puente con PlaceholderAPI.
 *
 * Toda referencia a clases de PlaceholderAPI vive en {@link TraderExpansion},
 * que solo se carga cuando el plugin esta instalado: gracias al softdepend,
 * JPShopKeeper arranca igual en servidores sin PlaceholderAPI.
 */
public final class PlaceholderHook {

    private PlaceholderHook() {
    }

    public static void register(JPShopKeeper plugin) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            if (new TraderExpansion(plugin).isRegistered()) {
                return;
            }
            boolean registered = new TraderExpansion(plugin).register();
            if (registered) {
                plugin.getLogger().info("PlaceholderAPI detectado: placeholders %jpsk_...% registrados.");
            } else {
                plugin.getLogger().warning("PlaceholderAPI no acepto el registro de los placeholders %jpsk_...%.");
            }
        } catch (Throwable throwable) {
            // Si PAPI cambia o no esta: el plugin sigue funcionando sin placeholders.
            plugin.getLogger().warning("No se pudieron registrar los placeholders de PlaceholderAPI: " + throwable);
        }
    }
}
