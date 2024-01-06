package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.LastMonthRevenueData;
import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.apiutils.result.IResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

public class LastMonthRevenueTracker implements ICurseTracker<List<LastMonthRevenueData>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LastMonthRevenueTracker.class);


    @Override
    public void run(Tracker.Config config, PostgressHandler handler, List<LastMonthRevenueData> projectDownloadData) {
        for(LastMonthRevenueData revenueData : projectDownloadData) {
            LOGGER.info("Revenue for {}: {}", revenueData.getRevenueDate(), revenueData.revenueDate());
            handler.executeUpdate("INSERT INTO curseforge.monthly_revenue (date, amount) VALUES (?, ?) ON CONFLICT DO NOTHING;", statement -> {
                statement.setLong(1, Instant.ofEpochMilli(revenueData.revenueDate()).getEpochSecond());
                statement.setLong(2, revenueData.revenue());
            });
        }
    }

    @Override
    public IResult<List<LastMonthRevenueData>> get() {
        return CurseAuthorsAPI.getLastMonthRevenue();
    }


}
