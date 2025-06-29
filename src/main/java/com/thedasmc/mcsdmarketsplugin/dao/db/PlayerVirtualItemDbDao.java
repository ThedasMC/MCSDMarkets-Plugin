package com.thedasmc.mcsdmarketsplugin.dao.db;

import com.thedasmc.mcsdmarketsplugin.config.SessionFactoryManager;
import com.thedasmc.mcsdmarketsplugin.dao.PlayerVirtualItemDao;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItemPK;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;

import java.util.Optional;

public class PlayerVirtualItemDbDao implements PlayerVirtualItemDao {

    private final SessionFactoryManager sessionFactoryManager;

    public PlayerVirtualItemDbDao(SessionFactoryManager sessionFactoryManager) {
        this.sessionFactoryManager = sessionFactoryManager;
    }

    @Override
    public Optional<PlayerVirtualItem> findById(PlayerVirtualItemPK pk) {
        try (StatelessSession session = sessionFactoryManager.openReadOnlySession()) {
            return Optional.ofNullable(session.get(PlayerVirtualItem.class, pk));
        }
    }

    @Override
    public PlayerVirtualItem save(PlayerVirtualItem playerVirtualItem) {
        try (Session session = sessionFactoryManager.openSession()) {
            Transaction transaction = session.beginTransaction();
            playerVirtualItem = session.merge(playerVirtualItem);
            transaction.commit();
        }

        return playerVirtualItem;
    }

    @Override
    public void delete(PlayerVirtualItem playerVirtualItem) {
        try (Session session = sessionFactoryManager.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.remove(playerVirtualItem);
            transaction.commit();
        }
    }

    @Override
    public void shutdown() {
        sessionFactoryManager.shutdown();
    }
}
