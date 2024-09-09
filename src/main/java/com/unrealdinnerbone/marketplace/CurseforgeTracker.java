package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.curseapi.api.CurseAPI;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.marketplace.curseforge.trackers.*;
import com.unrealdinnerbone.unreallib.LogHelper;
import com.unrealdinnerbone.unreallib.exception.WebResultException;
import com.unrealdinnerbone.unreallib.json.exception.JsonParseException;
import org.slf4j.Logger;

import java.util.List;

public record CurseforgeTracker(Tracker.Config config, CurseAPI curseAPI) implements IStatsTracker {

    private static final Logger LOGGER = LogHelper.getLogger();

    private static final List<ICurseTracker<?>> trackers = List.of(
//            new EstimatedRevenueTracker(),
//            new LastMonthDownloadTracker(),
//            new LastMonthRevenueTracker(),
            new ProjectSlugTracker(),
            new DailyPointTracker(),
            new ProjectDownloadsTracker(),
//            new ProjectRevenueTracker(),
//            new TotalDownloadsTracker(),
            new FileDownloadTracker(),
            new UserPointTracker()
            );

    @Override
    public void run(CFHandler handler, CurseAPI curseAPI) {
        for (ICurseTracker<?> tracker : trackers) {
            try {
                run(handler, tracker);
            } catch (Exception e) {
                LOGGER.error("Failed to run tracker: {}", tracker.getClass().getSimpleName(), e);
            }
        }

    }

    public <T> void run(CFHandler postgresHandler, ICurseTracker<T> tracker) throws JsonParseException, WebResultException {
        tracker.run(config, postgresHandler, curseAPI, tracker.get().getNow());
    }

    public static boolean isStringLoader(String loader) {
        return loader.equalsIgnoreCase("forge") || loader.equalsIgnoreCase("fabric") || loader.equalsIgnoreCase("NeoForge");
    }

    public static boolean isStringJava(String loader) {
        return loader.toLowerCase().contains("java");
    }
}
