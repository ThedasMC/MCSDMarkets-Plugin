package com.thedasmc.mcsdmarketsplugin.support;

import com.thedasmc.mcsdmarketsplugin.config.SessionFactoryManager;
import com.thedasmc.mcsdmarketsplugin.dao.PlayerVirtualItemDao;
import com.thedasmc.mcsdmarketsplugin.dao.PriceHistoryMapDao;
import jakarta.validation.ValidatorFactory;

public class PersistenceManager {

    private ValidatorFactory fileValidatorFactory;
    private SessionFactoryManager sessionFactoryManager;

    private PlayerVirtualItemDao playerVirtualItemDao;
    private PriceHistoryMapDao priceHistoryMapDao;

    public ValidatorFactory getFileValidatorFactory() {
        return fileValidatorFactory;
    }

    public void setFileValidatorFactory(ValidatorFactory fileValidatorFactory) {
        this.fileValidatorFactory = fileValidatorFactory;
    }

    public SessionFactoryManager getSessionFactoryManager() {
        return sessionFactoryManager;
    }

    public void setSessionFactoryManager(SessionFactoryManager sessionFactoryManager) {
        this.sessionFactoryManager = sessionFactoryManager;
    }

    public PlayerVirtualItemDao getPlayerVirtualItemDao() {
        return playerVirtualItemDao;
    }

    public void setPlayerVirtualItemDao(PlayerVirtualItemDao playerVirtualItemDao) {
        this.playerVirtualItemDao = playerVirtualItemDao;
    }

    public PriceHistoryMapDao getPriceHistoryMapDao() {
        return priceHistoryMapDao;
    }

    public void setPriceHistoryMapDao(PriceHistoryMapDao priceHistoryMapDao) {
        this.priceHistoryMapDao = priceHistoryMapDao;
    }

    public void shutdown() {
        if (fileValidatorFactory != null)
            fileValidatorFactory.close();

        if (sessionFactoryManager != null)
            sessionFactoryManager.shutdown();
    }
}
