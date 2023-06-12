package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.UserPointData;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.LogHelper;
import com.unrealdinnerbone.unreallib.apiutils.IResult;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.Objects;

public class TotalPointTracker implements ICurseTracker<UserPointData> {

    private static final Logger LOGGER = LogHelper.getLogger();


    @Override
    public void run(PostgressHandler handler, UserPointData userPointData) {
        LOGGER.info("Current User Points: {}", userPointData.userPoints());
        handler.executeUpdate("INSERT INTO curseforge.user_points (points, date, hash) VALUES (?, ?, ?) ON CONFLICT DO NOTHING;", statement -> {
            long now = Instant.now().getEpochSecond();
            statement.setLong(1, userPointData.userPoints());
            statement.setLong(2, now);
            statement.setLong(3, Objects.hash(userPointData.userPoints(), now));
        });
    }

    @Override
    public IResult<UserPointData> get() {
        return CurseAuthorsAPI.getUserPoints();
    }

}
