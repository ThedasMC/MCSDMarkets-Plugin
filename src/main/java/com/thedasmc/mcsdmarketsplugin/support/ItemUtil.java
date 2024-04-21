package com.thedasmc.mcsdmarketsplugin.support;

import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Objects;

public class ItemUtil {

    private static final Material CONTRACT_MATERIAL = Material.PAPER;

    public static ItemStack getPurchaseContractItem(MCSDMarkets plugin, Material material, Integer quantity) {
        ItemStack itemStack = new ItemStack(CONTRACT_MATERIAL);
        ItemMeta itemMeta = Objects.requireNonNull(itemStack.getItemMeta());
        itemMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "[MCSDMarkets]" + ChatColor.GRAY + "Purchase Contract");
        itemMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Item: " + material.name(),
            ChatColor.GRAY + "Quantity: " + quantity
        ));

        NamespacedKey namespacedKeyItem = new NamespacedKey(plugin, "contract-item");
        NamespacedKey namespacedKeyQuantity = new NamespacedKey(plugin, "contract-quantity");
        itemMeta.getPersistentDataContainer().set(namespacedKeyItem, PersistentDataType.STRING, material.name());
        itemMeta.getPersistentDataContainer().set(namespacedKeyQuantity, PersistentDataType.INTEGER, quantity);

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static boolean isPurchaseContractItem(MCSDMarkets plugin, ItemStack itemStack) {
        if (itemStack.getType() != CONTRACT_MATERIAL)
            return false;

        NamespacedKey namespacedKeyItem = new NamespacedKey(plugin, "contract-item");
        NamespacedKey namespacedKeyQuantity = new NamespacedKey(plugin, "contract-quantity");

        PersistentDataContainer container = Objects.requireNonNull(itemStack.getItemMeta()).getPersistentDataContainer();
        return container.has(namespacedKeyItem, PersistentDataType.STRING) && container.has(namespacedKeyQuantity, PersistentDataType.INTEGER);
    }

}
