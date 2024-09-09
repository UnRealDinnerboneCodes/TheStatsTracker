package com.unrealdinnerbone.marketplace.curseforge.trackers;

import com.unrealdinnerbone.curseapi.api.CurseAPI;
import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.File;
import com.unrealdinnerbone.curseauthorsapi.api.GameVersion;
import com.unrealdinnerbone.curseauthorsapi.api.Project;
import com.unrealdinnerbone.marketplace.CFHandler;
import com.unrealdinnerbone.marketplace.CurseforgeTracker;
import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.marketplace.curseforge.api.ICurseTracker;
import com.unrealdinnerbone.postgresslib.PostgresConsumer;
import com.unrealdinnerbone.unreallib.LogHelper;
import com.unrealdinnerbone.unreallib.apiutils.result.IResult;
import org.slf4j.Logger;

import java.sql.Array;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FileDownloadTracker implements ICurseTracker<List<Project>> {

    private static final Logger LOGGER = LogHelper.getLogger();
    @Override
    public void run(Tracker.Config config, CFHandler handler, CurseAPI curseAPI, List<Project> projects) {
        for (Project project : projects) {
            List<PostgresConsumer> files = new ArrayList<>();
            List<PostgresConsumer> fileDownloads = new ArrayList<>();
            try {
                for (File file : CurseAuthorsAPI.getProjectFiles(project.id()).getNow()) {
                    LOGGER.info("File: {} has {} downloads", file.fileName(), file.downloads());
                    Stream<String> versions = file.gameVersions().stream().map(GameVersion::label);
                    List<String> javaVersions = new ArrayList<>();
                    List<String> loaderVersions = new ArrayList<>();
                    List<String> minecraftVersions = new ArrayList<>();
                    for (String version : versions.toArray(String[]::new)) {
                        if(CurseforgeTracker.isStringLoader(version)) {
                            loaderVersions.add(version);
                            continue;
                        }
                        if(CurseforgeTracker.isStringJava(version)) {
                            javaVersions.add(version);
                            continue;
                        }
                        minecraftVersions.add(version);
                    }
                    files.add(preparedStatement -> {
                        preparedStatement.setInt(1, project.id());
                        preparedStatement.setInt(2, file.id());
                        Array text = handler.createArray("text", minecraftVersions.toArray());
                        preparedStatement.setArray(3, text);
                        Array loader = handler.createArray("text", loaderVersions.toArray());
                        preparedStatement.setArray(4, loader);
                        Array java = handler.createArray("text", javaVersions.toArray());
                        preparedStatement.setArray(5, java);
                        preparedStatement.setString(6, file.fileName());
                    });
                    Instant startOfToday = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);

                    fileDownloads.add(preparedStatement -> {
                        preparedStatement.setInt(1, file.id());
                        preparedStatement.setTimestamp(2, java.sql.Timestamp.from(startOfToday));
                        preparedStatement.setLong(3, file.downloads());
                    });
                }
            }catch (Exception e) {
                LOGGER.error("Failed to get files for project: {}", project.name(), e);
            }
            handler.executeBatchUpdate("INSERT INTO curseforge.file (project, id, versions, java_versions, loader_versions, name) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (id) do update set versions = EXCLUDED.versions, name = EXCLUDED.name;", files);
            handler.executeBatchUpdate("INSERT INTO curseforge.file_downloads (file, timestamp, downloads) VALUES (?, ?, ?) ON CONFLICT (file, timestamp) do update set downloads = EXCLUDED.downloads;", fileDownloads);
        }
    }

    @Override
    public IResult<List<Project>> get() {
        return CurseAuthorsAPI.getProjects();
    }
}
