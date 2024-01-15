package com.unrealdinnerbone.marketplace.database.tasks;

import com.unrealdinnerbone.curseapi.api.CurseAPI;
import com.unrealdinnerbone.curseapi.api.response.Responses;
import com.unrealdinnerbone.curseapi.quaries.ModQuery;
import com.unrealdinnerbone.curseauthorsapi.CurseAuthorsAPI;
import com.unrealdinnerbone.curseauthorsapi.api.Project;
import com.unrealdinnerbone.marketplace.CFHandler;
import com.unrealdinnerbone.marketplace.Tracker;
import com.unrealdinnerbone.marketplace.database.IDBTask;
import com.unrealdinnerbone.unreallib.LogHelper;
import org.slf4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MigrateV2 implements IDBTask {

    private static final Logger LOGGER = LogHelper.getLogger();

    @Override
    public void run(Tracker.Config config, CFHandler handler, CurseAPI curseAPI) throws SQLException {
        LOGGER.info("Getting Slug -> Project Id's");
        Map<String, Integer> map = handler.slugToIDMap(config);
        ResultSet set = handler.getSet("SELECT * from curseforge.projects");
        while (set.next()) {
            String slug = set.getString("slug");
            String name = set.getString("name");
            if(!map.containsKey(slug)) {
                Responses.SearchMods now = curseAPI.v1().searchMods(ModQuery.builder().slug(slug).gameId(432)).getNow();
                if(now.data().size() != 1) {
                    throw new IllegalStateException("Could not find mod for slug " + slug + " and name " + name + " found " + now .data().size() + " mods");
                }else {
                    LOGGER.info("Found {} mods for slug {} and name {}", now.data().size(), slug, name);
                    map.put(slug, now.data().get(0).id());
                }
            }
        }
        LOGGER.info("Adding id column to projects");
        handler.tryUpdate("""
                ALTER TABLE curseforge.projects
                ADD COLUMN id int;""");

        LOGGER.info("Adding id information to projects");
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String slug = entry.getKey();
            Integer id = entry.getValue();
            LOGGER.info("Adding id {} to slug {}", id, slug);
            if(!handler.executeUpdate("""
                    UPDATE curseforge.projects
                    SET id = ?
                    WHERE slug = ?;""", preparedStatement -> {
                preparedStatement.setLong(1, id);
                preparedStatement.setString(2, slug);
            })) {
                throw new IllegalStateException("Failed to update slug " + slug + " with id " + id);
            };
        }
        LOGGER.info("Setting id to not null");
        handler.tryUpdate("""
                ALTER TABLE curseforge.projects
                ALTER COLUMN id SET NOT NULL;""");

        LOGGER.info("Make id primary key");
        handler.tryUpdate("""
                alter table curseforge.projects
                    drop constraint project_slug;""");
        handler.tryUpdate("""
                alter table curseforge.projects
                    add constraint project_id
                        primary key (id);""");

        LOGGER.info("Updating Projects Downloads to use ids");
        handler.tryUpdate("""
                alter table curseforge.project_downloads
                    add id integer;""");

        handler.tryUpdate("""
                update curseforge.project_downloads
                    set id = projects.id
                    from curseforge.projects
                    where curseforge.project_downloads.project = projects.slug;""");

        LOGGER.info("Fixing primary key");
        handler.tryUpdate("""
                alter table curseforge.project_downloads
                    drop constraint unique_project_time;""");

        handler.tryUpdate("""
                alter table curseforge.project_downloads
                    add constraint unique_project_time
                        unique (id, time);""");

        handler.tryUpdate("""
                alter table curseforge.project_downloads
                    drop column project;""");


        LOGGER.info("Updating Projects Points to use ids");
        handler.tryUpdate("""
                alter table curseforge.project_points
                    add id integer;""");

        handler.tryUpdate("""
                update curseforge.project_points
                    set id = projects.id
                    from curseforge.projects
                    where curseforge.project_points.slug = projects.slug;""");

        LOGGER.info("Fixing primary key");
        handler.tryUpdate("""
                alter table curseforge.project_points
                    drop constraint unique_project_time_points;""");

        handler.tryUpdate("""
                alter table curseforge.project_points
                    add constraint unique_project_time_points
                        unique (id, time);""");

        handler.tryUpdate("""
                alter table curseforge.project_points
                    drop column slug;""");

        LOGGER.info("Creating Files DB");
        handler.tryUpdate("""
                create table curseforge.file
                (
                    project  integer,
                    id       integer,
                    versions text array,
                    name     text,
                    constraint file_id
                            unique (id)
                );""");

        LOGGER.info("Add File Downloads DB");
        handler.tryUpdate("""
                create table curseforge.file_downloads
                (
                    file      integer,
                    timestamp timestamp,
                    downloads integer,
                    constraint file_downloads_time
                        unique (file, timestamp)
                );""");


    }
}
