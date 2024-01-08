package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.marketplace.curseforge.trackers.*;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.LogHelper;
import com.unrealdinnerbone.unreallib.exception.WebResultException;
import com.unrealdinnerbone.unreallib.json.exception.JsonParseException;
import org.slf4j.Logger;

import java.util.List;

public record CurseforgeTracker(Tracker.Config config) implements IStatsTracker {

    private static final Logger LOGGER = LogHelper.getLogger();

    private static final List<ICurseTracker<?>> trackers = List.of(
//            new EstimatedRevenueTracker(),
//            new LastMonthDownloadTracker(),
//            new LastMonthRevenueTracker(),
            new DailyPointTracker(),
            new ProjectDownloadsTracker(),
//            new ProjectRevenueTracker(),
//            new TotalDownloadsTracker(),
            new UserPointTracker()
            );

    @Override
    public void run(PostgressHandler handler) {
        for (ICurseTracker<?> tracker : trackers) {
            try {
                run(handler, tracker);
            } catch (Exception e) {
                LOGGER.error("Failed to run tracker: {}", tracker.getClass().getSimpleName(), e);
            }
        }

    }

    public <T> void run(PostgressHandler postgressHandler, ICurseTracker<T> tracker) throws JsonParseException, WebResultException {
        tracker.run(config, postgressHandler, tracker.get().getNow());
    }
}
