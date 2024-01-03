package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.config.api.ConfigCreator;
import com.unrealdinnerbone.config.api.exception.ConfigException;
import com.unrealdinnerbone.config.config.ConfigValue;
import com.unrealdinnerbone.config.impl.provider.EnvProvider;
import com.unrealdinnerbone.postgresslib.PostgresConfig;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.LogHelper;
import com.unrealdinnerbone.unreallib.TaskScheduler;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class Tracker
{
    public static final Logger LOGGER = LogHelper.getLogger();


    public static void main(String[] args) throws SQLException {
        LOGGER.info("Loading Me!");
        EnvProvider<PostgresConfig> envProvider = new EnvProvider<>();
        PostgresConfig postgresConfig = envProvider.loadConfig("postgres", PostgresConfig::new);
        Config config = envProvider.loadConfig("general", Config::new);
        try {
            envProvider.read();
        } catch (ConfigException e) {
            LOGGER.error("Error while loading config", e);
            return;
        }
        LOGGER.info("Host: {} Port: {} Database: {} Username: {} Password: {}", postgresConfig.getHost().get(), postgresConfig.getPort().get(), postgresConfig.getDb().get(), postgresConfig.getUsername().get(), postgresConfig.getPassword().get());
        LOGGER.info("Running tracker every {} hours", config.getTime().get());
        PostgressHandler postgressHandler = new PostgressHandler(postgresConfig);
        register(postgressHandler, TimeUnit.HOURS, config.getTime().get(), new CurseforgeTracker());
        if(config.getEnableStore().get()) {
            register(postgressHandler, TimeUnit.HOURS, config.getTime().get(), new CurseforgeStoreTracker());
        }

    }

    public static void register(PostgressHandler handler, TimeUnit unit, int time, IStatsTracker tracker) {
        TaskScheduler.scheduleRepeatingTask(time, unit, new TimerTask() {
            @Override
            public void run() {
                try {
                    tracker.run(handler);
                }catch (Exception e) {
                    LOGGER.error("Error while running tracker", e);
                }
            }
        });
    }

    public static class Config {

        private final ConfigValue<Integer> time;
        private final ConfigValue<Boolean> enableStore;
        public Config(ConfigCreator creator) {
            this.time = creator.createInteger("time", 12);
            this.enableStore = creator.createBoolean("enableStore", false);
        }

        public ConfigValue<Boolean> getEnableStore() {
            return enableStore;
        }
        public ConfigValue<Integer> getTime() {
            return time;
        }
    }


}
