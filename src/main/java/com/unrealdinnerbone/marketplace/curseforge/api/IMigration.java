package com.unrealdinnerbone.marketplace.curseforge.api;

import com.unrealdinnerbone.postgresslib.PostgressHandler;

public interface IMigration {

    void run(PostgressHandler handler);
}
