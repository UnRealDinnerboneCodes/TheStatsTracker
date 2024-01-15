package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseapi.api.CurseAPI;
import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.ProjectDownloadData;
import com.unrealdinnerbone.marketplace.CFHandler;
import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.postgresslib.PostgresConsumer;
import com.unrealdinnerbone.unreallib.apiutils.result.IResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

public class ProjectDownloadsTracker implements ICurseTracker<List<ProjectDownloadData>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectDownloadsTracker.class);


    @Override
    public void run(Tracker.Config config, CFHandler handler, CurseAPI curseAPI, List<ProjectDownloadData> projectDownloadData) {
        Map<String, Integer> projectToSlugMap = handler.slugToIDMap(config);
        Map<String, String> projectToSlugMap1 = getProjectToSlugMap(handler);
        for (ProjectDownloadData downloadData : projectDownloadData) {
            List<PostgresConsumer> postgresConsumers = new ArrayList<>();
            LOGGER.info("Downloads for {}", downloadData.getDownloadDate());
            for (Map.Entry<String, Integer> stringLongEntry : downloadData.modDownloads().entrySet()) {
                String key = stringLongEntry.getKey();
                if(projectToSlugMap1.containsKey(key)) {
                    Integer slug = projectToSlugMap.get(projectToSlugMap1.get(key));
                    if(slug == null) {
                        LOGGER.warn("Could not find slug for {}", key);
                        break;
                    }else {
                        postgresConsumers.add(statement -> {
                            statement.setInt(1, slug);
                            statement.setLong(2, stringLongEntry.getValue());
                            statement.setTimestamp(3, Timestamp.from(downloadData.getDownloadDate()));
                        });
                    }
                }else {
                    LOGGER.warn("Could not find slug for {}", key);
                }
            }
            handler.executeBatchUpdate("INSERT INTO curseforge.project_downloads (id, downloads, time) VALUES (?, ?, ?) ON CONFLICT (id, time) do update set downloads = EXCLUDED.downloads;", postgresConsumers);
        }
    }

    @Override
    public IResult<List<ProjectDownloadData>> get() {
        return CurseAuthorsAPI.getDownloads();
    }


}
