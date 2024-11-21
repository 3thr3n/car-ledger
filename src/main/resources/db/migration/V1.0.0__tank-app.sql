create sequence sequence_account start with 5 increment by 1;

create sequence sequence_bill start with 5 increment by 1;

create sequence sequence_car start with 5 increment by 1;

create table Account
(
    id       bigint not null,
    max_cars integer,
    user_id  varchar(255) unique,
    primary key (id)
);

create table Bill
(
    id             bigint not null,
    _day           date,
    distance       numeric(38, 2),
    estimate       numeric(38, 2),
    price_per_unit numeric(38, 2),
    unit           numeric(38, 2),
    car_id         bigint not null,
    primary key (id),
    unique (_day, unit, distance, car_id)
);

create table Car
(
    id           bigint not null,
    _description varchar(255),
    user_id      bigint,
    primary key (id)
);

alter table if exists Bill
    add constraint FKqr4dj8bwkuf9rxo3gn4vvmgiq
        foreign key (car_id)
            references Car;

alter table if exists Car
    add constraint FKkvv668b9tgb46lkuk20gcx49s
        foreign key (user_id)
            references Account;
