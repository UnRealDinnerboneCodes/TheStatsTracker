package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.config.ConfigManager;
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
        ConfigManager manager = ConfigManager.createSimpleEnvPropertyConfigManger();
        PostgresConfig postgresConfig = manager.loadConfig("postgres", PostgresConfig::new);
        PostgressHandler postgressHandler = new PostgressHandler(postgresConfig);
        register(postgressHandler, TimeUnit.HOURS, 12, new CurseforgeTracker());

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
