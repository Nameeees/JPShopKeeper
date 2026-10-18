package com.jp.shopkeeper;

import com.jp.shopkeeper.command.TraderCommand;
import com.jp.shopkeeper.hook.PlaceholderHook;
import com.jp.shopkeeper.listener.ChatInputListener;
import com.jp.shopkeeper.listener.MenuListener;
import com.jp.shopkeeper.listener.TraderInteractListener;
import com.jp.shopkeeper.listener.TraderProtectionListener;
import com.jp.shopkeeper.manager.ChatInputManager;
import com.jp.shopkeeper.manager.ConfigManager;
import com.jp.shopkeeper.manager.EconomyManager;
import com.jp.shopkeeper.manager.MenuResources;
import com.jp.shopkeeper.manager.MessageService;
import com.jp.shopkeeper.manager.TradeService;
import com.jp.shopkeeper.manager.TraderManager;
import com.jp.shopkeeper.storage.TraderStorage;
import com.jp.shopkeeper.util.Sounds;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * JPShopKeeper — NPCs comerciantes con tradeos y paginas configurables.
 *
 * Un unico JAR funciona de la 1.8 a la ultima version: se compila contra
 * Spigot 1.8.8 (bytecode Java 8) y todo lo que cambio entre versiones
 * (materiales, sonidos, entidades, metodos de entidad) se resuelve por
 * nombre/reflexion en tiempo de ejecucion. Sin NMS y sin Citizens.
 */
public final class JPShopKeeper extends JavaPlugin {

    private ConfigManager configManager;
    private MessageService messages;
    private MenuResources menus;
    private Sounds sounds;
    private EconomyManager economy;
    private ChatInputManager chatInput;
    private TraderStorage storage;
    private TradeService tradeService;
    private TraderManager traderManager;

    /** Folia trae su propio planificador por regiones, incompatible con este plugin. */
    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void onEnable() {
        if (isFolia()) {
            getLogger().severe("Folia detectado: JPShopKeeper todavia no soporta el planificador por"
                    + " regiones y se desactiva para no romper nada. Usa Paper o Spigot.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // Los textos salen del idioma activo (messages_<idioma>.yml y
        // menus_<idioma>.yml, con messages.yml/menus.yml como respaldo):
        // los prepara LangFile al cargar cada manager.
        saveIfMissing("config.yml");

        configManager = new ConfigManager(this);
        messages = new MessageService(this);
        menus = new MenuResources(this);
        sounds = new Sounds(this);

        economy = new EconomyManager(this);
        economy.setup();

        chatInput = new ChatInputManager(this);
        storage = new TraderStorage(this);
        tradeService = new TradeService(this);
        traderManager = new TraderManager(this);

        getServer().getPluginManager().registerEvents(new TraderInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new TraderProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(this), this);

        PluginCommand command = getCommand("trader");
        if (command != null) {
            TraderCommand executor = new TraderCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().warning("El comando /trader no esta declarado en plugin.yml");
        }

        traderManager.loadAll();

        if (configManager.placeholderApi()) {
            PlaceholderHook.register(this);
        }
        // Chat moderno de Paper: solo si el servidor lo trae (Spigot lo ignora).
        com.jp.shopkeeper.hook.PaperChatHook.register(this);

        getLogger().info("JPShopKeeper v" + getDescription().getVersion()
                + " activado (compatible 1.8 - ultima version). Usa /trader help.");
    }

    @Override
    public void onDisable() {
        if (tradeService != null) {
            // Los enfriamientos sobreviven al reinicio del servidor.
            tradeService.saveCooldowns();
        }
        if (traderManager != null) {
            traderManager.shutdown();
        }
        getLogger().info("JPShopKeeper desactivado: traders guardados.");
    }

    private void saveIfMissing(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.isFile()) {
            try {
                saveResource(fileName, false);
            } catch (Throwable throwable) {
                getLogger().warning("No se pudo copiar " + fileName + ": " + throwable.getMessage());
            }
        }
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public MessageService messages() {
        return messages;
    }

    public MenuResources menus() {
        return menus;
    }

    public Sounds sounds() {
        return sounds;
    }

    public EconomyManager economy() {
        return economy;
    }

    public ChatInputManager chatInput() {
        return chatInput;
    }

    public TraderStorage storage() {
        return storage;
    }

    public TradeService tradeService() {
        return tradeService;
    }

    public TraderManager traderManager() {
        return traderManager;
    }
}
