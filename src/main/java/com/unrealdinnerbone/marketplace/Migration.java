package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.curseapi.api.CurseAPI;
import com.unrealdinnerbone.marketplace.database.IDBTask;
import com.unrealdinnerbone.marketplace.database.tasks.MigrateV2;
import com.unrealdinnerbone.unreallib.LogHelper;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Migration {

    private static final Logger LOGGER = LogHelper.getLogger();
    private static final Map<Integer, IDBTask> MIGRATIONS = new HashMap<>();

    static {
        MIGRATIONS.put(2, new MigrateV2());
    }

    public static boolean runMigrations(int currentVersion, Tracker.Config config, CFHandler handler, CurseAPI api) {
        //get all versions greater than current version and sort them
        List<Map.Entry<Integer, IDBTask>> list = MIGRATIONS.entrySet()
                .stream()
                .filter(integerIDBTaskEntry -> integerIDBTaskEntry.getKey() > currentVersion)
                .sorted(Map.Entry.comparingByKey()).toList();

        int newVersion = currentVersion;
        for (Map.Entry<Integer, IDBTask> integerIDBTaskEntry : list) {
            String version = integerIDBTaskEntry.getKey().toString();
            IDBTask task = integerIDBTaskEntry.getValue();
            LOGGER.info("Running migration: {} -> {}", newVersion, (version));
            try {
                task.run(config, handler, api);
            } catch (Exception e) {
                LOGGER.error("Failed to run migration: {} -> {}", newVersion, version, e);
                return false;
            }
            newVersion = integerIDBTaskEntry.getKey();
        }
        if(newVersion != currentVersion) {
            LOGGER.info("Updating version from {} to {}", currentVersion, newVersion);
            int finalNewVersion = newVersion;
            handler.executeUpdate("UPDATE curseforge.version SET id = ?;", preparedStatement -> preparedStatement.setInt(1, finalNewVersion));
        }
        return true;

    }
}
