package com.thedasmc.mcsdmarketsplugin;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.PaperCommandManager;
import com.thedasmc.mcsdmarketsapi.MCSDMarketsAPI;
import com.thedasmc.mcsdmarketsplugin.commands.*;
import com.thedasmc.mcsdmarketsplugin.config.SessionFactoryManager;
import com.thedasmc.mcsdmarketsplugin.dao.PlayerVirtualItemDao;
import com.thedasmc.mcsdmarketsplugin.dao.db.PlayerVirtualItemDbDao;
import com.thedasmc.mcsdmarketsplugin.dao.file.PlayerVirtualItemFileDao;
import com.thedasmc.mcsdmarketsplugin.listener.InventoryClickEventListener;
import com.thedasmc.mcsdmarketsplugin.listener.InventoryCloseEventListener;
import com.thedasmc.mcsdmarketsplugin.listener.PlayerDropItemEventListener;
import com.thedasmc.mcsdmarketsplugin.support.SellInventoryManager;
import com.thedasmc.mcsdmarketsplugin.support.gui.GUISupport;
import com.thedasmc.mcsdmarketsplugin.support.messages.Message;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MCSDMarkets extends JavaPlugin {

    private MCSDMarketsAPI mcsdMarketsAPI;
    private Economy economy;
    private GUISupport guiSupport;
    private PlayerVirtualItemDao playerVirtualItemDao;
    private SellInventoryManager sellInventoryManager;

    @Override
    public void onEnable() {
        String pluginName = getDescription().getName();

        saveDefaultConfig();
        saveResource("messages.yml", false);

        if (!initMCSDMarketsAPI()) {
            getLogger().warning(String.format("[%s]No api-key found! You need to go get an api key and add it to the config.yml!", pluginName));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!initEconomy()) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found! Be sure you have a compatible economy plugin.", pluginName));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Message.setMessagesConfig(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml")));
        initGuiSupport();
        initPlayerVirtualItemDao();
        initSellInventoryManager();
        initCommandManager();

        registerListeners();
    }

    public MCSDMarketsAPI getMcsdMarketsAPI() {
        return mcsdMarketsAPI;
    }

    public Economy getEconomy() {
        return economy;
    }

    public GUISupport getGuiSupport() {
        return guiSupport;
    }

    public PlayerVirtualItemDao getPlayerVirtualItemDao() {
        return playerVirtualItemDao;
    }

    public SellInventoryManager getSellInventoryManager() {
        return sellInventoryManager;
    }

    private boolean initMCSDMarketsAPI() {
        String apiKey = getConfig().getString("api-key");

        if (apiKey == null || apiKey.trim().isEmpty())
            return false;

        this.mcsdMarketsAPI = new MCSDMarketsAPI(apiKey, getServer().getBukkitVersion().split("-")[0]);
        return true;
    }

    private boolean initEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null)
            return false;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null)
            return false;

        //noinspection ConstantConditions
        return (this.economy = rsp.getProvider()) != null;
    }

    private void initGuiSupport() {
        this.guiSupport = new GUISupport();
    }

    private void initPlayerVirtualItemDao() {
        if (getConfig().getBoolean("use-mysql", false)) {
            try {
                this.playerVirtualItemDao = new PlayerVirtualItemDbDao(new SessionFactoryManager(this));
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.SEVERE, String.format("[%s] - Failed to initialize database connection! Are your connection details correct? Disabling plugin.", getName()), e);
                Bukkit.getPluginManager().disablePlugin(this);
            }
        } else {
            Bukkit.getLogger().info(String.format("[%s] - Using flat file implementation. It is recommended to use MySQL for better performance.", getName()));
            this.playerVirtualItemDao = new PlayerVirtualItemFileDao(this);
        }
    }

    private void initSellInventoryManager() {
        this.sellInventoryManager = new SellInventoryManager();
    }

    private void initCommandManager() {
        BukkitCommandManager commandManager = new PaperCommandManager(this);

        //Command completions
        List<String> materialNames = Arrays.stream(Material.values())
            .map(Material::name)
            .filter(name -> !name.startsWith("LEGACY_"))
            .collect(Collectors.toCollection(LinkedList::new));

        commandManager.getCommandCompletions().registerAsyncCompletion("materials", context -> materialNames.stream()
            .filter(name -> name.startsWith(context.getInput().trim().toUpperCase()))
            .collect(Collectors.toList()));

        //Dependencies
        commandManager.registerDependency(Economy.class, this.economy);
        commandManager.registerDependency(MCSDMarketsAPI.class, this.mcsdMarketsAPI);
        commandManager.registerDependency(GUISupport.class, this.guiSupport);
        commandManager.registerDependency(PlayerVirtualItemDao.class, this.playerVirtualItemDao);
        commandManager.registerDependency(SellInventoryManager.class, this.sellInventoryManager);

        //Conditions
        commandManager.getCommandConditions().addCondition(Integer.class, "gt0", ((context, execContext, value) -> {
            //Null value will not be validated
            if (value != null && value <= 0)
                throw new ConditionFailedException(Message.INVALID_QUANTITY.getText());
        }));

        //Commands
        commandManager.registerCommand(new CheckPriceCommand());
        commandManager.registerCommand(new BuyCommand());
        commandManager.registerCommand(new SellCommand());
        commandManager.registerCommand(new WithdrawContractCommand());
        commandManager.registerCommand(new ViewCommand());
        commandManager.registerCommand(new SellInventoryCommand());
    }

    private void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerDropItemEventListener(this), this);
        pluginManager.registerEvents(new InventoryClickEventListener(this), this);
        pluginManager.registerEvents(new InventoryCloseEventListener(this), this);
    }
}
