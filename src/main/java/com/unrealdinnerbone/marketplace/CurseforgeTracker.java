package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.marketplace.temp.ReturnResult;
import com.unrealdinnerbone.marketplace.temp.Thing;
import com.unrealdinnerbone.postgresslib.PostgresConsumer;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.html.CSS;
import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;

public class CurseforgeTracker implements IStatsTracker{


    private static final Logger LOGGER = LoggerFactory.getLogger(CSS.class);

    private final HttpClient httpClient;

    public CurseforgeTracker(TrackerConfig trackerConfig) {
        CookieManager cookieManager = new CookieManager();
        HttpCookie.parse(trackerConfig.getCurseforgeToken()).forEach(httpCookie -> cookieManager.getCookieStore().add(URI.create(httpCookie.getDomain()), httpCookie));
        httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .build();
    }
    @Override
    public void run(PostgressHandler handler) {

        try {

            Pair<Instant, Long> downloadsTotal = getDownloadsTotal();
            LOGGER.info("Total Downloads at {}: {}", downloadsTotal.key(), downloadsTotal.value());
            for(Pair<Instant, Map<String, Long>> instantMapPair : getProjectDownloadsPerDay()) {
                List<PostgresConsumer> postgresConsumers = new ArrayList<>();
                LOGGER.info("Downloads for {}", instantMapPair.key());
                for(Map.Entry<String, Long> stringLongEntry : instantMapPair.value().entrySet()) {
                    String name = stringLongEntry.getKey();
                    long downloads = stringLongEntry.getValue();
                    long time = instantMapPair.key().getEpochSecond();
                    postgresConsumers.add(statement -> {
                        statement.setString(1, name);
                        statement.setLong(2, downloads);
                        statement.setLong(3, time);
                        statement.setLong(4, Objects.hash(name, downloads, time));
                    });
                }
                handler.executeBatchUpdate("INSERT INTO public.project_downloads (project, downloads, date, hash) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING;", postgresConsumers);
            }


            for(Pair<Instant, Map<String, Long>> instantMapPair : getProjectRevenuePerDay()) {
                List<PostgresConsumer> postgresConsumers = new ArrayList<>();
                LOGGER.info("Downloads for {}", instantMapPair.key());
                for(Map.Entry<String, Long> stringLongEntry : instantMapPair.value().entrySet()) {
                    String name = stringLongEntry.getKey();
                    long downloads = stringLongEntry.getValue();
                    long time = instantMapPair.key().getEpochSecond();
                    postgresConsumers.add(statement -> {
                        statement.setString(1, name);
                        statement.setLong(2, downloads);
                        statement.setLong(3, time);
                        statement.setLong(4, Objects.hash(name, downloads, time));
                    });
                }
                handler.executeBatchUpdate("INSERT INTO public.project_revenue (project, revenue, date, hash) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING;", postgresConsumers);
            }

            for(Pair<Instant, Long> instantLongPair : getRevenuePerMonth()) {
                LOGGER.info("Revenue for {}: {}", instantLongPair.key(), instantLongPair.value());
                handler.executeUpdate("INSERT INTO public.monthly_revenue (date, amount) VALUES (?, ?) ON CONFLICT DO NOTHING;", statement -> {
                    statement.setLong(1, instantLongPair.key().getEpochSecond());
                    statement.setLong(2, instantLongPair.value());
                });
            }
            for(Pair<Instant, Long> instantLongPair : getDownloadsPerMonth()) {
                LOGGER.info("Downloads for {}: {}", instantLongPair.key(), instantLongPair.value());
                handler.executeUpdate("INSERT INTO public.downloads (date, downloads) VALUES (?, ?) ON CONFLICT DO NOTHING;", statement -> {
                    statement.setLong(1, instantLongPair.key().getEpochSecond());
                    statement.setLong(2, instantLongPair.value());
                });

            }

            Pair<Instant, Pair<Long, Long>> estimatedRevenue = getEstimatedRevenue();
            LOGGER.info("Estimated Revenue: Y: {} M: {}", estimatedRevenue.value().value(), estimatedRevenue.value().key());

            long time = estimatedRevenue.key().getEpochSecond();

            handler.executeUpdate("INSERT INTO public.estimated_revenue (date, amount, type, hash) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING;", statement -> {
                long amount = estimatedRevenue.value().key();
                statement.setLong(1, time);
                statement.setLong(2, estimatedRevenue.value().key());
                statement.setString(3, "month");
                statement.setLong(4, Objects.hash(time, amount, "month"));
            });

            handler.executeUpdate("INSERT INTO public.estimated_revenue (date, amount, type, hash) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING;", statement -> {
                long amount = estimatedRevenue.value().key();
                statement.setLong(1, time);
                statement.setLong(2, estimatedRevenue.value().value());
                statement.setString(3, "year");
                statement.setLong(4, Objects.hash(time, amount, "year"));
            });
        }catch(Exception e) {
            LOGGER.error("Error", e);
        }

    }

    private Pair<Instant, Pair<Long, Long>> getEstimatedRevenue() throws IOException, InterruptedException {
        ReturnResult<Query> result = getQ("https://authors-next.curseforge.com/_api/statistics/queries/revenueEstimation");
        com.unrealdinnerbone.css.json.QueryResult query = result.getExceptionally().queryResult();
        return new Pair<>(Instant.parse(query.retrievedAt()), Pair.of(query.data().get(0).get("estimatedLastMonthRevenue"), query.data().get(0).get("estimatedYearlyRevenue")));
    }

    private List<Pair<Instant, Long>> getDownloadsPerMonth() throws IOException, InterruptedException {
        ReturnResult<Legend> result = getLQR("https://authors-next.curseforge.com/_api/statistics/queries/lastMonthDownloads");
        com.unrealdinnerbone.css.json.LegendQueryResult legendQueryResult = result.getExceptionally().queryResult();
        List<Pair<Instant, Long>> downloadsPerMonth = new ArrayList<>();
        for(Map<String, Long> datum : legendQueryResult.data()) {
            Instant date = Instant.ofEpochSecond(datum.get("downloadDate") / 1000);
            long amount = datum.get("totalDownloads");
            downloadsPerMonth.add(Pair.of(date, amount));
        }
        return downloadsPerMonth;
    }
    private List<Pair<Instant, Long>> getRevenuePerMonth() throws IOException, InterruptedException {
        ReturnResult<Legend> result = getLQR("https://authors-next.curseforge.com/_api/statistics/queries/lastMonthRevenue");
        com.unrealdinnerbone.css.json.LegendQueryResult legendQueryResult = result.getExceptionally().queryResult();
        List<Pair<Instant, Long>> downloadsPerMonth = new ArrayList<>();
        for(Map<String, Long> datum : legendQueryResult.data()) {
            Instant date = Instant.ofEpochSecond(datum.get("revenueDate") / 1000);
            long amount = datum.get("revenue");
            downloadsPerMonth.add(Pair.of(date, amount));
        }
        return downloadsPerMonth;
    }

    private List<Pair<Instant, Map<String, Long>>> getProjectDownloadsPerDay() throws IOException, InterruptedException {
        ReturnResult<Legend> result = getLQR("https://authors-next.curseforge.com/_api/statistics/queries/downloads");
        com.unrealdinnerbone.css.json.LegendQueryResult legendQueryResult = result.getExceptionally().queryResult();
        List<Pair<Instant, Map<String, Long>>> downloadsPerDay = new ArrayList<>();
        for(Map<String, Long> datum : legendQueryResult.data()) {
            Map<String, Long> downloads = new HashMap<>();
            Instant date = Instant.ofEpochSecond(datum.get("downloadDate") / 1000);
            for(Map.Entry<String, Long> stringLongEntry : datum.entrySet()) {
                if(stringLongEntry.getKey().equals("downloadDate") || stringLongEntry.getKey().equals("downloads")) {
                    continue;
                }
                downloads.put(stringLongEntry.getKey(), stringLongEntry.getValue());
            }
            downloadsPerDay.add(new Pair<>(date, downloads));
        }
        return downloadsPerDay;
    }

    private List<Pair<Instant, Map<String, Long>>> getProjectRevenuePerDay() throws IOException, InterruptedException {
        ReturnResult<Legend> result = getLQR("https://authors-next.curseforge.com/_api/statistics/queries/revenue");
        com.unrealdinnerbone.css.json.LegendQueryResult legendQueryResult = result.getExceptionally().queryResult();
        List<Pair<Instant, Map<String, Long>>> downloadsPerDay = new ArrayList<>();
        for(Map<String, Long> datum : legendQueryResult.data()) {
            Map<String, Long> downloads = new HashMap<>();
            Instant date = Instant.ofEpochSecond(datum.get("revenueMonth") / 1000);
            for(Map.Entry<String, Long> stringLongEntry : datum.entrySet()) {
                if(stringLongEntry.getKey().equals("revenueMonth") || stringLongEntry.getKey().equals("revenue")) {
                    continue;
                }
                downloads.put(stringLongEntry.getKey(), stringLongEntry.getValue());
            }
            downloadsPerDay.add(new Pair<>(date, downloads));
        }
        return downloadsPerDay;
    }

    private Pair<Instant, Long> getDownloadsTotal() throws IOException, InterruptedException {
        ReturnResult<Query> result = getQ("https://authors-next.curseforge.com/_api/statistics/queries/downloadsTotal");
        com.unrealdinnerbone.css.json.QueryResult query = result.getExceptionally().queryResult();
        return new Pair<>(Instant.parse(query.retrievedAt()), query.data().get(0).get("total"));
    }



    private ReturnResult<Legend> getLQR(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(url)).build();
        HttpResponse<String> s = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new ReturnResult<>(s.body(), Legend.class);
    }

    private ReturnResult<Query> getQ(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(url)).build();
        HttpResponse<String> s = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new ReturnResult<>(s.body(), Query.class);
    }

    public static class Legend extends Thing<com.unrealdinnerbone.css.json.LegendQueryResult> {
        public Legend(com.unrealdinnerbone.css.json.LegendQueryResult queryResult) {
            super(queryResult);
        }
    }

    public static class Query extends Thing<com.unrealdinnerbone.css.json.QueryResult> {
        public Query(com.unrealdinnerbone.css.json.QueryResult queryResult) {
            super(queryResult);
        }
    }
}
