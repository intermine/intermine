create table common_annotation as select common_annotation_id, ann_id, name, text, comments from interpro_src_data.common_annotation;

create table cv_entry_type as select cv_entry_type_id, code, abbrev, description from interpro_src_data.cv_entry_type;

create table cv_relation as select cv_relation_id, code, abbrev, description, forward, reverse from interpro_src_data.cv_relation;

create table cv_evidence as select cv_evidence_id, code, abbrev, description from interpro_src_data.cv_evidence;

create table cv_database as select cv_database_id, dbcode, dbname, dborder, dbshort from interpro_src_data.cv_database;

create table db_version as select db_version_id, dbcode, version, entry_count, file_date, load_date from interpro_src_data.db_version where dbcode in (select dbcode from cv_database);
alter table db_version add column cv_database_id INTEGER;
update db_version set cv_database_id=(select cv_database_id from cv_database where db_version.dbcode=cv_database.dbcode);

create table taxonomy (taxonomy_id serial, taxa_id integer);
insert into taxonomy (taxa_id)  select distinct tax_id from interpro_src_data.taxonomy2protein where tax_id in (6239,7165,7227,180454);

create table protein2taxonomy as select taxonomy2protein_id, protein_ac, tax_id, hierarchy, tax_name_concat from interpro_src_data.taxonomy2protein where tax_id in (select taxa_id from taxonomy);
alter table protein2taxonomy add column protein_id INTEGER;
alter table protein2taxonomy add column taxonomy_id INTEGER;
alter table protein2taxonomy rename column taxonomy2protein_id to protein2taxonomy_id;
create index idx_p2t_protein_ac on protein2taxonomy(protein_ac);

create table protein as select p.protein_id, p.protein_ac, p.name, p.dbcode, p.crc64, p.len, p.fragment, p.struct_flag from interpro_src_data.protein p, protein2taxonomy p2t where p.protein_ac = p2t.protein_ac;
alter table protein add column cv_database_id INTEGER;
create index idx_p_protein_ac on protein(protein_ac);
update protein set cv_database_id=(select cv_database_id from cv_database where protein.dbcode=cv_database.dbcode);

update protein2taxonomy set protein_id=(select protein_id from protein where protein2taxonomy.protein_ac=protein.protein_ac);
update protein2taxonomy set taxonomy_id=(select taxonomy_id from taxonomy where protein2taxonomy.tax_id=taxonomy.taxa_id);

create table method as select method_id, method_ac, name, dbcode, method_date, skip_flag from interpro_src_data.method;
alter table method add column cv_database_id INTEGER;
update method set cv_database_id=(select cv_database_id from cv_database where method.dbcode=cv_database.dbcode);
create index idx_m_method_ac on method(method_ac);

create table entry as select entry_id, entry_ac, entry_type, name, checked, created, short_name from interpro_src_data.entry;
alter table entry add column cv_entry_type_id INTEGER;
update entry set cv_entry_type_id=(select cv_entry_type_id from cv_entry_type where entry.entry_type=cv_entry_type.code);
create index idx_e_protein_ac on entry(entry_ac);

create table supermatch as select s.supermatch_id, s.protein_ac, s.entry_ac, s.pos_from, s.pos_to from interpro_src_data.supermatch s, protein p where s.protein_ac = p.protein_ac;
alter table supermatch add column protein_id INTEGER;
alter table supermatch add column entry_id INTEGER;
update supermatch set protein_id=(select protein_id from protein where supermatch.protein_ac=protein.protein_ac);
update supermatch set entry_id=(select entry_id from entry where supermatch.entry_ac=entry.entry_ac);

create table matches as select m.matches_id, m.protein_ac, m.method_ac, m.pos_from, m.pos_to, m.status, m.dbcode, m.evidence, m.seq_date, m.match_date, m.score from interpro_src_data.matches m, protein p where m.protein_ac = p.protein_ac and m.pos_from is not null and m.pos_to is not null;
alter table matches add column protein_id INTEGER;
alter table matches add column method_id INTEGER;
alter table matches add column cv_database_id INTEGER;
alter table matches add column cv_evidence_id INTEGER;

update matches set protein_id=(select protein_id from protein where matches.protein_ac=protein.protein_ac);
update matches set method_id=(select method_id from method where matches.method_ac=method.method_ac);
update matches set cv_database_id=(select cv_database_id from cv_database where matches.dbcode=cv_database.dbcode);
update matches set cv_evidence_id=(select cv_evidence_id from cv_evidence where matches.evidence=cv_evidence.code);

create table entry2common_annotation as select entry2common_id as entry2common_annotation_id, entry_ac, ann_id, order_in from interpro_src_data.entry2common;
alter table entry2common_annotation add column entry_id INTEGER;
alter table entry2common_annotation add column common_annotation_id INTEGER;
update entry2common_annotation set entry_id=(select entry_id from entry where entry2common_annotation.entry_ac=entry.entry_ac);
update entry2common_annotation set common_annotation_id=(select common_annotation_id from common_annotation where entry2common_annotation.ann_id=common_annotation.ann_id);

create table entry2cv_database as select entry_xref_id as entry2cv_database_id, entry_ac, dbcode, ac, name from interpro_src_data.entry_xref;
alter table entry2cv_database add column entry_id INTEGER;
alter table entry2cv_database add column cv_database_id INTEGER;
update entry2cv_database set entry_id=(select entry_id from entry where entry2cv_database.entry_ac=entry.entry_ac);
update entry2cv_database set cv_database_id=(select cv_database_id from cv_database where entry2cv_database.dbcode=cv_database.dbcode);

create table entry2method as select entry2method_id, entry_ac, method_ac, evidence, ida from interpro_src_data.entry2method;
alter table entry2method add column entry_id INTEGER;
alter table entry2method add column method_id INTEGER;
alter table entry2method add column cv_evidence_id INTEGER;
update entry2method set entry_id=(select entry_id from entry where entry2method.entry_ac=entry.entry_ac);
update entry2method set method_id=(select method_id from method where entry2method.method_ac=method.method_ac);
update entry2method set cv_evidence_id=(select cv_evidence_id from cv_evidence where entry2method.evidence=cv_evidence.code);

create table entry2comp as select entry2comp_id, entry1_ac, entry2_ac, relation from interpro_src_data.entry2comp;
alter table entry2comp add column cv_relation_id INTEGER;
alter table entry2comp add column entry1_id INTEGER;
alter table entry2comp add column entry2_id INTEGER;
update entry2comp set cv_relation_id=(select cv_relation_id from cv_relation where entry2comp.relation=cv_relation.code);
update entry2comp set entry1_id=(select entry_id from entry where entry2comp.entry1_ac=entry.entry_ac);
update entry2comp set entry2_id=(select entry_id from entry where entry2comp.entry2_ac=entry.entry_ac);

create table entry2entry as select entry2entry_id, entry_ac, parent_ac, relation from interpro_src_data.entry2entry;
alter table entry2entry add column cv_relation_id INTEGER;
alter table entry2entry add column entry_id INTEGER;
alter table entry2entry add column parent_id INTEGER;
update entry2entry set cv_relation_id=(select cv_relation_id from cv_relation where entry2entry.relation=cv_relation.code);
update entry2entry set entry_id=(select entry_id from entry where entry2entry.entry_ac=entry.entry_ac);
update entry2entry set parent_id=(select entry_id from entry where entry2entry.parent_ac=entry.entry_ac);



-- ====================== Remove non Family/Domain Entry rows & their relatives - NOT JUST FOR TESTING!
delete from entry where entry_type not in ('D','F');
delete from entry2method where entry_ac not in (select entry_ac from entry);
create index idx_e2m_method_ac on entry2method(method_ac);
create index idx_e2m_entry_ac on entry2method(entry_ac);

delete from entry2common_annotation where entry_ac not in (select entry_ac from entry);

create table entry2comp_to_keep as select entry2comp_id from entry2comp where entry1_ac in (select entry_ac from entry) and entry2_ac in (select entry_ac from entry);
delete from entry2comp where entry2comp_id not in (select entry2comp_id from entry2comp_to_keep);
drop table entry2comp_to_keep;

create table entry2entry_to_keep as select entry2entry_id from entry2entry where parent_ac in (select entry_ac from entry) and entry_ac in (select entry_ac from entry);
delete from entry2entry where entry2entry_id not in (select entry2entry_id from entry2entry_to_keep);
drop table entry2entry_to_keep;

delete from entry2cv_database where entry_ac not in (select entry_ac from entry);
delete from supermatch where entry_ac not in (select entry_ac from entry);

delete from method where method_ac not in (select method_ac from entry2method);
delete from matches where method_ac not in (select method_ac from method);

delete from entry2common_annotation where common_annotation_id in ( select common_annotation_id from common_annotation where name like 'Name goes here');
delete from common_annotation where name like 'Name goes here';

-- ====================== Remove non A.gambiae protein data - TESTING - just reduce amount of protein data...
--delete from protein2taxonomy where tax_id != 7165;
--delete from taxonomy where taxa_id != 7165;
--delete from protein where protein_ac not in (select protein_ac from protein2taxonomy);
--delete from supermatch where protein_ac not in (select protein_ac from protein);
--delete from matches where protein_ac not in (select protein_ac from protein);

