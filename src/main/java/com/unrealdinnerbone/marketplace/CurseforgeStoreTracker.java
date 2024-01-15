package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.curseapi.api.CurseAPI;
import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.CategoryData;
import com.unrealdinnerbone.curseauthorsapi.api.ItemData;
import com.unrealdinnerbone.unreallib.exception.WebResultException;
import com.unrealdinnerbone.unreallib.json.exception.JsonParseException;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

public class CurseforgeStoreTracker implements IStatsTracker{

    @Override
    public void run(CFHandler handler, CurseAPI curseAPI) throws ExecutionException, InterruptedException, WebResultException, JsonParseException {
        for (CategoryData category : CurseAuthorsAPI.getRewardStore().getNow().categories()) {
            handler.executeUpdate("INSERT INTO curseforge.reward_group (id, name) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET name = ?;", preparedStatement -> {
                preparedStatement.setInt(1, category.id());
                preparedStatement.setString(2, category.name());
                preparedStatement.setString(3, category.name());
            });
            for (ItemData availbleItem : category.availbleItems()) {
                handler.executeUpdate("INSERT INTO curseforge.reward_item (id, name, group_id) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET name = ?", preparedStatement -> {
                    preparedStatement.setInt(1, availbleItem.id());
                    preparedStatement.setString(2, availbleItem.name());
                    preparedStatement.setInt(3, category.id());
                    preparedStatement.setString(4, availbleItem.name());
                });

                //id, amount, cost
                handler.executeUpdate("INSERT INTO curseforge.reward_data (id, amount, cost, time) VALUES (?, ?, ?, ?)", preparedStatement -> {
                    preparedStatement.setInt(1, availbleItem.id());
                    preparedStatement.setInt(2, availbleItem.cachedQuantity());
                    preparedStatement.setInt(3, availbleItem.pointPrice());
                    preparedStatement.setLong(4, Instant.now().getEpochSecond());
                });
            }
        }
    }
}
