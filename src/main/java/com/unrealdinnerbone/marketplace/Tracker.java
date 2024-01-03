package com.unrealdinnerbone.marketplace;

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
        PostgresConfig postgresConfig = envProvider.loadConfig(PostgresConfig::new);
        LOGGER.info("Host: {} Port: {} Database: {} Username: {} Password: {}", postgresConfig.getHost().get(), postgresConfig.getPort().get(), postgresConfig.getDb().get(), postgresConfig.getUsername().get(), postgresConfig.getPassword().get());
        PostgressHandler postgressHandler = new PostgressHandler(postgresConfig);
        register(postgressHandler, TimeUnit.HOURS, 12, new CurseforgeTracker());
        register(postgressHandler, TimeUnit.HOURS, 1, new CurseforgeStoreTracker());

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
