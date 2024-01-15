package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.curseapi.api.CurseAPI;
import com.unrealdinnerbone.unreallib.exception.WebResultException;
import com.unrealdinnerbone.unreallib.json.exception.JsonParseException;

import java.util.concurrent.ExecutionException;

public interface IStatsTracker {
    void run(CFHandler handler, CurseAPI curseAPI) throws ExecutionException, InterruptedException, WebResultException, JsonParseException;

}
