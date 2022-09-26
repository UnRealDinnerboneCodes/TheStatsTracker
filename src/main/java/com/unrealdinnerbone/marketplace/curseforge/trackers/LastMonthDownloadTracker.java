package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.LastMonthDownloadsData;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.apiutils.IReturnResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

public class LastMonthDownloadTracker implements ICurseTracker<List<LastMonthDownloadsData>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LastMonthDownloadTracker.class);


    @Override
    public void run(PostgressHandler handler, List<LastMonthDownloadsData> lastMonthDownloadsData) {
        for(LastMonthDownloadsData downloadsData : lastMonthDownloadsData) {
            LOGGER.info("Downloads for {}: {}", downloadsData.getDownloadDate(), downloadsData.totalDownloads());
            handler.executeUpdate("INSERT INTO public.downloads (date, downloads) VALUES (?, ?) ON CONFLICT DO NOTHING;", statement -> {
                statement.setLong(1, Instant.ofEpochMilli(downloadsData.downloadDate()).getEpochSecond());
                statement.setLong(2, downloadsData.totalDownloads());
            });

        }
    }

    @Override
    public IReturnResult<List<LastMonthDownloadsData>> get() {
        return CurseAuthorsAPI.getLastMonthDownloads();
    }


}