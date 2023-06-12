package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.exception.WebResultException;
import com.unrealdinnerbone.unreallib.json.exception.JsonParseException;

import java.util.concurrent.ExecutionException;

public interface IStatsTracker {
    void run(PostgressHandler handler) throws ExecutionException, InterruptedException, WebResultException, JsonParseException;

}
