package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.marketplace.curseforge.trackers.*;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.json.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CurseforgeTracker implements IStatsTracker {


    private static final List<ICurseTracker<?>> trackers = List.of(
            new EstimatedRevenueTracker(),
            new LastMonthDownloadTracker(),
            new LastMonthRevenueTracker(),
            new ProjectDownloadsTracker(),
            new ProjectRevenueTracker(),
            new TotalDownloadsTracker(),
            new TotalPointTracker(),
            new DailyPointTracker()
            );
    private static final Logger LOGGER = LoggerFactory.getLogger(CurseforgeTracker.class);

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


    public <T> void run(PostgressHandler postgressHandler, ICurseTracker<T> tracker) throws JsonParseException {
        tracker.run(postgressHandler, tracker.get().getExceptionally());
    }
}
