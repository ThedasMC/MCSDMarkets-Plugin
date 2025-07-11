package com.thedasmc.mcsdmarketsplugin.config;

import com.mysql.cj.jdbc.Driver;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import org.bukkit.configuration.file.FileConfiguration;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;

public class SessionFactoryManager {

    private final SessionFactory sessionFactory;

    public SessionFactoryManager(MCSDMarkets plugin) {
        FileConfiguration pluginConfig = plugin.getConfig();
        final String dbHost = pluginConfig.getString("database.host");
        final String dbPort = pluginConfig.getString("database.port");
        final String dbUser = pluginConfig.getString("database.user");
        final String dbPassword = pluginConfig.getString("database.password");
        final String dbName = pluginConfig.getString("database.name");

        Configuration cfg = new Configuration()
            .addAnnotatedClass(PlayerVirtualItem.class)
            .setProperty("hibernate.connection.provider_class", HikariCPConnectionProvider.class.getName())
            .setProperty("hibernate.transaction.jta.platform", NoJtaPlatform.class.getName())
            .setProperty("hibernate.hikari.minimumIdle", "1")
            .setProperty("hibernate.hikari.maximumPoolSize", "10")
            .setProperty("hibernate.connection.url", "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName)
            .setProperty("hibernate.connection.username", dbUser)
            .setProperty("hibernate.connection.password", dbPassword)
            .setProperty("hibernate.connection.driver_class", Driver.class.getName())
            .setProperty("hibernate.show_sql", "false")
            .setProperty("hibernate.hbm2ddl.auto", "update");

        this.sessionFactory = cfg.buildSessionFactory();
    }

    public Session openSession() {
        return this.sessionFactory.openSession();
    }

    public StatelessSession openReadOnlySession() {
        return this.sessionFactory.openStatelessSession();
    }

    public void shutdown() {
        this.sessionFactory.close();
    }

}
