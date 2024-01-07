package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.UserPointData;
import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.LogHelper;
import com.unrealdinnerbone.unreallib.apiutils.result.IResult;
import org.slf4j.Logger;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

public class UserPointTracker implements ICurseTracker<UserPointData> {

    private static final Logger LOGGER = LogHelper.getLogger();


    @Override
    public void run(Tracker.Config config, PostgressHandler handler, UserPointData userPointData) {
        LOGGER.info("Current User Points: {}", userPointData.userPoints());
        handler.executeUpdate("INSERT INTO curseforge.user_points (points, time) VALUES (?, ?) ON CONFLICT DO NOTHING;", statement -> {
            statement.setLong(1, userPointData.userPoints());
            statement.setTimestamp(2, Timestamp.from(Instant.now()));
        });
    }

    @Override
    public IResult<UserPointData> get() {
        return CurseAuthorsAPI.getUserPoints();
    }

}
