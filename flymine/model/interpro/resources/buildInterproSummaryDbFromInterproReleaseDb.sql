drop schema interpro_prod cascade;
create schema interpro_prod;


create table interpro_prod.cv_entry_type as select code, abbrev, description from interpro_src_data.cv_entry_type;
alter table interpro_prod.cv_entry_type add column cv_entry_type_id INTEGER(32) AUTO_INCREMENT primary key first;

create table interpro_prod.cv_relation as select code, abbrev, description, forward, reverse from interpro_src_data.cv_relation;
alter table interpro_prod.cv_relation add column cv_relation_id INTEGER(32) AUTO_INCREMENT primary key first;

create table interpro_prod.cv_evidence as select code, abbrev, description from interpro_src_data.cv_evidence;
alter table interpro_prod.cv_evidence add column cv_evidence_id INTEGER(32) AUTO_INCREMENT primary key first;

create table interpro_prod.cv_database as select dbcode, dbname, dborder, dbshort from interpro_src_data.cv_database;
alter table interpro_prod.cv_database add column cv_database_id INTEGER(32) AUTO_INCREMENT primary key first;

create table interpro_prod.db_version as select dbcode, version, entry_count, file_date, load_date from interpro_src_data.db_version where dbcode in (select dbcode from interpro_prod.cv_database);
alter table interpro_prod.db_version add column db_version_id INTEGER(32) AUTO_INCREMENT primary key first;
alter table interpro_prod.db_version add column cv_database_id INTEGER(32) after db_version_id;
update interpro_prod.db_version dbv_outer set dbv_outer.cv_database_id=(select cvd_inner.cv_database_id from interpro_prod.cv_database cvd_inner where dbv_outer.dbcode=cvd_inner.dbcode);

create table interpro_prod.taxonomy as select DISTINCT tax_id as taxa_id from interpro_src_data.taxonomy2protein where tax_id in (6239,7165,7227,180454);
alter table interpro_prod.taxonomy add column taxonomy_id INTEGER(32) AUTO_INCREMENT primary key first;

create table interpro_prod.protein2taxonomy as select protein_ac, tax_id, hierarchy, tax_name_concat from interpro_src_data.taxonomy2protein where tax_id in (select taxa_id from interpro_prod.taxonomy);
alter table interpro_prod.protein2taxonomy add column protein2taxonomy_id INTEGER(32) AUTO_INCREMENT primary key first;
alter table interpro_prod.protein2taxonomy add column protein_id INTEGER(32) after protein2taxonomy_id;
alter table interpro_prod.protein2taxonomy add column taxonomy_id INTEGER(32) after protein_id;
create index idx_p2t_protein_ac on interpro_prod.protein2taxonomy(protein_ac);

create table interpro_prod.protein as select ip10.protein_ac, ip10.name, ip10.dbcode, ip10.crc64, ip10.len, ip10.fragment, ip10.struct_flag from interpro_src_data.protein ip10, interpro_prod.protein2taxonomy itp2t where ip10.protein_ac = itp2t.protein_ac;
alter table interpro_prod.protein add column protein_id INTEGER(32) AUTO_INCREMENT primary key first;
alter table interpro_prod.protein add column cv_database_id INTEGER(32) after protein_id;
create index idx_p_protein_ac on interpro_prod.protein(protein_ac);
update interpro_prod.protein p_outer set p_outer.cv_database_id=(select cvd_inner.cv_database_id from interpro_prod.cv_database cvd_inner where p_outer.dbcode=cvd_inner.dbcode);

update interpro_prod.protein2taxonomy p2t_outer set p2t_outer.protein_id=(select p_inner.protein_id from interpro_prod.protein p_inner where p2t_outer.protein_ac=p_inner.protein_ac);
update interpro_prod.protein2taxonomy p2t_outer set p2t_outer.taxonomy_id=(select t_inner.taxonomy_id from interpro_prod.taxonomy t_inner where p2t_outer.tax_id=t_inner.taxa_id);


create table interpro_prod.method as select method_ac, name, dbcode, method_date, skip_flag from interpro_src_data.method;
alter table interpro_prod.method add column method_id INTEGER(32) AUTO_INCREMENT primary key first;
alter table interpro_prod.method add column cv_database_id INTEGER(32) after method_id;
update interpro_prod.method m_outer set m_outer.cv_database_id=(select cvd_inner.cv_database_id from interpro_prod.cv_database cvd_inner where m_outer.dbcode=cvd_inner.dbcode);
create index idx_m_method_ac on interpro_prod.method(method_ac);


create table interpro_prod.entry as select entry_ac, entry_type, name, checked, created, short_name from interpro_src_data.entry;
alter table interpro_prod.entry add column entry_id INTEGER(32) AUTO_INCREMENT primary key first;
alter table interpro_prod.entry add column cv_entry_type_id INTEGER(32) after entry_id;
update interpro_prod.entry e_outer set e_outer.cv_entry_type_id=(select cvet_inner.cv_entry_type_id from interpro_prod.cv_entry_type cvet_inner where e_outer.entry_type=cvet_inner.code);
create index idx_e_protein_ac on interpro_prod.entry(entry_ac);

create table interpro_prod.supermatch as select i10s.protein_ac, i10s.entry_ac, i10s.pos_from, i10s.pos_to from interpro_src_data.supermatch i10s, interpro_prod.protein itp where i10s.protein_ac = itp.protein_ac;
alter table interpro_prod.supermatch add column supermatch_id INTEGER(32) AUTO_INCREMENT primary key first;
alter table interpro_prod.supermatch add column protein_id INTEGER(32) after supermatch_id;
alter table interpro_prod.supermatch add column entry_id INTEGER(32) after protein_id;
update interpro_prod.supermatch s_outer set s_outer.protein_id=(select p_inner.protein_id from interpro_prod.protein p_inner where s_outer.protein_ac=p_inner.protein_ac);
update interpro_prod.supermatch s_outer set s_outer.entry_id=(select e_inner.entry_id from interpro_prod.entry e_inner where s_outer.entry_ac=e_inner.entry_ac);

create table interpro_prod.matches as select i10m.protein_ac, i10m.method_ac, i10m.pos_from, i10m.pos_to, i10m.status, i10m.dbcode, i10m.evidence, i10m.seq_date, i10m.match_date, i10m.score from interpro_src_data.matches i10m, interpro_prod.protein itp where i10m.protein_ac = itp.protein_ac;
alter table interpro_prod.matches add column matches_id INTEGER(32) AUTO_INCREMENT primary key first;
alter table interpro_prod.matches add column protein_id INTEGER(32) after matches_id;
alter table interpro_prod.matches add column method_id INTEGER(32) after protein_id;
alter table interpro_prod.matches add column cv_database_id INTEGER(32) after method_id;
alter table interpro_prod.matches add column cv_evidence_id INTEGER(32) after cv_database_id;

update interpro_prod.matches m_outer set m_outer.protein_id=(select p_inner.protein_id from interpro_prod.protein p_inner where m_outer.protein_ac=p_inner.protein_ac);
update interpro_prod.matches m_outer set m_outer.method_id=(select m_inner.method_id from interpro_prod.method m_inner where m_outer.method_ac=m_inner.method_ac);
update interpro_prod.matches m_outer set m_outer.cv_database_id=(select cvd_inner.cv_database_id from interpro_prod.cv_database cvd_inner where m_outer.dbcode=cvd_inner.dbcode);
update interpro_prod.matches m_outer set m_outer.cv_evidence_id=(select cve_inner.cv_evidence_id from interpro_prod.cv_evidence cve_inner where m_outer.evidence=cve_inner.code);


create table interpro_prod.common_annotation as select ann_id, name, text, comments from interpro_src_data.common_annotation;
alter table interpro_prod.common_annotation add column common_annotation_id INTEGER(32) AUTO_INCREMENT primary key first;


create table interpro_prod.entry2common_annotation as select entry_ac, ann_id, order_in from interpro_src_data.entry2common;
alter table interpro_prod.entry2common_annotation add column entry2common_annotation_id INTEGER(32) AUTO_INCREMENT primary key first;
alter table interpro_prod.entry2common_annotation add column entry_id INTEGER(32) after entry2common_annotation_id;
alter table interpro_prod.entry2common_annotation add column common_annotation_id INTEGER(32) after entry_id;
update interpro_prod.entry2common_annotation e2ca_outer set e2ca_outer.entry_id=(select e_inner.entry_id from interpro_prod.entry e_inner where e2ca_outer.entry_ac=e_inner.entry_ac);
update interpro_prod.entry2common_annotation e2ca_outer set e2ca_outer.common_annotation_id=(select ca_inner.common_annotation_id from interpro_prod.common_annotation ca_inner where e2ca_outer.ann_id=ca_inner.ann_id);

create table interpro_prod.entry2cv_database as select entry_ac, dbcode, ac, name from interpro_src_data.entry_xref;
alter table interpro_prod.entry2cv_database add column entry2cv_database_id INTEGER(32) AUTO_INCREMENT primary key first;
alter table interpro_prod.entry2cv_database add column entry_id INTEGER(32) after entry2cv_database_id;
alter table interpro_prod.entry2cv_database add column cv_database_id INTEGER(32) after entry_id;
update interpro_prod.entry2cv_database e2cvd_outer set e2cvd_outer.entry_id=(select e_inner.entry_id from interpro_prod.entry e_inner where e2cvd_outer.entry_ac=e_inner.entry_ac);
update interpro_prod.entry2cv_database e2cvd_outer set e2cvd_outer.cv_database_id=(select cvd_inner.cv_database_id from interpro_prod.cv_database cvd_inner where e2cvd_outer.dbcode=cvd_inner.dbcode);

create table interpro_prod.entry2method as select entry_ac, method_ac, evidence, ida from interpro_src_data.entry2method;
alter table interpro_prod.entry2method add column entry2method_id INTEGER(32) AUTO_INCREMENT primary key first;
alter table interpro_prod.entry2method add column entry_id INTEGER(32) after entry2method_id;
alter table interpro_prod.entry2method add column method_id INTEGER(32) after entry_id;
alter table interpro_prod.entry2method add column cv_evidence_id INTEGER(32) after method_id;
update interpro_prod.entry2method e2m_outer set e2m_outer.entry_id=(select e_inner.entry_id from interpro_prod.entry e_inner where e2m_outer.entry_ac=e_inner.entry_ac);
update interpro_prod.entry2method e2m_outer set e2m_outer.method_id=(select m_inner.method_id from interpro_prod.method m_inner where e2m_outer.method_ac=m_inner.method_ac);
update interpro_prod.entry2method e2m_outer set e2m_outer.cv_evidence_id=(select cve_inner.cv_evidence_id from interpro_prod.cv_evidence cve_inner where e2m_outer.evidence=cve_inner.code);

create table interpro_prod.entry2comp as select entry1_ac, entry2_ac, relation from interpro_src_data.entry2comp;
alter table interpro_prod.entry2comp add column entry2comp_id INTEGER(32) AUTO_INCREMENT primary key first;
alter table interpro_prod.entry2comp add column cv_relation_id INTEGER(32) after entry2comp_id;
alter table interpro_prod.entry2comp add column entry1_id INTEGER(32) after cv_relation_id;
alter table interpro_prod.entry2comp add column entry2_id INTEGER(32) after entry1_id ;
update interpro_prod.entry2comp e2c_outer set e2c_outer.cv_relation_id=(select cvr_inner.cv_relation_id from interpro_prod.cv_relation cvr_inner where e2c_outer.relation=cvr_inner.code);
update interpro_prod.entry2comp e2c_outer set e2c_outer.entry1_id=(select e_inner.entry_id from interpro_prod.entry e_inner where e2c_outer.entry1_ac=e_inner.entry_ac);
update interpro_prod.entry2comp e2c_outer set e2c_outer.entry2_id=(select e_inner.entry_id from interpro_prod.entry e_inner where e2c_outer.entry2_ac=e_inner.entry_ac);

create table interpro_prod.entry2entry as select entry_ac, parent_ac, relation from interpro_src_data.entry2entry;
alter table interpro_prod.entry2entry add column entry2entry_id INTEGER(32) AUTO_INCREMENT primary key first;
alter table interpro_prod.entry2entry add column cv_relation_id INTEGER(32) after entry2entry_id;
alter table interpro_prod.entry2entry add column entry_id INTEGER(32) after cv_relation_id;
alter table interpro_prod.entry2entry add column parent_id INTEGER(32) after entry_id;
update interpro_prod.entry2entry e2e_outer set e2e_outer.cv_relation_id=(select cvr_inner.cv_relation_id from interpro_prod.cv_relation cvr_inner where e2e_outer.relation=cvr_inner.code);
update interpro_prod.entry2entry e2e_outer set e2e_outer.entry_id=(select e_inner.entry_id from interpro_prod.entry e_inner where e2e_outer.entry_ac=e_inner.entry_ac);
update interpro_prod.entry2entry e2e_outer set e2e_outer.parent_id=(select e_inner.entry_id from interpro_prod.entry e_inner where e2e_outer.parent_ac=e_inner.entry_ac);



-- ====================== Remove non Family/Domain Entry rows & their relatives - NOT JUST FOR TESTING!
delete from interpro_prod.entry where entry_type not in ('D','F');
delete from interpro_prod.entry2method where entry_ac not in (select entry_ac from interpro_prod.entry);
create index idx_e2m_method_ac on interpro_prod.entry2method(method_ac);
create index idx_e2m_entry_ac on interpro_prod.entry2method(entry_ac);

delete from interpro_prod.entry2common_annotation where entry_ac not in (select entry_ac from interpro_prod.entry);

create table interpro_prod.entry2comp_to_keep as select entry2comp_id from interpro_prod.entry2comp where entry1_ac in (select entry_ac from interpro_prod.entry) and entry2_ac in (select entry_ac from interpro_prod.entry);
delete from interpro_prod.entry2comp where entry2comp_id not in (select entry2comp_id from interpro_prod.entry2comp_to_keep);
drop table interpro_prod.entry2comp_to_keep;

create table interpro_prod.entry2entry_to_keep as select entry2entry_id from interpro_prod.entry2entry where parent_ac in (select entry_ac from interpro_prod.entry) and entry_ac in (select entry_ac from interpro_prod.entry);
delete from interpro_prod.entry2entry where entry2entry_id not in (select entry2entry_id from interpro_prod.entry2entry_to_keep);
drop table interpro_prod.entry2entry_to_keep;

delete from interpro_prod.entry2cv_database where entry_ac not in (select entry_ac from interpro_prod.entry);
delete from interpro_prod.supermatch where entry_ac not in (select entry_ac from interpro_prod.entry);

delete from interpro_prod.method where method_ac not in (select method_ac from interpro_prod.entry2method);
delete from interpro_prod.matches where method_ac not in (select method_ac from interpro_prod.method);

delete from entry2common_annotation where common_annotation_id in ( select common_annotation_id from common_annotation where name like 'Name goes here');
delete from common_annotation where name like 'Name goes here';

-- ====================== Remove non A.gambiae protein data - TESTING - just reduce amount of protein data...
--delete from interpro_prod.protein2taxonomy where tax_id != 7165;
--delete from interpro_prod.taxonomy where taxa_id != 7165;
--delete from interpro_prod.protein where protein_ac not in (select protein_ac from interpro_prod.protein2taxonomy);
--delete from interpro_prod.supermatch where protein_ac not in (select protein_ac from interpro_prod.protein);
--delete from interpro_prod.matches where protein_ac not in (select protein_ac from interpro_prod.protein);

