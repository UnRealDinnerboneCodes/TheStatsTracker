package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.RevenueEstimationData;
import com.unrealdinnerbone.curseauthorsapi.api.base.QueryResult;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.apiutils.IReturnResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class EstimatedRevenueTracker implements ICurseTracker<QueryResult<RevenueEstimationData>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EstimatedRevenueTracker.class);


    @Override
    public void run(PostgressHandler handler, QueryResult<RevenueEstimationData> estimatedRevenue) {
        RevenueEstimationData revenueEstimationData = estimatedRevenue.data();
        LOGGER.info("Estimated Revenue: Y: {} M: {}", revenueEstimationData.estimatedYearlyRevenue(), revenueEstimationData.estimatedLastMonthRevenue());

        long time = estimatedRevenue.getRetrievedAt().getEpochSecond();

        handler.executeUpdate("INSERT INTO curseforge.estimated_revenue (date, amount, type, hash) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING;", statement -> {
            long amount = revenueEstimationData.estimatedLastMonthRevenue();
            statement.setLong(1, time);
            statement.setLong(2, amount);
            statement.setString(3, "month");
            statement.setLong(4, Objects.hash(time, amount, "month"));
        });

        handler.executeUpdate("INSERT INTO curseforge.estimated_revenue (date, amount, type, hash) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING;", statement -> {
            long amount = revenueEstimationData.estimatedYearlyRevenue();
            statement.setLong(1, time);
            statement.setLong(2, amount);
            statement.setString(3, "year");
            statement.setLong(4, Objects.hash(time, amount, "year"));
        });
    }

    @Override
    public IReturnResult<QueryResult<RevenueEstimationData>> get() {
        return CurseAuthorsAPI.getEstimatedRevenue();
    }


}
