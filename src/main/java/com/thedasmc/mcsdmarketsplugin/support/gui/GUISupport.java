package com.thedasmc.mcsdmarketsplugin.support.gui;

import com.google.common.base.Preconditions;
import com.thedasmc.mcsdmarketsapi.response.impl.ItemPageResponse;
import com.thedasmc.mcsdmarketsapi.response.impl.ItemResponse;
import com.thedasmc.mcsdmarketsapi.response.impl.PageResponse;
import com.thedasmc.mcsdmarketsplugin.support.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GUISupport {

    public static final int INVENTORY_SIZE = 54;
    public static final int PREVIOUS_BUTTON_SLOT = INVENTORY_SIZE - 9;
    public static final int CLOSE_BUTTON_SLOT = INVENTORY_SIZE - 5;
    public static final int NEXT_BUTTON_SLOT = INVENTORY_SIZE - 1;

    private final Map<UUID, PageInfo> pageTracker = new HashMap<>();

    public void openMenu(Player player, ItemPageResponse response) {
        Inventory inventory = getMenu(response);

        PageInfo pageInfo = new PageInfo(player.getUniqueId(), response.getPageInfo().getPage(), inventory);
        pageTracker.put(player.getUniqueId(), pageInfo);

        player.openInventory(inventory);
    }

    public void inventoryClosed(UUID uuid) {
        pageTracker.remove(uuid);
    }

    public PageInfo getPageInfo(UUID uuid) {
        return pageTracker.get(uuid);
    }

    private Inventory getMenu(ItemPageResponse response) {
        Preconditions.checkArgument(response.getItems().size() <= INVENTORY_SIZE - 9);
        PageResponse pageInfo = response.getPageInfo();
        List<ItemResponse> itemResponseList = response.getItems();

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, ChatColor.GOLD + "" + ChatColor.BOLD + "MCSDMarkets Items");

        //Fill inventory with items for sale
        itemResponseList.forEach(itemResponse -> {
            try {
                inventory.addItem(createItem(itemResponse));
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Failed to get item for items menu: " + e.getMessage());
            }
        });

        //Add navigation buttons
        ItemStack previousButton = new ItemStack(Material.ARROW);
        ItemMeta previousMeta = Objects.requireNonNull(previousButton.getItemMeta());
        previousMeta.setDisplayName(ChatColor.GRAY + "-->");
        previousButton.setItemMeta(previousMeta);

        ItemStack nextButton = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = Objects.requireNonNull(nextButton.getItemMeta());
        nextMeta.setDisplayName(ChatColor.GRAY + "<--");
        nextButton.setItemMeta(nextMeta);

        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = Objects.requireNonNull(closeButton.getItemMeta());
        closeMeta.setDisplayName(ChatColor.RED + "Close");
        closeButton.setItemMeta(closeMeta);

        if (pageInfo.getPage() > 0)
            inventory.setItem(PREVIOUS_BUTTON_SLOT, previousButton);

        inventory.setItem(CLOSE_BUTTON_SLOT, closeButton);

        if (pageInfo.getPage() < pageInfo.getPages() - 1)
            inventory.setItem(NEXT_BUTTON_SLOT, nextButton);

        return inventory;
    }

    private ItemStack createItem(ItemResponse itemResponse) throws IllegalArgumentException {
        Optional<Material> optionalMaterial = ItemUtil.getMaterial(itemResponse.getMaterial());

        if (!optionalMaterial.isPresent())
            throw new IllegalArgumentException("Can't find associated Material for ItemResponse material " + itemResponse.getMaterial() + "!");

        ItemStack itemStack = new ItemStack(optionalMaterial.get());
        ItemMeta itemMeta = Objects.requireNonNull(itemStack.getItemMeta());
        itemMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Price: " + ChatColor.GREEN + "$" + itemResponse.getCurrentPrice().toPlainString(),
            ChatColor.GRAY + "Original Price: " + ChatColor.GREEN + "$" + itemResponse.getBasePrice().toPlainString(),
            ChatColor.GRAY + "Inventory: " + ChatColor.AQUA + itemResponse.getInventory()
        ));

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

}
