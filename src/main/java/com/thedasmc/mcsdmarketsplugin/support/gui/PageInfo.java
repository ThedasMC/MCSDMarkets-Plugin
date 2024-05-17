package com.thedasmc.mcsdmarketsplugin.support.gui;

import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class PageInfo {

    private final UUID uuid;
    private final int page;
    private final Inventory inventory;

    public PageInfo(UUID uuid, int page, Inventory inventory) {
        this.uuid = uuid;
        this.page = page;
        this.inventory = inventory;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getPage() {
        return page;
    }

    public Inventory getInventory() {
        return inventory;
    }
}
