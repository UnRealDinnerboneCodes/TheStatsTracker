package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.ProjectBreakdownData;
import com.unrealdinnerbone.curseauthorsapi.api.ProjectsBreakdownData;
import com.unrealdinnerbone.curseauthorsapi.api.TransactionData;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.postgresslib.PostgresConsumer;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.LogHelper;
import com.unrealdinnerbone.unreallib.apiutils.IReturnResult;
import com.unrealdinnerbone.unreallib.discord.DiscordWebhook;
import com.unrealdinnerbone.unreallib.discord.EmbedObject;
import com.unrealdinnerbone.unreallib.json.JsonParseException;
import org.slf4j.Logger;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DailyPointTracker implements ICurseTracker<List<TransactionData>> {

    private static final Logger LOGGER = LogHelper.getLogger();

    private static final String DISCORD_WEBHOOK = System.getenv("DISCORD_WEBHOOK");

    private static final DecimalFormat CURR_FORMAT = new DecimalFormat("$#,###.00");

    private long lastTime = 0;

    @Override
    public void run(PostgressHandler handler, List<TransactionData> transactionData) {
        for (TransactionData transactionDatum : transactionData) {
            if(transactionDatum.type() == 1) {
                long date = transactionDatum.getDateCreated().getEpochSecond();
                try {
                    ProjectsBreakdownData projectsBreakdownData = CurseAuthorsAPI.getBreakdown(transactionDatum.id()).getExceptionally();
                    if(date > lastTime) {
                        EmbedObject.EmbedObjectBuilder builder = EmbedObject.builder();
                        double totalPoints = 0;
                        List<ProjectBreakdownData> projectBreakdownData = new ArrayList<>(projectsBreakdownData.projectsBreakdown());
                        projectBreakdownData.sort((o1, o2) -> Double.compare(o2.points(), o1.points()));
                        for (ProjectBreakdownData projectBreakdown : projectBreakdownData) {
                            builder = builder.field(EmbedObject.Field.of(projectBreakdown.projectName(), projectBreakdown.points() + " (" + CURR_FORMAT.format(projectBreakdown.points() * 0.05) + ")", false));
                            totalPoints += projectBreakdown.points();
                        }
                        DiscordWebhook.of(DISCORD_WEBHOOK)
                                .addEmbed(builder.title("Total Points " + totalPoints + " (" + CURR_FORMAT.format(totalPoints * 0.05) + ")").build())
                                .setUsername("Curse Points Bot")
                                .execute();
                        lastTime = date;
                    }
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
                    handler.executeBatchUpdate("INSERT INTO curseforge.project_breakdown (slug, date, points, hash) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING;", consumers);
                } catch (JsonParseException | IOException | InterruptedException e) {
                    LOGGER.error("Error while getting project breakdown", e);
                }
            }
        }
    }

    @Override
    public IReturnResult<List<TransactionData>> get() {
        return CurseAuthorsAPI.getTransactions();
    }

}
