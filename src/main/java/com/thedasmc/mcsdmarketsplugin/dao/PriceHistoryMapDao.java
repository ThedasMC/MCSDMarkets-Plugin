package com.thedasmc.mcsdmarketsplugin.dao;

import com.thedasmc.mcsdmarketsplugin.model.PriceHistoryMap;

import java.util.Optional;

public interface PriceHistoryMapDao {

    Optional<PriceHistoryMap> findById(int id);

    PriceHistoryMap save(PriceHistoryMap priceHistoryMap);

}
