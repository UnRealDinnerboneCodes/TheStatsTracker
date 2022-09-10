package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.marketplaceapi.MarketplaceAPI;
import com.unrealdinnerbone.marketplaceapi.api.Product;
import com.unrealdinnerbone.postgresslib.PostgresConsumer;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MarketplaceTracker implements IStatsTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketplaceTracker.class);

    @Override
    public void run(PostgressHandler handler) {
        try {
            List<PostgresConsumer> statements = new ArrayList<>();
            long time = System.currentTimeMillis();
            for (Product product : MarketplaceAPI.getProduct("ftb", 0, 100).getExceptionally()) {
                double rating = product.averageRating() == null ? 0 : product.averageRating();
                int ratingCount = product.totalRatingsCount() == null ? 0 : product.totalRatingsCount();
                LOGGER.info("Product: {} for {} Rating: {} Rating Count: {}", product.title().neutral(), product.displayProperties().creatorName(), rating, ratingCount);
                statements.add(preparedStatement -> {
                    preparedStatement.setString(1, product.title().neutral());
                    preparedStatement.setString(2, product.id());
                    preparedStatement.setDouble(3, rating);
                    preparedStatement.setInt(4, ratingCount);
                    preparedStatement.setLong(5, time);
                    preparedStatement.setString(6, product.displayProperties().creatorName());
                });
            }
            handler.executeBatchUpdate("INSERT INTO public.marketplace (name, id, rating, ratings, time, team) VALUES (?, ?, ?, ?, ?, ?)", statements);

        }catch (Exception e) {
            LOGGER.error("Error while getting FTB products", e);
        }
    }
}
