package com.thedasmc.mcsdmarketsplugin.dao.db.impl;

import com.thedasmc.mcsdmarketsplugin.config.SessionFactoryManager;
import com.thedasmc.mcsdmarketsplugin.dao.PlayerVirtualItemDao;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItemPK;
import com.thedasmc.mcsdmarketsplugin.support.PageResult;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;

import java.util.List;
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
    public PageResult<PlayerVirtualItem> findAllByPlayerId(String playerId, int page, int pageSize) {
        try (StatelessSession session = sessionFactoryManager.openReadOnlySession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<PlayerVirtualItem> cq = cb.createQuery(PlayerVirtualItem.class);

            Root<PlayerVirtualItem> playerVirtualItem = cq.from(PlayerVirtualItem.class);
            cq.select(playerVirtualItem)
                .where(cb.equal(playerVirtualItem.get("id").get("uuid"), playerId))
                .orderBy(cb.asc(playerVirtualItem.get("id").get("material")));

            List<PlayerVirtualItem> resultList = session.createQuery(cq)
                .setFirstResult(page * pageSize)
                .setMaxResults(pageSize + 1)//Add 1 to determine if there is a next page
                .getResultList();

            return new PageResult<>(resultList.subList(0, Math.min(resultList.size(), pageSize)), resultList.size() > pageSize);
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
}
