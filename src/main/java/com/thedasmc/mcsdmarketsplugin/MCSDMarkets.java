package com.thedasmc.mcsdmarketsplugin;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.PaperCommandManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tchristofferson.betterscheduler.TaskQueueRunner;
import com.thedasmc.mcsdmarketsapi.MCSDMarketsAPI;
import com.thedasmc.mcsdmarketsplugin.commands.*;
import com.thedasmc.mcsdmarketsplugin.config.SessionFactoryManager;
import com.thedasmc.mcsdmarketsplugin.dao.PlayerVirtualItemDao;
import com.thedasmc.mcsdmarketsplugin.dao.PriceHistoryMapDao;
import com.thedasmc.mcsdmarketsplugin.dao.db.impl.PlayerVirtualItemDbDao;
import com.thedasmc.mcsdmarketsplugin.dao.db.impl.PriceHistoryDbDao;
import com.thedasmc.mcsdmarketsplugin.dao.file.impl.PlayerVirtualItemFileDao;
import com.thedasmc.mcsdmarketsplugin.dao.file.impl.PriceHistoryMapFileDao;
import com.thedasmc.mcsdmarketsplugin.json.PlayerVirtualItemJsonConverter;
import com.thedasmc.mcsdmarketsplugin.listener.InventoryClickEventListener;
import com.thedasmc.mcsdmarketsplugin.listener.InventoryCloseEventListener;
import com.thedasmc.mcsdmarketsplugin.listener.MapInitializeEventListener;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import com.thedasmc.mcsdmarketsplugin.support.PersistenceManager;
import com.thedasmc.mcsdmarketsplugin.support.SellInventoryManager;
import com.thedasmc.mcsdmarketsplugin.support.TimeUnit;
import com.thedasmc.mcsdmarketsplugin.support.gui.GUISupport;
import com.thedasmc.mcsdmarketsplugin.support.messages.Message;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

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
    private PersistenceManager persistenceManager;
    private SellInventoryManager sellInventoryManager;
    private TaskQueueRunner taskQueueRunner;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        if (!initMCSDMarketsAPI()) {
            getLogger().warning("No api-key found! You need to go get an api key and add it to the config.yml!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!initEconomy()) {
            getLogger().severe("Disabled due to no Vault dependency found! Be sure you have a compatible economy plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Message.setMessagesConfig(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml")));
        initGuiSupport();
        initPersistenceManager();
        initSellInventoryManager();
        initTaskQueueRunner();
        initCommandManager();

        registerListeners();
    }

    @Override
    public void onDisable() {
        taskQueueRunner.shutdown();
        persistenceManager.shutdown();
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

    public SellInventoryManager getSellInventoryManager() {
        return sellInventoryManager;
    }

    public TaskQueueRunner getTaskQueueRunner() {
        return taskQueueRunner;
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

    private void initPersistenceManager() {
        this.persistenceManager = new PersistenceManager();

        if (getConfig().getBoolean("use-mysql", false)) {
            try {
                SessionFactoryManager sessionFactoryManager = new SessionFactoryManager(this);
                this.persistenceManager.setSessionFactoryManager(sessionFactoryManager);
                this.persistenceManager.setPlayerVirtualItemDao(new PlayerVirtualItemDbDao(sessionFactoryManager));
                this.persistenceManager.setPriceHistoryMapDao(new PriceHistoryDbDao(sessionFactoryManager));
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to initialize database connection! Are your connection details correct? Disabling plugin.");
                Bukkit.getPluginManager().disablePlugin(this);
            }
        } else {
            getLogger().info("Using flat file implementation. It is recommended to use MySQL.");
            Gson gson = new GsonBuilder()
                .registerTypeAdapter(PlayerVirtualItem.class, new PlayerVirtualItemJsonConverter())
                .create();

            ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory();

            this.persistenceManager.setFileValidatorFactory(validatorFactory);
            this.persistenceManager.setPlayerVirtualItemDao(new PlayerVirtualItemFileDao(this, gson, validatorFactory));
            this.persistenceManager.setPriceHistoryMapDao(new PriceHistoryMapFileDao(this, gson, validatorFactory));
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

        commandManager.getCommandCompletions().registerAsyncCompletion("timeUnits", context -> Arrays.stream(TimeUnit.values())
            .map(TimeUnit::name)
            .filter(timeUnit -> timeUnit.startsWith(context.getInput().trim().toUpperCase()))
            .collect(Collectors.toList())
        );

        //Dependencies
        commandManager.registerDependency(Economy.class, this.economy);
        commandManager.registerDependency(MCSDMarketsAPI.class, this.mcsdMarketsAPI);
        commandManager.registerDependency(GUISupport.class, this.guiSupport);
        commandManager.registerDependency(PlayerVirtualItemDao.class, this.persistenceManager.getPlayerVirtualItemDao());
        commandManager.registerDependency(PriceHistoryMapDao.class, this.persistenceManager.getPriceHistoryMapDao());
        commandManager.registerDependency(SellInventoryManager.class, this.sellInventoryManager);
        commandManager.registerDependency(TaskQueueRunner.class, this.taskQueueRunner);

        //Conditions
        commandManager.getCommandConditions().addCondition(Integer.class, "gt0", ((context, execContext, value) -> ensureGt0Condition(value != null ? value.longValue() : null)));
        commandManager.getCommandConditions().addCondition(Long.class, "gt0", ((context, execContext, value) -> ensureGt0Condition(value)));

        //Commands
        commandManager.registerCommand(new CheckPriceCommand());
        commandManager.registerCommand(new BuyCommand());
        commandManager.registerCommand(new SellCommand());
        commandManager.registerCommand(new WithdrawCommand());
        commandManager.registerCommand(new ViewCommand());
        commandManager.registerCommand(new SellInventoryCommand());
        commandManager.registerCommand(new PriceHistoryCommand());
        commandManager.registerCommand(new PortfolioCommand());
    }

    private void initTaskQueueRunner() {
        //Scheduled with BukkitScheduler in constructor
        taskQueueRunner = new TaskQueueRunner(this);
    }

    private void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new InventoryClickEventListener(this), this);
        pluginManager.registerEvents(new InventoryCloseEventListener(this), this);
        pluginManager.registerEvents(new MapInitializeEventListener(this, this.persistenceManager.getPriceHistoryMapDao()), this);
    }

    private void ensureGt0Condition(Long value) {
        if (value != null && value <= 0)
            throw new ConditionFailedException(Message.INVALID_QUANTITY.getText());
    }
}
