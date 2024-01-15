package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.config.api.ConfigCreator;
import com.unrealdinnerbone.config.api.exception.ConfigException;
import com.unrealdinnerbone.config.config.ConfigValue;
import com.unrealdinnerbone.config.impl.provider.EnvProvider;
import com.unrealdinnerbone.curseapi.api.CurseAPI;
import com.unrealdinnerbone.curseapi.quaries.ModQuery;
import com.unrealdinnerbone.marketplace.database.tasks.CreateTask;
import com.unrealdinnerbone.postgresslib.PostgresConfig;
import com.unrealdinnerbone.unreallib.LogHelper;
import com.unrealdinnerbone.unreallib.TaskScheduler;
import org.slf4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Tracker
{
    public static final Logger LOGGER = LogHelper.getLogger();

    private static final EnvProvider ENV_PROVIDER = new EnvProvider();

    private static final PostgresConfig POSTGRES_CONFIG = ENV_PROVIDER.loadConfig("postgres", PostgresConfig::new);

    private static final Config GENERAL_CONFIG = ENV_PROVIDER.loadConfig("general", Config::new);


    public static void main(String[] args) {
        LOGGER.info("Starting Curseforge Tracker!");

        try {
            ENV_PROVIDER.read();
        } catch (ConfigException e) {
            LOGGER.error("Error while loading config", e);
            return;
        }
        if(GENERAL_CONFIG.getDebug().get()) {
            LOGGER.info("Host: {} Port: {} Database: {} Username: {} Password: {}", POSTGRES_CONFIG.getHost().get(), POSTGRES_CONFIG.getPort().get(), POSTGRES_CONFIG.getDb().get(), POSTGRES_CONFIG.getUsername().get(), POSTGRES_CONFIG.getPassword().get());

        }
        LOGGER.info("Running CurseForge tracker every {} hour(s)", GENERAL_CONFIG.getTime().get());
        CFHandler postgresHandler;
        try {
            postgresHandler = new CFHandler(POSTGRES_CONFIG);
        } catch (SQLException e) {
            LOGGER.error("Error while connecting to database", e);
            return;
        }
        CurseAPI curseAPI = new CurseAPI(GENERAL_CONFIG.getCurseApiKey().get(), GENERAL_CONFIG.getCurseApiUrl().get());
        if(runTasks(GENERAL_CONFIG, postgresHandler, curseAPI)) {
            register(curseAPI, postgresHandler, TimeUnit.HOURS, GENERAL_CONFIG.getTime().get(), new CurseforgeTracker(GENERAL_CONFIG, curseAPI));
        }
    }


    public static boolean runTasks(Tracker.Config config, CFHandler handler, CurseAPI curseAPI) {
        try {
            new CreateTask().run(config, handler, curseAPI);
        }catch (SQLException e) {
            LOGGER.error("Failed to create database", e);
            return false;
        }


        int version;
        try {
            ResultSet set = handler.getSet("select * from curseforge.version");
            if(set.next()) {
                version = set.getInt("id");
            }else {
                LOGGER.error("Failed to get version");
                return false;
            }
        }catch (SQLException e) {
            LOGGER.error("Failed to get version", e);
            return false;
        }
        LOGGER.info("Current Version: {}", version);
        LOGGER.info("Running Migrations");
        if(Migration.runMigrations(version, config, handler, curseAPI)) {
            LOGGER.info("Finished Migrations");
            return true;
        }else {
            LOGGER.error("Failed to run migrations");
            return false;
        }

    }

    public static void register(CurseAPI curseAPI, CFHandler handler, TimeUnit unit, int time, IStatsTracker tracker) {
        TaskScheduler.scheduleRepeatingTask(time, unit, new TimerTask() {
            @Override
            public void run() {
                try {
                    tracker.run(handler, curseAPI);
                }catch (Exception e) {
                    LOGGER.error("Error while running tracker", e);
                }
            }
        });
    }

    public static class Config {

        private final ConfigValue<Integer> time;
        private final ConfigValue<Boolean> enableStore;

        private final ConfigValue<Boolean> discordEnabled;

        private final ConfigValue<Boolean> trackThings;
        private final ConfigValue<Boolean> debug;

        private final ConfigValue<String> curseApiUrl;

        private final ConfigValue<String> curseApiKey;

        private final ConfigValue<Map<String, Integer>> slugMap;
        public Config(ConfigCreator creator) {
            this.time = creator.createInteger("time", 12);
            this.enableStore = creator.createBoolean("enable_store", false);
            this.discordEnabled = creator.createBoolean("discord_enabled", true);
            this.trackThings = creator.createBoolean("track_things", true);
            this.debug = creator.createBoolean("debug", false);
            this.curseApiUrl = creator.createString("curse_api_url", "https://api.curseforge.com/v1/");
            this.curseApiKey = creator.createString("curse_api_key", "");
            this.slugMap = creator.createMap("slug_map", new HashMap<>(), Integer.class);
        }

        public ConfigValue<Map<String, Integer>> getSlugMap() {
            return slugMap;
        }

        public ConfigValue<String> getCurseApiKey() {
            return curseApiKey;
        }

        public ConfigValue<String> getCurseApiUrl() {
            return curseApiUrl;
        }

        public ConfigValue<Boolean> getDebug() {
            return debug;
        }

        public ConfigValue<Boolean> getTrackThings() {
            return trackThings;
        }

        public ConfigValue<Boolean> getDiscordEnabled() {
            return discordEnabled;
        }

        public ConfigValue<Boolean> getEnableStore() {
            return enableStore;
        }
        public ConfigValue<Integer> getTime() {
            return time;
        }
    }


}
