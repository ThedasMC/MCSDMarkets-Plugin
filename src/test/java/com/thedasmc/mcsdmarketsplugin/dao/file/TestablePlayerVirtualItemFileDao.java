package com.thedasmc.mcsdmarketsplugin.dao.file;

import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class TestablePlayerVirtualItemFileDao extends PlayerVirtualItemFileDao {

    public TestablePlayerVirtualItemFileDao(MCSDMarkets plugin) {
        super(plugin);
    }

    public void save(PlayerVirtualItem playerVirtualItem, ReentrantLock lock) {
        locks.put(UUID.fromString(playerVirtualItem.getId().getUuid()), lock);
        super.save(playerVirtualItem);
    }
}
