package com.thedasmc.mcsdmarketsplugin.util;

import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItemPK;
import org.bukkit.Material;

import java.util.UUID;

public class PlayerVirtualItemUtil {

    public static PlayerVirtualItem createSamplePlayerVirtualItem() {
        //Uses random uuid so each test doesn't interfere with each other's data
        PlayerVirtualItemPK id = new PlayerVirtualItemPK(UUID.randomUUID().toString(), Material.DIAMOND.name());
        PlayerVirtualItem pvi = new PlayerVirtualItem();
        pvi.setId(id);
        pvi.setQuantity(5);

        return pvi;
    }

    public static PlayerVirtualItem createSamplePlayerVirtualItemWithVersion(int version) {
        PlayerVirtualItem pvi = createSamplePlayerVirtualItem();
        pvi.setVersion(version);

        return pvi;
    }

}
