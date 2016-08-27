create table todo (
    id bigint unsigned not null auto_increment,
    text text not null,
    limit_at datetime not null,
    created_at datetime not null,
    updated_at datetime not null,
    PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
insert into todo (text, limit_at, created_at, updated_at) values ('test', current_timestamp, current_timestamp, current_timestamp);
