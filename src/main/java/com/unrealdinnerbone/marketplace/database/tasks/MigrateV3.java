package com.unrealdinnerbone.marketplace.database.tasks;

import com.unrealdinnerbone.curseapi.api.CurseAPI;
import com.unrealdinnerbone.marketplace.CFHandler;
import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.marketplace.database.IDBTask;
import com.unrealdinnerbone.unreallib.LogHelper;
import org.slf4j.Logger;

public class MigrateV3 implements IDBTask {

    private static final MigrateV4 MIGRATE_V4 = new MigrateV4();

    private static final Logger LOGGER = LogHelper.getLogger();

    @Override
    public void run(Tracker.Config config, CFHandler handler, CurseAPI curseAPI) throws Exception {
        LOGGER.info("Adding loader_versions column to file");
        handler.tryUpdate("""
                alter table curseforge.file
                    add loader_versions text[];
                """);
        LOGGER.info("Adding loader_versions column to projects");

        LOGGER.info("Adding java_versions column to file");
        handler.tryUpdate("""
                alter table curseforge.file
                    add java_versions text[];
                """);
        LOGGER.info("Adding java_versions column to projects");
        MIGRATE_V4.run(config, handler, curseAPI);
    }
}
