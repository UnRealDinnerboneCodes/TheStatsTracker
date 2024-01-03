package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.ProjectDownloadData;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.postgresslib.PostgresConsumer;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.apiutils.result.IResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProjectDownloadsTracker implements ICurseTracker<List<ProjectDownloadData>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectDownloadsTracker.class);


    @Override
    public void run(PostgressHandler handler, List<ProjectDownloadData> projectDownloadData) {
        for(ProjectDownloadData downloadData : projectDownloadData) {
            List<PostgresConsumer> postgresConsumers = new ArrayList<>();
            LOGGER.info("Downloads for {}", downloadData.getDownloadDate());
            for(Map.Entry<String, Integer> stringLongEntry : downloadData.modDownloads().entrySet()) {
                String name = stringLongEntry.getKey();
                long downloads = stringLongEntry.getValue();
                long time = downloadData.getDownloadDate().getEpochSecond();
                postgresConsumers.add(statement -> {
                    statement.setString(1, name);
                    statement.setLong(2, downloads);
                    statement.setLong(3, time);
                    statement.setLong(4, Objects.hash(name, downloads, time));
                });
            }
            handler.executeBatchUpdate("INSERT INTO curseforge.project_downloads (project, downloads, date, hash) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING;", postgresConsumers);
        }
    }

    @Override
    public IResult<List<ProjectDownloadData>> get() {
        return CurseAuthorsAPI.getDownloads();
    }


}
