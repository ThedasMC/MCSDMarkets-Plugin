package com.thedasmc.mcsdmarketsplugin.listener;

import com.tchristofferson.betterscheduler.BSAsyncTask;
import com.tchristofferson.betterscheduler.BSCallable;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.dao.PriceHistoryMapDao;
import com.thedasmc.mcsdmarketsplugin.renderer.HistoricalGraphRenderer;
import com.thedasmc.mcsdmarketsplugin.support.TimeUnit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.map.MapView;

public class MapInitializeEventListener implements Listener {

    private final MCSDMarkets plugin;
    private final PriceHistoryMapDao priceHistoryMapDao;

    public MapInitializeEventListener(MCSDMarkets plugin, PriceHistoryMapDao priceHistoryMapDao) {
        this.plugin = plugin;
        this.priceHistoryMapDao = priceHistoryMapDao;
    }

    @EventHandler
    public void onMapInitializeEvent(MapInitializeEvent event) {
        MapView mapView = event.getMap();
        int mapId = mapView.getId();

        plugin.getTaskQueueRunner().scheduleAsyncTask(new BSAsyncTask(plugin) {
            @Override
            public void run() {
                priceHistoryMapDao.findById(mapId).ifPresent(priceHistoryMap -> plugin.getTaskQueueRunner().submitSyncTask(new BSCallable<Void>() {
                    @Override
                    protected Void execute() {
                        mapView.getRenderers().forEach(mapView::removeRenderer);

                        Material material = Material.matchMaterial(priceHistoryMap.getMaterialName());
                        TimeUnit timeUnit = TimeUnit.valueOf(priceHistoryMap.getTimeUnit());
                        int timeAmount = priceHistoryMap.getTimeAmount();

                        mapView.addRenderer(new HistoricalGraphRenderer(plugin, material, timeUnit.chronoUnit, timeAmount));
                        return null;
                    }
                }));
            }
        });
    }

}
