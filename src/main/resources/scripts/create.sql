create schema curseforge;

create table curseforge.project_downloads
(
    downloads integer,
    time      timestamp,
    id        integer,
    constraint unique_project_time
        unique (id, time)
);

create table curseforge.user_points
(
    points integer,
    time   timestamp
        constraint unique_time
            unique
);

create table curseforge.transaction
(
    id           integer
        constraint unique_id
            unique,
    point_change integer,
    type         integer,
    time         timestamp
);

create table curseforge."order"
(
    id       integer
        constraint unique_project_id
            unique,
    quantity integer,
    item     varchar(255),
    type     integer,
    time     timestamp
);

create table curseforge.project_points
(
    time   timestamp,
    points integer,
    id     integer,
    constraint unique_project_time_points
        unique (id, time)
);

create table curseforge.projects
(
    slug varchar(255) not null,
    name varchar(255),
    id   integer      not null
        constraint project_id
            primary key
);

create table curseforge.version
(
    id integer
);

create table curseforge.file
(
    project  integer,
    id       integer
        constraint file_id
            unique,
    versions text[],
    name     text
);

create table curseforge.file_downloads
(
    file      integer,
    timestamp timestamp,
    downloads integer,
    constraint file_downloads_time
        unique (file, timestamp)
);

create table curseforge.version(
    id integer
);

insert into curseforge.version values (2);

