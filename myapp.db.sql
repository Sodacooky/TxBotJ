create table global
(
    key   text not null
        constraint global_pk
            primary key,
    value text
);

create table group_warmer
(
    group_id               integer           not null
        constraint group_warmer_pk
            primary key,
    last_message_timestamp integer default 0 not null
);

create table repeater
(
    group_id          INTEGER not null
        constraint repeater_pk
            primary key,
    last_repeat_time  integer not null,
    last_msg_of_group TEXT    not null,
    passed_msg_amount integer not null,
    last_repeat_msg   text    not null
);


