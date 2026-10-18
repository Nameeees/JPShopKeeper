package com.jp.shopkeeper.manager;

import com.jp.shopkeeper.JPShopKeeper;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Implementacion real del puente Vault. Esta clase SOLO se carga si el
 * plugin Vault esta presente (ver EconomyManager.setup()).
 */
public final class VaultEconomy implements EconomyManager.EconomyProvider {

    private final Economy economy;

    public VaultEconomy(JPShopKeeper plugin) {
        RegisteredServiceProvider<Economy> registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        this.economy = registration == null ? null : registration.getProvider();
    }

    @Override
    public boolean ready() {
        return economy != null;
    }

    @Override
    public double balance(Player player) {
        return economy == null ? 0.0 : economy.getBalance(player);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (economy == null) {
            return false;
        }
        try {
            return economy.withdrawPlayer(player, amount).transactionSuccess();
        } catch (Throwable throwable) {
            return false;
        }
    }
}
