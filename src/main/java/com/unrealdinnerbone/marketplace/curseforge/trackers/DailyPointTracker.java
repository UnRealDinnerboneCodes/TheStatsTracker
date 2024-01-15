package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseapi.api.CurseAPI;
import com.unrealdinnerbone.curseapi.api.mod.Mod;
import com.unrealdinnerbone.curseapi.quaries.ModQuery;
import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.OrderData;
import com.unrealdinnerbone.curseauthorsapi.api.ProjectBreakdownData;
import com.unrealdinnerbone.curseauthorsapi.api.ProjectsBreakdownData;
import com.unrealdinnerbone.curseauthorsapi.api.TransactionData;
import com.unrealdinnerbone.marketplace.CFHandler;
import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.postgresslib.PostgresConsumer;
import com.unrealdinnerbone.postgresslib.PostgresHandler;
import com.unrealdinnerbone.unreallib.LogHelper;
import com.unrealdinnerbone.unreallib.apiutils.result.IResult;
import com.unrealdinnerbone.unreallib.discord.DiscordWebhook;
import com.unrealdinnerbone.unreallib.discord.EmbedObject;
import com.unrealdinnerbone.unreallib.exception.WebResultException;
import com.unrealdinnerbone.unreallib.json.exception.JsonParseException;
import org.slf4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DailyPointTracker implements ICurseTracker<List<TransactionData>> {

    private static final Logger LOGGER = LogHelper.getLogger();

    private static final String DISCORD_WEBHOOK = System.getenv("DISCORD_WEBHOOK");

    private static final DecimalFormat CURR_FORMAT = new DecimalFormat("#,###.00");

    @Override
    public void run(Tracker.Config config, CFHandler handler, CurseAPI curseAPI, List<TransactionData> transactionData) {
        List<PostgresConsumer> tConsumers = new ArrayList<>();
        List<PostgresConsumer> orders = new ArrayList<>();
        List<PostgresConsumer> projectPoints = new ArrayList<>();

        Map<String, Integer> slugToID = handler.slugToIDMap(config);

        try {
            List<Long> doneTransactions = getRanTransactions(handler);
            topLoop:
            for (TransactionData transactionDatum : transactionData) {
                Timestamp timestamp = Timestamp.from(transactionDatum.getDateCreated());
                if(!doneTransactions.contains(transactionDatum.id())) {
                    LOGGER.info("Transaction for {}: {} {}", transactionDatum.getDateCreated(), transactionDatum.id(), transactionDatum.pointChange());

                    if(transactionDatum.type() == TransactionData.Type.REWARD) {
                        try {
                            if(config.getTrackThings().get()) {
                                ProjectsBreakdownData projectsBreakdownData = CurseAuthorsAPI.getBreakdown(transactionDatum.id()).getNow();
                                EmbedObject.EmbedObjectBuilder builder = EmbedObject.builder();
                                double totalPoints = 0;
                                List<ProjectBreakdownData> projectBreakdownData = new ArrayList<>(projectsBreakdownData.projectsBreakdown());
                                projectBreakdownData.sort((o1, o2) -> Double.compare(o2.points(), o1.points()));
                                for (ProjectBreakdownData projectBreakdown : projectBreakdownData) {
                                    builder = builder.field(projectBreakdown.projectName(), projectBreakdown.points() + getCurrencyFormat(projectBreakdown.points()), false);
                                    totalPoints += projectBreakdown.points();
                                }
                                if(config.getDiscordEnabled().get()) {
                                    DiscordWebhook.builder()
                                            .addEmbed(builder.title("Total Points " + totalPoints + getCurrencyFormat(totalPoints)).build())
                                            .setUsername("Curse Points Bot")
                                            .post(DISCORD_WEBHOOK);
                                }
                                for (ProjectBreakdownData projectBreakdown : projectsBreakdownData.projectsBreakdown()) {

                                    String projectSlug = projectBreakdown.getSlug().toLowerCase();
                                    if(!slugToID.containsKey(projectSlug)) {
                                        LOGGER.info("Could not find id for {}", projectBreakdown.projectName());
                                        List<Mod> data = curseAPI.v1().searchMods(ModQuery.builder().gameId(432).slug(projectSlug)).getNow().data();
                                        if(data.size() != 1) {
                                            LOGGER.error("Could not find mod for slug {} found {} mods", projectSlug, data.size());
                                            break topLoop;
                                        }
                                        Mod mod = data.get(0);
                                        handler.addProject(projectBreakdown.projectName(), projectBreakdown.getSlug(), mod.id());
                                        LOGGER.info("Added Project {} with id {}", projectBreakdown.projectName(), mod.id());
                                    }

                                    projectPoints.add(st -> {
                                        st.setInt(1, slugToID.get(projectSlug));
                                        st.setTimestamp(2, timestamp);
                                        st.setDouble(3, projectBreakdown.points());
                                    });
                                }
                            }
                        } catch (JsonParseException | WebResultException | IllegalStateException e) {
                            LOGGER.error("Error while getting project breakdown", e);
                        }
                    }
                    tConsumers.add(st -> {
                        st.setLong(1, transactionDatum.id());
                        st.setDouble(2, transactionDatum.pointChange());
                        st.setInt(3, transactionDatum.type().getId());
                        st.setTimestamp(4, timestamp);
                    });
                    if(transactionDatum.order() != null) {
                        OrderData order = transactionDatum.order();
                        orders.add(preparedStatement -> {
                            preparedStatement.setLong(1, order.id());
                            preparedStatement.setInt(2, order.quantity());
                            preparedStatement.setString(3, order.item());
                            preparedStatement.setInt(4, transactionDatum.type().getId());
                            preparedStatement.setTimestamp(5, timestamp);
                        });
                    }
                }

            }
            LOGGER.info("Inserting {} Transactions", tConsumers.size());
            handler.executeBatchUpdate("INSERT INTO curseforge.transaction (id, point_change, type, time) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING;", tConsumers);
            LOGGER.info("Inserted {} Transactions", tConsumers.size());

            LOGGER.info("Inserting {} Orders", orders.size());
            handler.executeBatchUpdate("INSERT INTO curseforge.order (id, quantity, item, type, time) VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;", orders);
            LOGGER.info("Inserted {} Orders", orders.size());

            LOGGER.info("Inserting {} Project Points", projectPoints.size());
            handler.executeBatchUpdate("INSERT INTO curseforge.project_points (id, time, points) VALUES (?, ?, ?) ON CONFLICT DO NOTHING;", projectPoints);
            LOGGER.info("Inserted {} Project Points", projectPoints.size());


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }

    public List<Long> getRanTransactions(PostgresHandler handler) throws SQLException {
        ResultSet set = handler.getSet("SELECT id from curseforge.transaction");
        List<Long> ids = new ArrayList<>();
        while (set.next()) {
            long id = set.getLong("id");
            ids.add(id);
        }
        return ids;
    }


    //https://cdn.jsdelivr.net/gh/fawazahmed0/currency-api@1/latest/currencies/usd/eur.json
    public static String getCurrencyFormat(double points) {
        return " ($" + CURR_FORMAT.format(points * 0.05) + ")";
    }

    @Override
    public IResult<List<TransactionData>> get() {
        return CurseAuthorsAPI.getTransactions();
    }

}
