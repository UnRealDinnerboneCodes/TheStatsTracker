package com.unrealdinnerbone.marketplace.curseforge.api;

import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.apiutils.result.IResult;

public interface ICurseTracker<T> {

    void run(Tracker.Config config, PostgressHandler handler, T t);

    IResult<T> get();
}
