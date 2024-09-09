package com.unrealdinnerbone.marketplace.database.tasks;

import com.unrealdinnerbone.curseapi.api.CurseAPI;
import com.unrealdinnerbone.marketplace.CFHandler;
import com.unrealdinnerbone.marketplace.CurseforgeTracker;
import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.marketplace.database.IDBTask;
import com.unrealdinnerbone.unreallib.LogHelper;
import org.slf4j.Logger;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MigrateV4 implements IDBTask {

    private static final Logger LOGGER = LogHelper.getLogger();

    @Override
    public void run(Tracker.Config config, CFHandler handler, CurseAPI curseAPI) throws Exception {
        LOGGER.info("Updating loader_versions and java_versions");
        ResultSet set = handler.getSet("select * from curseforge.file");
        while (set.next()) {
            int id = set.getInt("id");
            LOGGER.info("Updating file id: {}", id);
            String[] versions = (String[]) set.getArray("versions").getArray();
            String[] loaderVersionsArray = (String[]) set.getArray("loader_versions").getArray();
            String[] javaVersionsArray = (String[]) set.getArray("java_versions").getArray();
            List<String> minecraftVersions = new ArrayList<>();
            List<String> loaderVersions = new ArrayList<>();
            List<String> javaVersions = new ArrayList<>();
            loaderVersions.addAll(List.of(loaderVersionsArray));
            javaVersions.addAll(List.of(javaVersionsArray));

            for (String version : versions) {
                if(CurseforgeTracker.isStringLoader(version)) {
                    loaderVersions.add(version);
                    continue;
                }
                if(CurseforgeTracker.isStringJava(version)) {
                    javaVersions.add(version);
                    continue;
                }
                minecraftVersions.add(version);
            }
            handler.executeUpdate("""
                    update curseforge.file
                    set versions = ?,
                        loader_versions = ?,
                        java_versions = ?
                    where id = ?;
                    """, preparedStatement -> {
                preparedStatement.setArray(1, handler.createArray("text", minecraftVersions.toArray()));
                preparedStatement.setArray(2, handler.createArray("text", loaderVersions.toArray()));
                preparedStatement.setArray(3, handler.createArray("text", javaVersions.toArray()));
                preparedStatement.setInt(4, id);
            });
        }
        LOGGER.info("Updatied loader_versions and java_versions for projects");
    }
}
