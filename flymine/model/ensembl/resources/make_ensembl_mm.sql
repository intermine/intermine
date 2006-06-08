drop database ensembl_mm;
create database ensembl_mm;

USE ensembl_mm;

create table coord_system as select * from mus_musculus_core_38_35.coord_system;

create table gene_seq_region_id as select seq_region_id from (
    select seq_region_id from mus_musculus_core_38_35.seq_region
    where coord_system_id = 1 and name like '19'
) bob LIMIT 1;
create index idx_gsrid_sr_id on gene_seq_region_id(seq_region_id);

create table gene as select * from mus_musculus_core_38_35.gene
where seq_region_id in (select seq_region_id from gene_seq_region_id);
create index idx_g_id on gene(gene_id);
create index idx_g_x_id on gene(display_xref_id);
create index idx_g_sr_id on gene(seq_region_id);

create table gene_stable_id as select * from mus_musculus_core_38_35.gene_stable_id
where gene_id in (select gene_id from gene);
create index idx_gsid_e_id on gene_stable_id(gene_id);
create index idx_gsid_s_id on gene_stable_id(stable_id);

create table seq_region as select * from mus_musculus_core_38_35.seq_region
where seq_region_id in (select seq_region_id from gene);

create table xref as select * from mus_musculus_core_38_35.xref
where xref_id in (select display_xref_id from gene);
create index idx_x_xid on xref(xref_id);
create index idx_x_eid on xref(external_db_id);

create table external_db as select * from mus_musculus_core_38_35.external_db
where external_db_id in (select external_db_id from xref);



drop table gene_seq_region_id;