package com.thedasmc.mcsdmarketsplugin.support;

import com.google.common.base.Preconditions;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static com.thedasmc.mcsdmarketsplugin.support.Constants.CONTRACT_ITEM_MATERIAL;

public class ItemUtil {

    /**
     * Get a new purchase contract item
     * @param plugin The plugin
     * @param material The material for the contract material
     * @param quantity The quantity of the material
     * @return An {@link ItemStack} representing the contract item
     */
    public static ItemStack getPurchaseContractItem(MCSDMarkets plugin, Material material, Integer quantity) {
        ItemStack itemStack = new ItemStack(CONTRACT_ITEM_MATERIAL);
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

    /**
     * Check if the specified {@link ItemStack} is a purchase contract item
     * @param plugin The plugin
     * @param itemStack The {@link ItemStack} to check
     * @return {@code true} if it is a contract item, otherwise {@code false}
     */
    public static boolean isPurchaseContractItem(MCSDMarkets plugin, ItemStack itemStack) {
        return isPurchaseContractItem(plugin, null, itemStack);
    }

    /**
     * Check if the specified {@link ItemStack} is a purchase contract and the contract material is the same as specified material
     * @param plugin The plugin
     * @param material The {@link Material} that has to match the contract material
     * @param itemStack The {@link ItemStack} to check
     * @return {@code true} if it is a contract item and the contract material matches the specified material, otherwise {@code false}
     */
    public static boolean isPurchaseContractItem(MCSDMarkets plugin, Material material, ItemStack itemStack) {
        if (itemStack.getType() != CONTRACT_ITEM_MATERIAL || !itemStack.hasItemMeta())
            return false;

        NamespacedKey namespacedKeyItem = new NamespacedKey(plugin, "contract-item");
        NamespacedKey namespacedKeyQuantity = new NamespacedKey(plugin, "contract-quantity");

        PersistentDataContainer container = Objects.requireNonNull(itemStack.getItemMeta()).getPersistentDataContainer();
        return container.has(namespacedKeyItem, PersistentDataType.STRING) && container.has(namespacedKeyQuantity, PersistentDataType.INTEGER)
            && (material == null || Material.getMaterial(Objects.requireNonNull(container.get(namespacedKeyItem, PersistentDataType.STRING), "Namespace key " + namespacedKeyItem.getNamespace() + " is null!")) == material);
    }

    /**
     * Finds the first contract item in the specified inventory that has the same contract material as the specified material
     * @param plugin The plugin
     * @param material The {@link Material} that has to match the contract material
     * @param inventory The inventory to search
     * @return {@link ItemStack} that is a contract item with contract material matching the specified material, otherwise {@code null}
     */
    public static ItemStack findFirstPurchaseContractItem(MCSDMarkets plugin, Material material, Inventory inventory) {
        int slot = indexOfPurchaseContractItem(plugin, material, inventory);
        return slot == -1 ? null : inventory.getItem(slot);
    }

    /**
     * Finds the first contract item in the specified inventory that has the same contract material as the specified material
     * @param plugin The plugin
     * @param material The {@link Material} that has to match the contract material
     * @param inventory The inventory to search
     * @return The index/slot of the contract item with contract material matching the specified material, otherwise -1
     */
    public static int indexOfPurchaseContractItem(MCSDMarkets plugin, Material material, Inventory inventory) {
        Map.Entry<Integer, ? extends ItemStack> contractItemEntry = inventory.all(CONTRACT_ITEM_MATERIAL).entrySet().stream()
            .filter(entry -> isPurchaseContractItem(plugin, material, entry.getValue()))
            .findFirst()
            .orElse(null);

        return contractItemEntry == null ? -1 : contractItemEntry.getKey();
    }

    /**
     * Get the contract item's quantity
     * @param plugin The plugin
     * @param itemStack The {@link ItemStack} that is also a contract item
     * @return The contract item's quantity
     */
    public static int getPurchaseContractQuantity(MCSDMarkets plugin, ItemStack itemStack) {
        if (!isPurchaseContractItem(plugin, itemStack))
            throw new IllegalArgumentException("Item isn't a purchase contract item!");

        NamespacedKey namespacedKeyQuantity = new NamespacedKey(plugin, "contract-quantity");
        PersistentDataContainer container = Objects.requireNonNull(itemStack.getItemMeta()).getPersistentDataContainer();

        return Objects.requireNonNull(container.get(namespacedKeyQuantity, PersistentDataType.INTEGER), "Namespace key " + namespacedKeyQuantity.getNamespace() + " is null!");
    }

    /**
     * Get the contract item's material
     * @param plugin The plugin
     * @param itemStack The {@link ItemStack} that is also a contract item
     * @return The contract item's material
     */
    public static Material getPurchaseContractMaterial(MCSDMarkets plugin, ItemStack itemStack) {
        if (!isPurchaseContractItem(plugin, itemStack))
            throw new IllegalArgumentException("Item isn't a purchase contract item!");

        NamespacedKey namespacedKeyItem = new NamespacedKey(plugin, "contract-item");
        PersistentDataContainer container = Objects.requireNonNull(itemStack.getItemMeta()).getPersistentDataContainer();

        return Material.getMaterial(Objects.requireNonNull(container.get(namespacedKeyItem, PersistentDataType.STRING), "Namespace key " + namespacedKeyItem.getNamespace() + " is null!"));
    }

    /**
     * Take specified quantity of contracts with the specified contract material
     * @param plugin The plugin
     * @param material The contract material
     * @param inventory The inventory
     * @param quantity The amount to take or -1 for all
     * @return The amount taken
     */
    public static int takeContracts(MCSDMarkets plugin, Material material, Inventory inventory, int quantity) {
        Preconditions.checkArgument(quantity > 0 || quantity == -1, "quantity must be > 0 or equal to -1!");
        return take(plugin, inventory, quantity, itemStack -> isPurchaseContractItem(plugin, material, itemStack));
    }

    /**
     * Take the specified quantity of an item where {@link ItemStack#hasItemMeta()} is {@code false}
     * @param plugin The plugin
     * @param material The material of the {@link ItemStack}
     * @param inventory The inventory
     * @param quantity The amount to take or -1 for all
     * @return The amount taken
     */
    public static int takeItems(MCSDMarkets plugin, Material material, Inventory inventory, int quantity) {
        Preconditions.checkArgument(quantity > 0 || quantity == -1, "quantity must be > 0 or equal to -1!");
        return take(plugin, inventory, quantity, itemStack -> itemStack.getType() == material && !isPurchaseContractItem(plugin, itemStack));
    }

    private static int take(MCSDMarkets plugin, Inventory inventory, final int quantity, Predicate<ItemStack> itemPredicate) {
        final boolean takeAll = quantity == -1;
        int taken = 0;
        int foundQuantity = 0;

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack itemStack = inventory.getItem(slot);

            if (itemStack == null || itemPredicate.negate().test(itemStack))
                continue;

            int q = isPurchaseContractItem(plugin, itemStack) ? getPurchaseContractQuantity(plugin, itemStack) : itemStack.getAmount();
            foundQuantity += q;

            if (!takeAll && foundQuantity > quantity) {
                int newQuantity = foundQuantity - quantity;
                int toSub = q - newQuantity;

                if (isPurchaseContractItem(plugin, itemStack)) {
                    subtractQuantity(plugin, itemStack, toSub);
                } else {
                    itemStack.setAmount(newQuantity);
                }

                taken += toSub;
            } else {//remove item
                inventory.setItem(slot, null);
                taken += q;
            }

            if (!takeAll && foundQuantity >= quantity)
                break;
        }

        return taken;
    }

    /**
     * Add to the contract item's quantity
     * @param plugin The plugin
     * @param itemStack The {@link ItemStack} that is also a contract item
     * @param toAdd The amount to add
     */
    public static void addQuantity(MCSDMarkets plugin, ItemStack itemStack, Integer toAdd) {
        modifyQuantity(plugin, itemStack, toAdd, true);
    }

    /**
     * Subtract the contract item's quantity
     * @param plugin The plugin
     * @param itemStack The {@link ItemStack} that is also a contract item
     * @param toSubtract The amount to subtract
     */
    public static void subtractQuantity(MCSDMarkets plugin, ItemStack itemStack, Integer toSubtract) {
        modifyQuantity(plugin, itemStack, toSubtract, false);
    }

    private static void modifyQuantity(MCSDMarkets plugin, ItemStack itemStack, Integer delta, boolean add) {
        if (itemStack.getAmount() != 1)
            throw new IllegalStateException("ItemStack amount has to be 1!");

        if (!isPurchaseContractItem(plugin, itemStack))
            throw new IllegalStateException("Item isn't a purchase contract item!");

        NamespacedKey namespacedKeyQuantity = new NamespacedKey(plugin, "contract-quantity");
        ItemMeta itemMeta = Objects.requireNonNull(itemStack.getItemMeta());
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        int quantity = Objects.requireNonNull(container.get(namespacedKeyQuantity, PersistentDataType.INTEGER), "Namespace key " + namespacedKeyQuantity.getNamespace() + " is null!");

        if (add) {
            quantity += delta;
        } else {
            if (quantity < delta)
                throw new IllegalArgumentException("Delta too large for contract item with quantity: " + quantity);

            quantity -= delta;
        }

        container.set(namespacedKeyQuantity, PersistentDataType.INTEGER, quantity);

        itemMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Item: " + getPurchaseContractMaterial(plugin, itemStack),
            ChatColor.GRAY + "Quantity: " + quantity
        ));

        itemStack.setItemMeta(itemMeta);
    }

}
