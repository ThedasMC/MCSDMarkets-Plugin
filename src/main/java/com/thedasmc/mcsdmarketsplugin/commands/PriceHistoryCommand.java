package com.thedasmc.mcsdmarketsplugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.tchristofferson.betterscheduler.BSAsyncTask;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.dao.PriceHistoryMapDao;
import com.thedasmc.mcsdmarketsplugin.model.PriceHistoryMap;
import com.thedasmc.mcsdmarketsplugin.renderer.HistoricalGraphRenderer;
import com.thedasmc.mcsdmarketsplugin.support.ItemUtil;
import com.thedasmc.mcsdmarketsplugin.support.TimeUnit;
import com.thedasmc.mcsdmarketsplugin.support.messages.Message;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.util.Optional;

import static com.thedasmc.mcsdmarketsplugin.support.Constants.BASE_COMMAND;
import static com.thedasmc.mcsdmarketsplugin.support.Constants.PRICE_HISTORY_COMMAND_PERMISSION;

@CommandAlias(BASE_COMMAND)
public class PriceHistoryCommand extends BaseCommand {

    @Dependency private MCSDMarkets plugin;
    @Dependency private PriceHistoryMapDao priceHistoryMapDao;

    @Subcommand("pricehistory|ph")
    @CommandPermission(PRICE_HISTORY_COMMAND_PERMISSION)
    @Syntax("<material> <timeUnit>")
    @Description("Get a map displaying the price history of an item")
    @CommandCompletion("@materials @timeUnits")
    public void handlePriceHistoryCommand(Player player, String materialName, String timeUnitString) {
        Optional<Material> optionalMaterial = ItemUtil.getMaterial(materialName);

        if (optionalMaterial.isEmpty()) {
            player.sendMessage(Message.INVALID_MATERIAL.getText());
            return;
        }

        Optional<TimeUnit> optionalTimeUnit = TimeUnit.getTimeUnit(timeUnitString);

        if (optionalTimeUnit.isEmpty()) {
            player.sendMessage(Message.INVALID_TIME_UNIT.getText());
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(Message.NO_INVENTORY_SPACE.getText());
            return;
        }

        Material material = optionalMaterial.get();
        TimeUnit timeUnit = optionalTimeUnit.get();

        int timeAmount = switch (timeUnit) {
            case DAILY -> 7;
            case HOURLY -> 24;
            default -> throw new IllegalStateException("Unsupported time unit!");
        };

        HistoricalGraphRenderer graphRenderer = new HistoricalGraphRenderer(plugin, material, timeUnit.chronoUnit, timeAmount);

        ItemStack mapItemStack = new ItemStack(Material.FILLED_MAP, 1);
        MapMeta mapMeta = (MapMeta) mapItemStack.getItemMeta();
        mapMeta.setDisplayName(material.name() + " Price History (" + timeUnit.name().toLowerCase() + ")");
        MapView view = Bukkit.createMap(player.getWorld());
        view.getRenderers().forEach(view::removeRenderer);
        view.addRenderer(graphRenderer);
        mapMeta.setMapView(view);
        mapItemStack.setItemMeta(mapMeta);

        player.getInventory().addItem(mapItemStack);

        PriceHistoryMap priceHistoryMap = new PriceHistoryMap();
        priceHistoryMap.setId(view.getId());
        priceHistoryMap.setMaterialName(material.name());
        priceHistoryMap.setTimeUnit(timeUnit.name());
        priceHistoryMap.setTimeAmount(timeAmount);

        plugin.getTaskQueueRunner().scheduleAsyncTask(new BSAsyncTask(plugin) {
            @Override
            public void run() {
                priceHistoryMapDao.save(priceHistoryMap);
            }
        });
    }
}
