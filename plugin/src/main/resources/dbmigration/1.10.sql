-- apply changes
create table rcmobs_mobgroup_spawn_location (
  id                            integer auto_increment not null,
  spawn_group                   varchar(255) not null,
  x                             integer not null,
  y                             integer not null,
  z                             integer not null,
  chunk_x                       integer not null,
  chunk_z                       integer not null,
  world                         varchar(255),
  cooldown                      double not null,
  respawn_treshhold             integer not null,
  last_spawn                    datetime(6),
  constraint pk_rcmobs_mobgroup_spawn_location primary key (id)
);

create table rcmobs_mob_player_kill_log (
  id                            integer auto_increment not null,
  uuid                          varchar(40),
  mob                           varchar(255),
  kill_count                    integer not null,
  constraint pk_rcmobs_mob_player_kill_log primary key (id)
);

create table rcmobs_mob_spawn_location (
  id                            integer auto_increment not null,
  mob                           varchar(255) not null,
  x                             integer not null,
  y                             integer not null,
  z                             integer not null,
  chunk_x                       integer not null,
  chunk_z                       integer not null,
  world                         varchar(255),
  cooldown                      double not null,
  last_spawn                    datetime(6),
  constraint pk_rcmobs_mob_spawn_location primary key (id)
);

create table rcmobs_player_mob_kill_log (
  id                            integer auto_increment not null,
  uuid                          varchar(40),
  mob                           varchar(255),
  kill_count                    integer not null,
  constraint pk_rcmobs_player_mob_kill_log primary key (id)
);

create table rcmobs_player_kill_player_log (
  id                            integer auto_increment not null,
  killer                        varchar(40),
  victim                        varchar(40),
  world                         varchar(255),
  timestamp                     datetime(6),
  constraint pk_rcmobs_player_kill_player_log primary key (id)
);

create table rcmobs_spawned_mobs (
  id                            integer auto_increment not null,
  uuid                          varchar(40),
  source_id                     varchar(255),
  mob                           varchar(255),
  spawn_time                    datetime(6),
  spawn_location_source_id      integer,
  mob_group_source_id           integer,
  unloaded                      tinyint(1) default 0 not null,
  world                         varchar(255),
  x                             integer not null,
  y                             integer not null,
  z                             integer not null,
  chunk_x                       integer not null,
  chunk_z                       integer not null,
  constraint uq_rcmobs_spawned_mobs_uuid unique (uuid),
  constraint pk_rcmobs_spawned_mobs primary key (id)
);

create table rcmobs_spawned_mob_groups (
  id                            integer auto_increment not null,
  mob_group                     varchar(255),
  spawn_time                    datetime(6),
  spawn_group_location_source_id integer,
  constraint pk_rcmobs_spawned_mob_groups primary key (id)
);

create index ix_rcmobs_spawned_mobs_spawn_location_source_id on rcmobs_spawned_mobs (spawn_location_source_id);
alter table rcmobs_spawned_mobs add constraint fk_rcmobs_spawned_mobs_spawn_location_source_id foreign key (spawn_location_source_id) references rcmobs_mob_spawn_location (id) on delete restrict on update restrict;

create index ix_rcmobs_spawned_mobs_mob_group_source_id on rcmobs_spawned_mobs (mob_group_source_id);
alter table rcmobs_spawned_mobs add constraint fk_rcmobs_spawned_mobs_mob_group_source_id foreign key (mob_group_source_id) references rcmobs_spawned_mob_groups (id) on delete restrict on update restrict;

create index ix_rcmobs_spawned_mob_groups_spawn_group_location_source_id on rcmobs_spawned_mob_groups (spawn_group_location_source_id);
alter table rcmobs_spawned_mob_groups add constraint fk_rcmobs_spawned_mob_groups_spawn_group_location_source_id foreign key (spawn_group_location_source_id) references rcmobs_mobgroup_spawn_location (id) on delete restrict on update restrict;

