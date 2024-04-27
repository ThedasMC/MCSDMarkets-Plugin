package com.thedasmc.mcsdmarketsplugin;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.PaperCommandManager;
import com.thedasmc.mcsdmarketsapi.MCSDMarketsAPI;
import com.thedasmc.mcsdmarketsplugin.commands.BuyCommand;
import com.thedasmc.mcsdmarketsplugin.commands.CheckPriceCommand;
import com.thedasmc.mcsdmarketsplugin.commands.SellCommand;
import com.thedasmc.mcsdmarketsplugin.support.messages.Message;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class MCSDMarkets extends JavaPlugin {

    private MCSDMarketsAPI mcsdMarketsAPI;
    private Economy economy;

    @Override
    public void onEnable() {
        String pluginName = getDescription().getName();

        saveDefaultConfig();
        saveResource("messages.yml", false);
        FileConfiguration config = getConfig();

        if (!initMCSDMarketsAPI(config)) {
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
        initCommandManager();
    }

    private boolean initMCSDMarketsAPI(FileConfiguration config) {
        String apiKey = config.getString("api-key");

        if (apiKey == null || apiKey.trim().isEmpty())
            return false;

        this.mcsdMarketsAPI = new MCSDMarketsAPI(apiKey, true);
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

        //Conditions
        commandManager.getCommandConditions().addCondition(Integer.class, "gt0", ((context, execContext, value) -> {
            if (value <= 0)
                throw new ConditionFailedException(Message.INVALID_QUANTITY.getText());
        }));

        //Commands
        commandManager.registerCommand(new CheckPriceCommand());
        commandManager.registerCommand(new BuyCommand());
        commandManager.registerCommand(new SellCommand());
    }
}
