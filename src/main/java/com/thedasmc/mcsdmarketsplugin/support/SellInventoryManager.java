package com.thedasmc.mcsdmarketsplugin.support;

import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SellInventoryManager {

    private final Map<UUID, Inventory> sellInventoryMap = new HashMap<>();

    public void addSellInventory(UUID uuid, Inventory inventory) {
        sellInventoryMap.put(uuid, inventory);
    }

    public Inventory removeSellInventory(UUID uuid) {
        return sellInventoryMap.remove(uuid);
    }

    public boolean containsSellInventory(UUID uuid) {
        return sellInventoryMap.containsKey(uuid);
    }

}
