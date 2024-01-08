package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.Project;
import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.postgresslib.PostgresConsumer;
import com.unrealdinnerbone.postgresslib.PostgressHandler;
import com.unrealdinnerbone.unreallib.apiutils.result.IResult;

import java.util.ArrayList;
import java.util.List;

public class ProjectSlugTracker implements ICurseTracker<List<Project>> {
    @Override
    public void run(Tracker.Config config, PostgressHandler handler, List<Project> project) {
        List<PostgresConsumer> statements = new ArrayList<>();
        for (Project project1 : project) {
            statements.add(ps -> {
                ps.setString(1, project1.slug());
                ps.setString(2, project1.name());
            });
        }
        handler.executeBatchUpdate("INSERT INTO curseforge.projects (slug, name) VALUES (?, ?) ON CONFLICT DO NOTHING;", statements);
    }

    @Override
    public IResult<List<Project>> get() {
        return CurseAuthorsAPI.getProjects();
    }
}
