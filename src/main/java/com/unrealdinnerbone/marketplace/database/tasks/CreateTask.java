package com.unrealdinnerbone.marketplace.database.tasks;

import com.unrealdinnerbone.curseapi.api.CurseAPI;
import com.unrealdinnerbone.marketplace.CFHandler;
import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.marketplace.database.IDBTask;
import com.unrealdinnerbone.postgresslib.PostgresHandler;
import com.unrealdinnerbone.unreallib.LogHelper;
import org.slf4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CreateTask implements IDBTask {

    private static final Logger LOGGER = LogHelper.getLogger();
    @Override
    public void run(Tracker.Config config, CFHandler handler, CurseAPI curseAPI) throws SQLException {
        ResultSet set = handler.getSet("select exists (SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'curseforge');");
        if(set.next()) {
            if(set.getBoolean("exists")) {
                LOGGER.info("Database already exists");
            }else {
                LOGGER.info("Creating Database");
                String resourceAsString = IDBTask.getResourceAsString("scripts/create.sql");
                handler.executeUpdate(resourceAsString, preparedStatement -> {});
            }
        }else {
            LOGGER.error("Failed to check if database exists");
        }
    }


}
