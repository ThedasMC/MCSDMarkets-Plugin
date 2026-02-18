package com.thedasmc.mcsdmarketsplugin.dao;

import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItemPK;
import com.thedasmc.mcsdmarketsplugin.support.PageResult;
import jakarta.persistence.OptimisticLockException;

import java.util.Optional;

public interface PlayerVirtualItemDao {

    /**
     * Find a player virtual item by its primary key
     * @param pk The primary key of the player virtual item
     * @return The player virtual item if found, otherwise empty
     */
    Optional<PlayerVirtualItem> findById(PlayerVirtualItemPK pk);

    /**
     * Find all player virtual items for a player, paginated
     * @param playerId The player's UUID
     * @param page The page to get. The first page is page 0
     * @param pageSize The number of items per page
     * @return A list of player virtual items
     */
    PageResult<PlayerVirtualItem> findAllByPlayerId(String playerId, int page, int pageSize);

    /**
     * Save a player virtual item to the database. If the item already exists, it will be updated
     * @param playerVirtualItem The player virtual item to save
     * @return The saved player virtual item, with a populated version number
     * @throws OptimisticLockException If the item has been updated since it was retrieved
     */
    PlayerVirtualItem save(PlayerVirtualItem playerVirtualItem);

    void delete(PlayerVirtualItem playerVirtualItem);

}
