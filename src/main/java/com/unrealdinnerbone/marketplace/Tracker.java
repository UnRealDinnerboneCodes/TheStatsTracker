package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.config.ConfigManager;
import com.unrealdinnerbone.postgresslib.PostgresConfig;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.TaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class Tracker
{
    public static final Logger LOGGER = LoggerFactory.getLogger(Tracker.class);


    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        ConfigManager manager = ConfigManager.createSimpleEnvPropertyConfigManger();
        PostgresConfig postgresConfig = manager.loadConfig(new PostgresConfig());
        PostgressHandler postgressHandler = new PostgressHandler(postgresConfig);
//        register(postgressHandler, TimeUnit.MINUTES, 30, new ModpackTracker());
        register(postgressHandler, TimeUnit.HOURS, 1, new MarketplaceTracker());

    }

    public static void register(PostgressHandler handler, TimeUnit unit, int time, IStatsTracker tracker) {
        TaskScheduler.scheduleRepeatingTask(time, unit, () -> tracker.run(handler));
    }


}
