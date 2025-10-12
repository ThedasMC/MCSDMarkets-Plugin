package com.thedasmc.mcsdmarketsplugin.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tchristofferson.betterscheduler.BSAsyncTask;
import com.thedasmc.mcsdmarketsapi.response.impl.HistoricalItemPriceResponse;
import com.thedasmc.mcsdmarketsapi.response.wrapper.HistoricalItemPriceResponseWrapper;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.support.GraphImageKey;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class HistoricalGraphRenderer extends MapRenderer {

    private static final Cache<GraphImageKey, Image> GRAPH_CACHE = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build();

    private final MCSDMarkets plugin;
    private final Material material;
    private final ChronoUnit timeUnit;
    private final int timeAmount;
    private final AtomicBoolean isRendering = new AtomicBoolean(false);
    private final AtomicReference<LocalDateTime> lastDrawTime = new AtomicReference<>(null);

    public HistoricalGraphRenderer(MCSDMarkets plugin, Material material, ChronoUnit timeUnit, int timeAmount) {
        super(true);//Allows render method to be called frequently
        this.plugin = plugin;
        this.material = material;

        if (!Arrays.asList(ChronoUnit.DAYS, ChronoUnit.HOURS).contains(timeUnit))
            throw new IllegalArgumentException("Invalid time unit! Must be DAYS or HOURS");

        this.timeUnit = timeUnit;
        this.timeAmount = timeAmount;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (isRendering.get())
            return;

        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Chicago")).truncatedTo(ChronoUnit.HOURS);

        if (lastDrawTime.get() != null && lastDrawTime.get().equals(now))
            return;

        final GraphImageKey key = new GraphImageKey(material, now, timeUnit, timeAmount);
        Image graphImage = GRAPH_CACHE.getIfPresent(key);

        if (graphImage != null) {
            canvas.drawImage(0, 0, graphImage);
            lastDrawTime.set(now);
            return;
        }

        plugin.getLogger().info(String.format("Rendering graph for material %s . . .", material.name()));
        isRendering.set(true);
        plugin.getTaskQueueRunner().scheduleAsyncTask(new BSAsyncTask(plugin) {
            @Override
            public void run() {
                LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Chicago")).truncatedTo(ChronoUnit.HOURS);
                HistoricalItemPriceResponseWrapper response;

                try {
                    if (timeUnit.equals(ChronoUnit.DAYS)) {
                        response = plugin.getMcsdMarketsAPI().getDaysHistoricalItemPrices(material.name(), timeAmount);
                    } else {//HOURS
                        response = plugin.getMcsdMarketsAPI().getHoursHistoricalItemPrice(material.name(), timeAmount);
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning(String.format("Failed to get historical item prices for material %s: %s", material.name(), e.getMessage()));
                    return;
                }

                if (!response.isSuccessful()) {
                    plugin.getLogger().warning(String.format("Failed to get historical item prices for material %s: %s", material.name(), response.getErrorResponse().getMessage()));
                    return;
                }

                GRAPH_CACHE.put(key, createGraphImage(response));
                isRendering.set(false);
                plugin.getLogger().info(String.format("Rendered graph for material %s", material.name()));
            }
        });
    }

    private Image createGraphImage(HistoricalItemPriceResponseWrapper response) {
        final int width = 128;
        final int height = 128;

        List<HistoricalItemPriceResponse> historicalItemPrices = response.getSuccessfulResponse();
        final Color lineColor = getGraphColor(historicalItemPrices);

        XYSeries series = new XYSeries(material.name());

        for (int i = 1; i <= historicalItemPrices.size(); i++) {
            HistoricalItemPriceResponse historicalItemPriceResponse = historicalItemPrices.get(i - 1);
            series.add(i, historicalItemPriceResponse.getPrice());
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);

        //Create chart
        JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false);
        chart.setAntiAlias(false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.setBorderVisible(false);
        chart.setPadding(RectangleInsets.ZERO_INSETS);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setInsets(RectangleInsets.ZERO_INSETS);
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, lineColor);
        renderer.setSeriesStroke(0, new BasicStroke(1));
        renderer.setSeriesShapesVisible(0, false);
        plot.setRenderer(renderer);

        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setLowerBound(1);
        xAxis.setUpperBound(timeAmount);
        xAxis.setAutoTickUnitSelection(true);
        xAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        xAxis.setLabelInsets(RectangleInsets.ZERO_INSETS);
        plot.setDomainAxis(xAxis);

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        yAxis.setLabelInsets(RectangleInsets.ZERO_INSETS);
        plot.setRangeAxis(yAxis);

        return chart.createBufferedImage(width, height);
    }

    private Color getGraphColor(List<HistoricalItemPriceResponse> historicalItemPrices) {
        if (historicalItemPrices.size() > 1) {
            BigDecimal firstPrice = historicalItemPrices.get(0).getPrice();
            BigDecimal lastPrice = historicalItemPrices.get(historicalItemPrices.size() - 1).getPrice();

            if (firstPrice.compareTo(lastPrice) > 0) {
                return Color.RED;
            } else if (firstPrice.compareTo(lastPrice) < 0) {
                return Color.GREEN;
            } else {
                return Color.BLACK;
            }
        }

        return Color.BLACK;
    }
}
