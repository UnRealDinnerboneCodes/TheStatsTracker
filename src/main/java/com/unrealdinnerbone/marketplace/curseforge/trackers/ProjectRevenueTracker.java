package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.ProjectRevenueData;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.postgresslib.PostgresConsumer;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.apiutils.result.IResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProjectRevenueTracker implements ICurseTracker<List<ProjectRevenueData>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectRevenueTracker.class);


    @Override
    public void run(PostgressHandler handler, List<ProjectRevenueData> projectRevenueData) {
        for(ProjectRevenueData revenueData : projectRevenueData) {
            List<PostgresConsumer> postgresConsumers = new ArrayList<>();
            Instant month = revenueData.getRevenueMonth();
            LOGGER.info("Revenue Data for {}", month);
            for(Map.Entry<String, Integer> stringLongEntry : revenueData.modRevenue().entrySet()) {
                String name = stringLongEntry.getKey();
                long downloads = stringLongEntry.getValue();
                long time = month.getEpochSecond();
                postgresConsumers.add(statement -> {
                    statement.setString(1, name);
                    statement.setLong(2, downloads);
                    statement.setLong(3, time);
                    statement.setLong(4, Objects.hash(name, downloads, time));
                });
            }
            handler.executeBatchUpdate("INSERT INTO curseforge.project_revenue (project, revenue, date, hash) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING;", postgresConsumers);
        }
    }

    @Override
    public IResult<List<ProjectRevenueData>> get() {
        return CurseAuthorsAPI.getRevenue();
    }


}
