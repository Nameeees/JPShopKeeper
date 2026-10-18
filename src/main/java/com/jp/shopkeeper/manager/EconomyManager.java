package com.jp.shopkeeper.manager;

import com.jp.shopkeeper.JPShopKeeper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Puente opcional con Vault. Si el servidor no tiene Vault (o no hay
 * proveedor de economia), el plugin sigue funcionando solo con items.
 *
 * La clase que toca clases de Vault (VaultEconomy) se carga por nombre
 * para que la ausencia del plugin jamas produzca NoClassDefFoundError.
 */
public final class EconomyManager {

    /** Interfaz propia: el resto del plugin no conoce a Vault. */
    public interface EconomyProvider {
        boolean ready();

        double balance(Player player);

        boolean withdraw(Player player, double amount);
    }

    private final JPShopKeeper plugin;
    private EconomyProvider provider;

    public EconomyManager(JPShopKeeper plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        provider = null;
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault no encontrado: los tradeos con dinero quedan desactivados.");
            return;
        }
        try {
            Class<?> hookClass = Class.forName("com.jp.shopkeeper.manager.VaultEconomy");
            Object hook = hookClass.getConstructor(JPShopKeeper.class).newInstance(plugin);
            EconomyProvider candidate = (EconomyProvider) hook;
            if (candidate.ready()) {
                provider = candidate;
                plugin.getLogger().info("Vault conectado: tradeos con dinero activados.");
            } else {
                plugin.getLogger().warning("Vault esta presente pero sin proveedor de economia.");
            }
        } catch (Throwable throwable) {
            plugin.getLogger().warning("No se pudo conectar con Vault: " + throwable);
        }
    }

    public boolean enabled() {
        return provider != null;
    }

    public double balance(Player player) {
        if (provider == null) {
            return 0.0;
        }
        try {
            return provider.balance(player);
        } catch (Throwable throwable) {
            // Un proveedor de economia que falla no puede dejar al jugador sin menu.
            plugin.getLogger().warning("La economia fallo al consultar el saldo de " + player.getName()
                    + ": " + throwable);
            return 0.0;
        }
    }

    public boolean withdraw(Player player, double amount) {
        if (provider == null) {
            return false;
        }
        try {
            return provider.withdraw(player, amount);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("La economia fallo al cobrar a " + player.getName() + ": " + throwable);
            return false;
        }
    }
}
