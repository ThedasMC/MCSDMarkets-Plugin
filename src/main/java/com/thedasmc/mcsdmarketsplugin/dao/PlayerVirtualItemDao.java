package com.thedasmc.mcsdmarketsplugin.dao;

import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItemPK;
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
     * Save a player virtual item to the database. If the item already exists, it will be updated
     * @param playerVirtualItem The player virtual item to save
     * @return The saved player virtual item, with a populated version number
     * @throws OptimisticLockException If the item has been updated since it was retrieved
     */
    PlayerVirtualItem save(PlayerVirtualItem playerVirtualItem);

    /**
     * Delete a player virtual item by its primary key. If the item does not exist, nothing will happen
     * @param pk The primary key of the player virtual item to delete
     */
    void deleteById(PlayerVirtualItemPK pk);

    /**
     * Shutdown the DAO. This will close any open connections and free any resources
     */
    void shutdown();

}
