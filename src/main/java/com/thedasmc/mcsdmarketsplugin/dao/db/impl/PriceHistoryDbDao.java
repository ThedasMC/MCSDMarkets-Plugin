package com.thedasmc.mcsdmarketsplugin.dao.db.impl;

import com.thedasmc.mcsdmarketsplugin.config.SessionFactoryManager;
import com.thedasmc.mcsdmarketsplugin.dao.PriceHistoryMapDao;
import com.thedasmc.mcsdmarketsplugin.model.PriceHistoryMap;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;

import java.util.Optional;

public class PriceHistoryDbDao implements PriceHistoryMapDao {

    private final SessionFactoryManager sessionFactoryManager;

    public PriceHistoryDbDao(SessionFactoryManager sessionFactoryManager) {
        this.sessionFactoryManager = sessionFactoryManager;
    }

    @Override
    public Optional<PriceHistoryMap> findById(int id) {
       try (StatelessSession session = sessionFactoryManager.openReadOnlySession()) {
           return Optional.ofNullable(session.get(PriceHistoryMap.class, id));
       }
    }

    @Override
    public PriceHistoryMap save(PriceHistoryMap priceHistoryMap) {
        try (Session session = sessionFactoryManager.openSession()) {
            Transaction transaction = session.beginTransaction();
            priceHistoryMap = session.merge(priceHistoryMap);
            transaction.commit();
        }

        return priceHistoryMap;
    }
}
