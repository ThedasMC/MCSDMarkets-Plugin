package com.thedasmc.mcsdmarketsplugin.config;

import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import org.bukkit.configuration.file.FileConfiguration;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.cfg.Configuration;

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
            .setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver")
            .setProperty("hibernate.connection.url", "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName)
            .setProperty("hibernate.connection.username", dbUser)
            .setProperty("hibernate.connection.password", dbPassword)
            .setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
            .setProperty("hibernate.show_sql", "false")
            .setProperty("hibernate.hbm2ddl.auto", "create-update");

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
