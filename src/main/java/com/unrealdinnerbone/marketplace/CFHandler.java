package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.Project;
import com.unrealdinnerbone.postgresslib.PostgresConfig;
import com.unrealdinnerbone.postgresslib.PostgresHandler;

import java.sql.Array;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CFHandler extends PostgresHandler {

    public CFHandler(PostgresConfig postgresConfig) throws SQLException {
        super(postgresConfig);
    }

    public void addProject(String name, String slug, int id) {
        executeUpdate("INSERT INTO curseforge.projects (name, slug, id) VALUES (?, ?, ?) ON CONFLICT DO NOTHING;", preparedStatement -> {
            preparedStatement.setString(1, name.toLowerCase());
            preparedStatement.setString(2, slug.toLowerCase());
            preparedStatement.setInt(3, id);
        });
    }

    public Map<String, Integer> slugToIDMap(Tracker.Config config) {
        Map<String, Integer> map = new HashMap<>();
        for (Project project : CurseAuthorsAPI.getProjects().getNow()) {
            map.put(project.slug(), project.id());
        }

        map.putAll(config.getSlugMap().get());
        return map;
    }

    public void tryUpdate(String quarry) {
        if(!executeUpdate(quarry, preparedStatement -> {})) {
            throw new IllegalStateException("Failed to update database");
        }
    }
}
