package com.unrealdinnerbone.marketplace.database;

import com.unrealdinnerbone.curseapi.api.CurseAPI;
import com.unrealdinnerbone.marketplace.CFHandler;
import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.postgresslib.PostgresHandler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.stream.Collectors;

public interface IDBTask {

    void run(Tracker.Config config, CFHandler handler, CurseAPI curseAPI) throws Exception;

    static String getResourceAsString(String thePath) {
        InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(thePath);
        InputStreamReader in = new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8);
        return new BufferedReader(in).lines().collect(Collectors.joining("\n"));
    }
}
