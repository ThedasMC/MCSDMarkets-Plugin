package com.thedasmc.mcsdmarketsplugin.json;

import com.google.gson.*;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItemPK;

import java.lang.reflect.Type;

public class PlayerVirtualItemJsonConverter implements JsonSerializer<PlayerVirtualItem>, JsonDeserializer<PlayerVirtualItem> {
    @Override
    public PlayerVirtualItem deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        PlayerVirtualItemPK id = new PlayerVirtualItemPK();
        id.setUuid(jsonObject.get("uuid").getAsString());
        id.setMaterial(jsonObject.get("material").getAsString());

        PlayerVirtualItem playerVirtualItem = new PlayerVirtualItem();
        playerVirtualItem.setId(id);
        playerVirtualItem.setQuantity(jsonObject.get("quantity").getAsLong());
        playerVirtualItem.setVersion(jsonObject.get("version").getAsInt());

        return playerVirtualItem;
    }

    @Override
    public JsonElement serialize(PlayerVirtualItem src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("uuid", src.getId().getUuid());
        jsonObject.addProperty("material", src.getId().getMaterial());
        jsonObject.addProperty("quantity", src.getQuantity());
        jsonObject.addProperty("version", src.getVersion());

        return jsonObject;
    }
}
