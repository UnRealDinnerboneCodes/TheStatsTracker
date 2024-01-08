package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.ProjectDownloadData;
import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.postgresslib.PostgresConsumer;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.apiutils.result.IResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class ProjectDownloadsTracker implements ICurseTracker<List<ProjectDownloadData>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectDownloadsTracker.class);


    @Override
    public void run(Tracker.Config config, PostgressHandler handler, List<ProjectDownloadData> projectDownloadData) {
        Map<String, String> projectToSlugMap = new HashMap<>();
        try {
            ResultSet set = handler.getSet("SELECT slug, name from curseforge.project");
            while (set.next()) {
                String slug = set.getString("slug");
                String name = set.getString("name");
                projectToSlugMap.put(name, slug);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        for (ProjectDownloadData downloadData : projectDownloadData) {
            List<PostgresConsumer> postgresConsumers = new ArrayList<>();
            LOGGER.info("Downloads for {}", downloadData.getDownloadDate());
            for (Map.Entry<String, Integer> stringLongEntry : downloadData.modDownloads().entrySet()) {
                if(projectToSlugMap.containsKey(stringLongEntry.getKey())) {
                    String slug = projectToSlugMap.get(stringLongEntry.getKey());
                    postgresConsumers.add(statement -> {
                        statement.setString(1, slug);
                        statement.setLong(2, stringLongEntry.getValue());
                        statement.setTimestamp(3, Timestamp.from(downloadData.getDownloadDate()));
                    });
                }else {
                    LOGGER.warn("Could not find slug for {}", stringLongEntry.getKey());
                }
            }
            handler.executeBatchUpdate("INSERT INTO curseforge.project_downloads (project, downloads, time) VALUES (?, ?, ?) ON CONFLICT (project, time) do update set downloads = EXCLUDED.downloads;", postgresConsumers);
        }
    }

    @Override
    public IResult<List<ProjectDownloadData>> get() {
        return CurseAuthorsAPI.getDownloads();
    }


}
