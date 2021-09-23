package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.postgresslib.PostgressHandler;

public interface IStatsTracker {
    void run(PostgressHandler handler);

}
