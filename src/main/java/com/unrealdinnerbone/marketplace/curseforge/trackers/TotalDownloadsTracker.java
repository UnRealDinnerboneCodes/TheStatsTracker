package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.DownloadsTotalData;
import com.unrealdinnerbone.curseauthorsapi.api.base.QueryResult;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.apiutils.IResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TotalDownloadsTracker implements ICurseTracker<QueryResult<DownloadsTotalData>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TotalDownloadsTracker.class);


    @Override
    public void run(PostgressHandler handler, QueryResult<DownloadsTotalData> downloadsTotalData) {
        LOGGER.info("Total Downloads at {}: {}", downloadsTotalData.data().total(), downloadsTotalData.getRetrievedAt());
    }

    @Override
    public IResult<QueryResult<DownloadsTotalData>> get() {
        return CurseAuthorsAPI.getTotalDownloads();
    }


}
