package com.unrealdinnerbone.marketplace.curseforge.api;

import com.unrealdinnerbone.postgresslib.PostgresHandler;

public interface IMigration {

    void run(PostgresHandler handler);
}
