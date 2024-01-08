package com.unrealdinnerbone.marketplace.curseforge.api;

import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.apiutils.result.IResult;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public interface ICurseTracker<T> {

    void run(Tracker.Config config, PostgressHandler handler, T t);

    IResult<T> get();


    default Map<String, String> getProjectToSlugMap(PostgressHandler handler) {
        Map<String, String> projectToSlugMap = new HashMap<>();
        try {
            ResultSet set = handler.getSet("SELECT slug, name from curseforge.projects");
            while (set.next()) {
                String slug = set.getString("slug");
                String name = set.getString("name").toLowerCase();
                projectToSlugMap.put(name, slug);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return projectToSlugMap;
    }
}
