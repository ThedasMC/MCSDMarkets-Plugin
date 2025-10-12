package com.thedasmc.mcsdmarketsplugin.dao.file;

import com.google.gson.Gson;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.dao.file.impl.PlayerVirtualItemFileDao;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import jakarta.validation.ValidatorFactory;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class TestablePlayerVirtualItemFileDao extends PlayerVirtualItemFileDao {

    public TestablePlayerVirtualItemFileDao(MCSDMarkets plugin, Gson gson, ValidatorFactory validatorFactory) {
        super(plugin, gson, validatorFactory);
    }

    public void save(PlayerVirtualItem playerVirtualItem, ReentrantLock lock) {
        locks.put(UUID.fromString(playerVirtualItem.getId().getUuid()), lock);
        super.save(playerVirtualItem);
    }

    public boolean containsLockFor(UUID uuid) {
        return locks.containsKey(uuid);
    }
}
