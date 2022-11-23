package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.marketplaceapi.MarketplaceAPI;
import com.unrealdinnerbone.marketplaceapi.api.PackIdentity;
import com.unrealdinnerbone.marketplaceapi.api.Product;
import com.unrealdinnerbone.postgresslib.PostgresConsumer;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.MathHelper;
import com.unrealdinnerbone.unreallib.web.HttpHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MarketplaceTracker implements IStatsTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketplaceTracker.class);

    @Override
    public void run(PostgressHandler handler) {
//        List<Team> teams = new ArrayList<>();
//        for (Category value : Category.values()) {
//            try {
//                Stopwatch stopwatch = Stopwatch.createStarted();
//                for (Product allProduct : MarketplaceAPI.getAllProducts(value, 0, MathHelper.randomInt(100, 110))) {
//                    Team team = new Team(getCreatorId(allProduct), allProduct.displayProperties().creatorName());
//                    if(!teams.contains(team)) {
//                        teams.add(team);
//                    }
//                }
//                LOGGER.info("Took {}ms to get all products for {}", stopwatch.elapsed(TimeUnit.MILLISECONDS), value.getName());
//
//
//
//            } catch (JsonParseException e) {
//                LOGGER.error("Failed to get all products for {}", value.getName(), e);
//                return;
//            }
//
//        }
//        List<PostgresConsumer> statementList = teams.stream()
//                .<PostgresConsumer>map(team -> preparedStatement -> {
//                    preparedStatement.setLong(1, team.id());
//                    preparedStatement.setString(2, team.name());
//                }).toList();
//        handler.executeBatchUpdate("insert into marketplace.creator (id, name) VALUES (?, ?) on conflict do nothing", statementList);
//        LOGGER.info("Found {} teams", teams.size());

        try {
            LOGGER.info("Hello There");
            ResultSet set = handler.getSet("select * from marketplace.creator");
            List<Team> teams = new ArrayList<>();
            while (set.next()) {
                long id = set.getLong("id");
                String name = set.getString("name");
                teams.add(new Team(id, name));
            }
            LOGGER.info("Found {} teams", teams.size());
            for (Team team : teams) {
                List<PostgresConsumer> statements = new ArrayList<>();
                long time = System.currentTimeMillis();
                List<PostgresConsumer> productsConsumers = new ArrayList<>();
                List<Product> products = MarketplaceAPI.getAllProducts(HttpHelper.encode(team.name()), 0, MathHelper.randomInt(90, 110));
                LOGGER.info("Took {}ms to get {} products for {}", System.currentTimeMillis() - time, products.size(), team);
                for (Product product : products) {
                    Instant startTime = Instant.parse(product.startDate());
                    String packType = product.displayProperties().packIdentity().stream().findFirst().map(PackIdentity::type).orElse("unknown");
                    long price = Long.parseLong(product.displayProperties().price());
                    productsConsumers.add(statement -> {
                        statement.setString(1, product.id());
                        statement.setString(2, product.title().neutral());
                        statement.setLong(3, getCreatorId(product));
                        statement.setLong(4, startTime.toEpochMilli());
                        statement.setString(5, packType);
                        statement.setLong(6, price);
                    });

                    double rating = product.averageRating() == null ? 0 : product.averageRating();
                    int ratingCount = product.totalRatingsCount() == null ? 0 : product.totalRatingsCount();
                    statements.add(preparedStatement -> {
                        preparedStatement.setString(1, product.id());
                        preparedStatement.setDouble(2, rating);
                        preparedStatement.setInt(3, ratingCount);
                        preparedStatement.setLong(4, time);
                        preparedStatement.setString(5, product.id());
                        preparedStatement.setInt(6, ratingCount);
                        preparedStatement.setDouble(7, rating);
                    });
                }
                handler.executeBatchUpdate("INSERT INTO marketplace.product (id, name, creator, release_time, type, price) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING", productsConsumers);
                handler.executeBatchUpdate("INSERT INTO marketplace.rating (id, rating, ratings, time) SELECT ?, ?, ?, ? WHERE not EXISTS ( SELECT * FROM marketplace.rating WHERE id = ? and (ratings = ? and rating = ?) order by time desc limit 1)", statements);
            }
//
            LOGGER.info("Done");
        } catch (Exception e) {
            LOGGER.error("Error while getting FTB products", e);
        }
    }

    public static long getCreatorId(Product product) {
        return product.creatorId() == null ? product.displayProperties().creatorName().equalsIgnoreCase("minecraft") ? 2535448579972708L : -1L : product.creatorId();
  }


    public record Team(long id, String name) {}


}
