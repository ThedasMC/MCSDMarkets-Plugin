package com.thedasmc.mcsdmarketsplugin.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import org.junit.jupiter.api.Test;

import static com.thedasmc.mcsdmarketsplugin.util.PlayerVirtualItemUtil.createSamplePlayerVirtualItemWithVersion;
import static org.junit.jupiter.api.Assertions.*;

public class PlayerVirtualItemJsonConverterTest {

    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(PlayerVirtualItem.class, new PlayerVirtualItemJsonConverter())
        .create();

    @Test
    public void ensurePlayerVirtualItemIsTheSameAfterBeingSerializedAndDeserialized() {
        PlayerVirtualItem playerVirtualItem = createSamplePlayerVirtualItemWithVersion(1);
        PlayerVirtualItem clone = gson.fromJson(gson.toJson(playerVirtualItem), PlayerVirtualItem.class);

        assertEquals(playerVirtualItem, clone);
    }
}