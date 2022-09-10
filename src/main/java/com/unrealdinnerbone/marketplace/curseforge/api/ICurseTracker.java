package com.unrealdinnerbone.marketplace.curseforge.api;

import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.apiutils.IReturnResult;

public interface ICurseTracker<T> {

    void run(PostgressHandler handler, T t);

    IReturnResult<T> get();
}
