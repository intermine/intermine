create table comments (
    comment_id integer not null primary key,
    gene integer references gene(gene_id),
    value text not null
);

create table gene (
    gene_id integer not null primary key, 
    identifer text not null unique
);


