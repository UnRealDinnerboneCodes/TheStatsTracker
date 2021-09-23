package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.file.PathHelper;
import com.unrealdinnerbone.unreallib.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MarketplaceTracker implements IStatsTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketplaceTracker.class);

    @Override
    public void run(PostgressHandler handler) {
        try {
            Path download = Path.of("download.json");
            if(Files.exists(download)) {
                Files.delete(download);
            }
            PathHelper.downloadFile("https://mcmarketstats.miste.fr/MCPE/MarketplaceData.php", download);
            long time = System.currentTimeMillis();
            Map<?, ?> map = JsonUtil.parsePath(download, JsonUtil.DEFAULT, Map.class);
            Map<String, Object> entryMap = ((Map<String, Object>) map.get("Marketplace"));
            for (Map.Entry<String, Object> stringObjectEntry : entryMap.entrySet()) {
                if (!stringObjectEntry.getKey().equals("totalCount")) {
                    Map<String, Object> monthMap = ((Map<String, Object>) stringObjectEntry.getValue());
                    for (Object value : getResults(monthMap.get("results"))) {
                        Map<String, Object> objectEntry = (Map<String, Object>) value;
                        if (objectEntry.containsKey("displayProperties")) {
                            Map<String, Object> displayProperties = (Map<String, Object>) objectEntry.get("displayProperties");
                            String ownerName = String.valueOf(displayProperties.get("creatorName"));
                            String title = ((Map<String, String>) objectEntry.get("title")).get("neutral");
                            UUID id = UUID.fromString(String.valueOf(objectEntry.get("id")));
                            LOGGER.info("Found {} Project {}", ownerName, title);
                            if (objectEntry.containsKey("rating")) {
                                Map<String, Double> rates = (Map<String, Double>) objectEntry.get("rating");
                                int test = 0;
                                handleData(handler, title, id, rates.getOrDefault("star1Count", 0.0) + test,
                                        rates.getOrDefault("star2Count", 0.0) + test,
                                        rates.getOrDefault("star3Count", 0.0) + test,
                                        rates.getOrDefault("star4Count", 0.0) + test,
                                        rates.getOrDefault("star5Count", 0.0) + test,
                                        rates.getOrDefault("averageRating", 0.0) + test,
                                        rates.getOrDefault("totalRatingsCount", 0.0) + test, time, ownerName);
                            } else {
                                handleData(handler, title, id, 0, 0, 0, 0, 0, 0, 0, time, ownerName);
                            }
                        }
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleData(PostgressHandler handler, String title, UUID id, double star1, double star2, double star3, double star4, double star5, double average, double total, long time, String owner) {
        handler.executeUpdate("INSERT INTO public.marketplace (name, id, one, two, three, four, five, average_rating, total_ratings_count, time, owner) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setString(1, title);
            preparedStatement.setString(2, id.toString());
            preparedStatement.setInt(3, (int) star1);
            preparedStatement.setInt(4, (int) star2);
            preparedStatement.setInt(5, (int) star3);
            preparedStatement.setInt(6, (int) star4);
            preparedStatement.setInt(7, (int) star5);
            preparedStatement.setFloat(8, (float) average);
            preparedStatement.setInt(9, (int) total);
            preparedStatement.setLong(10, time);
            preparedStatement.setString(11, owner);
        });
    }

    public static List<Object> getResults(Object o) {
        if(o instanceof List) {
            return ((List<Object>) o);
        }else if (o instanceof Map){
            return ((Map<String, Object>) o).values().stream().toList();
        }else {
            return new ArrayList<>();
        }
    }
}
