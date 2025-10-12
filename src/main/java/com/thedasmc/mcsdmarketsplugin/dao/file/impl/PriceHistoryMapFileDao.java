package com.thedasmc.mcsdmarketsplugin.dao.file.impl;

import com.google.gson.Gson;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.dao.PriceHistoryMapDao;
import com.thedasmc.mcsdmarketsplugin.dao.file.FileDao;
import com.thedasmc.mcsdmarketsplugin.model.PriceHistoryMap;
import jakarta.validation.ValidatorFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class PriceHistoryMapFileDao extends FileDao<Integer> implements PriceHistoryMapDao {

    private static final String MAP_FILE_NAME = "map_%d.json";

    public PriceHistoryMapFileDao(MCSDMarkets plugin, Gson gson, ValidatorFactory validatorFactory) {
        super(gson, validatorFactory, new File(plugin.getDataFolder(), "maps"));
        //noinspection ResultOfMethodCallIgnored
        this.savesDir.mkdir();
    }

    @Override
    public Optional<PriceHistoryMap> findById(int id) {
        try {
            return runWithLock(id, () -> {
                File file = new File(this.savesDir, String.format(MAP_FILE_NAME, id));

                try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                    return Optional.of(gson.fromJson(reader, PriceHistoryMap.class));
                } catch (FileNotFoundException e) {
                    return Optional.empty();
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public PriceHistoryMap save(PriceHistoryMap priceHistoryMap) {
        validate(priceHistoryMap);

        try {
            return runWithLock(priceHistoryMap.getId(), () -> {
                File file = new File(this.savesDir, String.format(MAP_FILE_NAME, priceHistoryMap.getId()));
                saveContent(gson.toJson(priceHistoryMap), file);
                return priceHistoryMap;
            });
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
