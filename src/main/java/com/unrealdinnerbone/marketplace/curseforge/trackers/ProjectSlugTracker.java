package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseapi.api.CurseAPI;
import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.Project;
import com.unrealdinnerbone.marketplace.CFHandler;
import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.unreallib.apiutils.result.IResult;

import java.util.List;

public class ProjectSlugTracker implements ICurseTracker<List<Project>> {
    @Override
    public void run(Tracker.Config config, CFHandler handler, CurseAPI curseAPI, List<Project> project) {
        for (Project project1 : project) {
            handler.addProject(project1.slug(), project1.name(), project1.id());
        }
    }

    @Override
    public IResult<List<Project>> get() {
        return CurseAuthorsAPI.getProjects();
    }
}
