package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.ProjectBreakdownData;
import com.unrealdinnerbone.curseauthorsapi.api.ProjectsBreakdownData;
import com.unrealdinnerbone.curseauthorsapi.api.TransactionData;
import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.postgresslib.PostgresConsumer;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.LogHelper;
import com.unrealdinnerbone.unreallib.apiutils.result.IResult;
import com.unrealdinnerbone.unreallib.discord.DiscordWebhook;
import com.unrealdinnerbone.unreallib.discord.EmbedObject;
import com.unrealdinnerbone.unreallib.exception.WebResultException;
import com.unrealdinnerbone.unreallib.json.exception.JsonParseException;
import org.slf4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DailyPointTracker implements ICurseTracker<List<TransactionData>> {

    private static final Logger LOGGER = LogHelper.getLogger();

    private static final String DISCORD_WEBHOOK = System.getenv("DISCORD_WEBHOOK");

    private static final DecimalFormat CURR_FORMAT = new DecimalFormat("$#,###.00");

    @Override
    public void run(Tracker.Config config, PostgressHandler handler, List<TransactionData> transactionData) {
        List<PostgresConsumer> tConsumers = new ArrayList<>();
        try {
            ResultSet set = handler.getSet("SELECT id from curseforge.transaction");
            List<Long> ids = new ArrayList<>();
            while (set.next()) {
                long id = set.getLong("id");
                ids.add(id);
            }
            for (TransactionData transactionDatum : transactionData) {
                long date = transactionDatum.getDateCreated().getEpochSecond();
                LOGGER.info("Transaction for {}: {} {}", transactionDatum.getDateCreated(), transactionDatum.id(), transactionDatum.pointChange());
                if(!ids.contains(transactionDatum.id())) {
                    if(transactionDatum.type() == TransactionData.Type.REWARD) {
                        try {
                            ProjectsBreakdownData projectsBreakdownData = CurseAuthorsAPI.getBreakdown(transactionDatum.id()).getNow();
                            EmbedObject.EmbedObjectBuilder builder = EmbedObject.builder();
                            double totalPoints = 0;
                            List<ProjectBreakdownData> projectBreakdownData = new ArrayList<>(projectsBreakdownData.projectsBreakdown());
                            projectBreakdownData.sort((o1, o2) -> Double.compare(o2.points(), o1.points()));
                            for (ProjectBreakdownData projectBreakdown : projectBreakdownData) {
                                builder = builder.field(projectBreakdown.projectName(), projectBreakdown.points() + " (" + CURR_FORMAT.format(projectBreakdown.points() * 0.05) + ")", false);
                                totalPoints += projectBreakdown.points();
                            }
                            if(config.getDiscordEnabled().get()) {
                                DiscordWebhook.builder()
                                        .addEmbed(builder.title("Total Points " + totalPoints + " (" + CURR_FORMAT.format(totalPoints * 0.05) + ")").build())
                                        .setUsername("Curse Points Bot")
                                        .post(DISCORD_WEBHOOK);
                            }
                            if(config.getTrackThings().get()) {
                                List<PostgresConsumer> consumers = new ArrayList<>();
                                for (ProjectBreakdownData projectBreakdown : projectsBreakdownData.projectsBreakdown()) {
                                    consumers.add(st -> {
                                        String slug = projectBreakdown.getSlug();
                                        st.setString(1, slug);
                                        st.setLong(2, date);
                                        st.setDouble(3, projectBreakdown.points());
                                        st.setDouble(4, Objects.hash(slug, date, projectBreakdown.points()));
                                    });
                                }
                            }
                            handler.executeBatchUpdate("INSERT INTO curseforge.project_breakdown (slug, date, points, hash) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING;", consumers);
                        } catch (JsonParseException | WebResultException | IllegalStateException e) {
                            LOGGER.error("Error while getting project breakdown", e);
                        }
                    }
                    tConsumers.add(st -> {
                        st.setLong(1, transactionDatum.id());
                        st.setDouble(2, transactionDatum.pointChange());
                        st.setInt(3, transactionDatum.type().getId());
                        st.setLong(4, date);
                    });

                }
            }
            LOGGER.info("Inserting {} Transactions", tConsumers.size());
            handler.executeBatchUpdate("INSERT INTO curseforge.transaction (id, point_change, type, date) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING;", tConsumers);
            LOGGER.info("Inserted {} Transactions", tConsumers.size());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public IResult<List<TransactionData>> get() {
        return CurseAuthorsAPI.getTransactions();
    }

}
