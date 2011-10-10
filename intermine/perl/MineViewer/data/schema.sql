create table comments (
    id integer not null primary key,
    item text references item(identifer),
    value text not null
);

create table item (
    identifer text primary key
);


