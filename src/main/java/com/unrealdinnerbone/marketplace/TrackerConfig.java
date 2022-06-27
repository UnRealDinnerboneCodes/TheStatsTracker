package com.unrealdinnerbone.marketplace;

import com.unrealdinnerbone.config.IConfigCreator;
import com.unrealdinnerbone.config.config.StringConfig;

public class TrackerConfig
{
    private final StringConfig curseforgeToken;

    public TrackerConfig(IConfigCreator configCreator) {
        this.curseforgeToken = configCreator.createString("token", "");
    }

    public String getCurseforgeToken() {
        return curseforgeToken.getValue();
    }
}
