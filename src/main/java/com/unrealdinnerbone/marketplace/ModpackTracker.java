package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.modpackapi.ModpackAPI;
import com.unrealdinnerbone.modpackapi.api.modpack.ModpackManifest;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.LogHelper;
import com.unrealdinnerbone.unreallib.exception.WebResultException;
import com.unrealdinnerbone.unreallib.json.exception.JsonParseException;
import org.slf4j.Logger;

import java.util.concurrent.ExecutionException;

public class ModpackTracker implements IStatsTracker {

    private static final Logger LOGGER = LogHelper.getLogger();

    @Override
    public void run(PostgressHandler handler) throws ExecutionException, InterruptedException, WebResultException, JsonParseException {
        LOGGER.info("Getting Modpacks...");
        int[] packs = ModpackAPI.Modpacks.getAll().getNow().packs();
        LOGGER.info("Found {} packs", packs.length);
        for (int pack : packs) {
            ModpackManifest modpack = ModpackAPI.Modpacks.getModpack(pack).getNow();
            LOGGER.info("{} - Installs: {} Plays: {}", modpack.name(), modpack.installs(), modpack.plays());
            handleData(handler, System.currentTimeMillis(), modpack);
        }
    }

    public static void handleData(PostgressHandler handler, long time, ModpackManifest modpack) {
        handler.executeUpdate("INSERT INTO modpacks.modpack (id, installs, plays, name, time) VALUES (?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setInt(1, modpack.id());
            preparedStatement.setInt(2, modpack.installs());
            preparedStatement.setInt(3, modpack.plays());
            preparedStatement.setString(4, modpack.name());
            preparedStatement.setLong(5, time);
        });
    }
}
