package com.thedasmc.mcsdmarketsplugin.support.gui;

import com.google.common.base.Preconditions;
import com.thedasmc.mcsdmarketsapi.enums.TransactionType;
import com.thedasmc.mcsdmarketsapi.response.impl.*;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItemPK;
import com.thedasmc.mcsdmarketsplugin.support.ItemUtil;
import com.thedasmc.mcsdmarketsplugin.support.PageResult;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.*;

public class GUISupport {

    public static final int INVENTORY_SIZE = 54;
    public static final int PREVIOUS_BUTTON_SLOT = INVENTORY_SIZE - 9;
    public static final int CLOSE_BUTTON_SLOT = INVENTORY_SIZE - 5;
    public static final int NEXT_BUTTON_SLOT = INVENTORY_SIZE - 1;

    private final Map<UUID, PageInfo> limitOrderPageTracker = new HashMap<>();
    private final Map<UUID, PageInfo> portfolioPageTracker = new HashMap<>();
    private final Map<UUID, PageInfo> itemMenuPageTracker = new HashMap<>();

    /* Limit Order Menu */

    public void openLimitOrdersMenu(Player player, LimitOrderPageResponse limitOrderPageResponse, int page) {
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, "Limit Orders");
        List<LimitOrderResponse> limitOrders = limitOrderPageResponse.getLimitOrders();

        for (LimitOrderResponse limitOrder : limitOrders) {
            Material material = ItemUtil.getMaterial(limitOrder.getMaterial())
                    .orElseThrow(() -> new IllegalStateException("Can't find associated Material for LimitOrder material " + limitOrder.getMaterial() + "!"));

            ItemStack itemStack = new ItemStack(material, 1);
            ItemMeta itemMeta = itemStack.getItemMeta();

            List<String> lore = itemMeta.getLore() == null ? new ArrayList<>(6) : itemMeta.getLore();
            lore.add(ChatColor.DARK_AQUA + "Type: " + limitOrder.getTransactionType());
            lore.add(ChatColor.DARK_AQUA + "Limit Price: " + ChatColor.GREEN + "$" + limitOrder.getLimitPrice().toPlainString());
            lore.add(ChatColor.DARK_AQUA + "Quantity: " + limitOrder.getQuantity());
            lore.add(ChatColor.DARK_AQUA + "Quantity Filled: " + limitOrder.getQuantityFulfilled());
            lore.add(ChatColor.DARK_AQUA + "Amount Filled: " + ChatColor.GREEN + "$" + limitOrder.getFulfilledAmount().toPlainString());
            lore.add(ChatColor.DARK_AQUA + "Withdrawable: " + ChatColor.GREEN + "$" + getWithdrawableAmount(limitOrder));

            itemMeta.setLore(lore);
            itemStack.setItemMeta(itemMeta);
        }

        if (page > 1)
            inventory.setItem(PREVIOUS_BUTTON_SLOT, getPreviousButton());

        inventory.setItem(CLOSE_BUTTON_SLOT, getCloseButton());

        if (limitOrderPageResponse.getPages() > page)
            inventory.setItem(NEXT_BUTTON_SLOT, getNextButton());

        PageInfo pageInfo = new PageInfo(player.getUniqueId(), page, inventory);
        limitOrderPageTracker.put(player.getUniqueId(), pageInfo);
        player.openInventory(inventory);
    }

    private static BigDecimal getWithdrawableAmount(LimitOrderResponse limitOrder) {
        BigDecimal withdrawable;

        if (limitOrder.getTransactionType() == TransactionType.SALE) {
            withdrawable = limitOrder.getFulfilledAmount().subtract(limitOrder.getFulfilledAmount());
        } else {
            //For purchases, it is basically a refund of the difference between actual cost and the expected cost from when the limit order was submitted
            BigDecimal fulfilledAmount = limitOrder.getFulfilledAmount();
            BigDecimal expectedFulfillmentAmount = limitOrder.getLimitPrice().multiply(BigDecimal.valueOf(limitOrder.getQuantityFulfilled()));
            withdrawable = expectedFulfillmentAmount.subtract(fulfilledAmount).subtract(limitOrder.getFulfillmentPaid());
        }
        return withdrawable;
    }

    /* Portfolio Menu */

    public void openPortfolio(Player player, PageResult<PlayerVirtualItem> playerVirtualItemsPage, int page) {
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, "Portfolio");
        List<PlayerVirtualItem> playerVirtualItems = playerVirtualItemsPage.getResults();

        for (int i = 0; i < playerVirtualItems.size(); i++) {
            PlayerVirtualItem playerVirtualItem = playerVirtualItems.get(i);
            PlayerVirtualItemPK id = playerVirtualItem.getId();

            Optional<Material> optionalMaterial = ItemUtil.getMaterial(id.getMaterial());

            if (optionalMaterial.isEmpty())
                throw new IllegalStateException("Can't find associated Material for PlayerVirtualItem material " + id.getMaterial());

            ItemStack itemStack = new ItemStack(optionalMaterial.get(), 1);
            ItemMeta itemMeta = itemStack.getItemMeta();
            List<String> lore = itemMeta.getLore() == null ? new ArrayList<>(1) : itemMeta.getLore();
            lore.add(ChatColor.DARK_AQUA + "Quantity: " + playerVirtualItem.getQuantity());
            itemMeta.setLore(lore);
            itemStack.setItemMeta(itemMeta);

            inventory.setItem(i, itemStack);
        }

        if (page > 1)
            inventory.setItem(PREVIOUS_BUTTON_SLOT, getPreviousButton());

        inventory.setItem(CLOSE_BUTTON_SLOT, getCloseButton());

        if (playerVirtualItemsPage.getHasNext())
            inventory.setItem(NEXT_BUTTON_SLOT, getNextButton());

        PageInfo pageInfo = new PageInfo(player.getUniqueId(), page, inventory);
        portfolioPageTracker.put(player.getUniqueId(), pageInfo);
        player.openInventory(inventory);
    }

    public PageInfo getPortfolioPageInfo(UUID uuid) {
        return portfolioPageTracker.get(uuid);
    }

    /* Item Menu */

    public void openItemMenu(Player player, ItemPageResponse response) {
        Inventory inventory = getItemMenu(response);

        PageInfo pageInfo = new PageInfo(player.getUniqueId(), response.getPageInfo().getPage(), inventory);
        itemMenuPageTracker.put(player.getUniqueId(), pageInfo);

        player.openInventory(inventory);
    }

    public void inventoryClosed(UUID uuid) {
        itemMenuPageTracker.remove(uuid);
        portfolioPageTracker.remove(uuid);
    }

    public PageInfo getItemMenuPageInfo(UUID uuid) {
        return itemMenuPageTracker.get(uuid);
    }

    private Inventory getItemMenu(ItemPageResponse response) {
        Preconditions.checkArgument(response.getItems().size() <= INVENTORY_SIZE - 9);
        PageResponse pageInfo = response.getPageInfo();
        List<ItemResponse> itemResponseList = response.getItems();

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, ChatColor.GOLD + "" + ChatColor.BOLD + "MCSDMarkets Items");

        //Fill inventory with items for sale
        itemResponseList.forEach(itemResponse -> {
            try {
                inventory.addItem(createItemMenuItem(itemResponse));
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Failed to get item for items menu: " + e.getMessage());
            }
        });

        //Add navigation buttons
        if (pageInfo.getPage() > 0)
            inventory.setItem(PREVIOUS_BUTTON_SLOT, getPreviousButton());

        inventory.setItem(CLOSE_BUTTON_SLOT, getCloseButton());

        if (pageInfo.getPage() < pageInfo.getPages() - 1)
            inventory.setItem(NEXT_BUTTON_SLOT, getNextButton());

        return inventory;
    }

    private ItemStack getPreviousButton() {
        ItemStack previousButton = new ItemStack(Material.ARROW);
        ItemMeta previousMeta = Objects.requireNonNull(previousButton.getItemMeta());
        previousMeta.setDisplayName(ChatColor.GRAY + "<--");
        previousButton.setItemMeta(previousMeta);

        return previousButton;
    }

    private ItemStack getNextButton() {
        ItemStack nextButton = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = Objects.requireNonNull(nextButton.getItemMeta());
        nextMeta.setDisplayName(ChatColor.GRAY + "-->");
        nextButton.setItemMeta(nextMeta);

        return nextButton;
    }

    private ItemStack getCloseButton() {
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = Objects.requireNonNull(closeButton.getItemMeta());
        closeMeta.setDisplayName(ChatColor.RED + "Close");
        closeButton.setItemMeta(closeMeta);

        return closeButton;
    }

    private ItemStack createItemMenuItem(ItemResponse itemResponse) throws IllegalArgumentException {
        Optional<Material> optionalMaterial = ItemUtil.getMaterial(itemResponse.getMaterial());

        if (optionalMaterial.isEmpty())
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
