package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.config.ConfigManager;
import com.unrealdinnerbone.postgresslib.PostgresConfig;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.TaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class Tracker
{
    public static final Logger LOGGER = LoggerFactory.getLogger(Tracker.class);


    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        LOGGER.info("Loading Me!");
        ConfigManager manager = ConfigManager.createSimpleEnvPropertyConfigManger();
        PostgresConfig postgresConfig = manager.loadConfig("postgres", PostgresConfig::new);
        postgresConfig.getDb().getValue();
        postgresConfig.getHost().getValue();
        postgresConfig.getPassword().getValue();
        postgresConfig.getUsername().getValue();
        postgresConfig.getPort().getValue();
        PostgressHandler postgressHandler = new PostgressHandler(postgresConfig);
        register(postgressHandler, TimeUnit.MINUTES, 30, new ModpackTracker());
        register(postgressHandler, TimeUnit.HOURS, 12, new CurseforgeTracker());
        register(postgressHandler, TimeUnit.HOURS, 1, new MarketplaceTracker());

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


}
