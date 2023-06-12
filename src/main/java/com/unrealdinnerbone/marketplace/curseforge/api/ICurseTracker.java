package com.unrealdinnerbone.marketplace.curseforge.api;

import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.apiutils.IResult;

public interface ICurseTracker<T> {

    void run(PostgressHandler handler, T t);

    IResult<T> get();
}
