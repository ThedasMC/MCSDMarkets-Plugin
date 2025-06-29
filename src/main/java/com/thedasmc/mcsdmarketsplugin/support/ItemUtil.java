package com.thedasmc.mcsdmarketsplugin.support;

import org.bukkit.Material;

import java.util.Optional;

public class ItemUtil {

    public static Optional<Material> getMaterial(String materialName) {
        materialName = materialName.trim().toUpperCase();
        Material material = Material.getMaterial(materialName);

        if (material == null) material = Material.getMaterial(materialName, true);
        if (material == null) material = Material.matchMaterial(materialName);

        return Optional.ofNullable(material);
    }
}
