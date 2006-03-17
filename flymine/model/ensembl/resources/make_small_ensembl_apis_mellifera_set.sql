drop database ensembl_apis_mellifera_small;
create database ensembl_apis_mellifera_small;

USE ensembl_apis_mellifera_small;

create table coord_system as select * from apis_mellifera_core_36_2c.coord_system;
create table meta_coord as select * from apis_mellifera_core_36_2c.meta_coord;

--Get the first 20 genes
create table gene as select * from apis_mellifera_core_36_2c.gene
where seq_region_id = 16058 limit 20;

--EMPTY FOR APIS MELLIFERA
create table karyotype as select * from apis_mellifera_core_36_2c.karyotype
where seq_region_id = 16058 limit 0;

create table transcript as select * from apis_mellifera_core_36_2c.transcript
where gene_id in (select gene_id from gene);

create table exon_transcript as select * from apis_mellifera_core_36_2c.exon_transcript
where transcript_id in (select transcript_id from transcript);

create table exon as select * from apis_mellifera_core_36_2c.exon
where exon_id in (select exon_id from exon_transcript);

create table xref as select * from apis_mellifera_core_36_2c.xref
where xref_id in (select display_xref_id from gene);
insert into xref select * from apis_mellifera_core_36_2c.xref
where xref_id in (select display_xref_id from transcript);

create table external_db as select * from apis_mellifera_core_36_2c.external_db
where external_db_id in (select external_db_id from xref);

create table translation as select * from apis_mellifera_core_36_2c.translation
where transcript_id in (select transcript_id from transcript)
and start_exon_id in (select exon_id from exon)
and end_exon_id in (select exon_id from exon);

create table assembly as select * from apis_mellifera_core_36_2c.assembly
where asm_seq_region_id = 16058 limit 20;
insert into assembly select * from apis_mellifera_core_36_2c.assembly
where asm_seq_region_id = 16058 limit 20;

//create the table with no rows - add others as we go...
create table seq_region as select * from apis_mellifera_core_36_2c.seq_region limit 0;

insert into seq_region select * from apis_mellifera_core_36_2c.seq_region
where seq_region_id in (select seq_region_id from transcript) limit 20;

insert into seq_region select * from apis_mellifera_core_36_2c.seq_region
where seq_region_id in (select seq_region_id from assembly) limit 20;

insert into seq_region select * from apis_mellifera_core_36_2c.seq_region
where seq_region_id in (select cmp_seq_region_id from assembly);

create index idx_sr_id on seq_region(seq_region_id);

create table simple_feature as select * from apis_mellifera_core_36_2c.simple_feature
where seq_region_id in (select seq_region_id from seq_region);

create table repeat_feature as select * from apis_mellifera_core_36_2c.repeat_feature
where seq_region_id in (select seq_region_id from seq_region);

create table repeat_consensus as select * from apis_mellifera_core_36_2c.repeat_consensus
where repeat_consensus_id in (select repeat_consensus_id from repeat_feature);

create table marker_feature as select * from apis_mellifera_core_36_2c.marker_feature
where seq_region_id in (select seq_region_id from seq_region);

create table marker as select * from apis_mellifera_core_36_2c.marker
where marker_id in (select marker_id from marker_feature);

create table marker_synonym as select * from apis_mellifera_core_36_2c.marker_synonym
where marker_synonym_id in (select display_marker_synonym_id from marker);

create table exon_stable_id as select * from apis_mellifera_core_36_2c.exon_stable_id
where exon_id in (select exon_id from exon);

create table gene_stable_id as select * from apis_mellifera_core_36_2c.gene_stable_id
where gene_id in (select gene_id from gene);

create table transcript_stable_id as select * from apis_mellifera_core_36_2c.transcript_stable_id
where transcript_id in (select transcript_id from transcript);

create table translation_stable_id as select * from apis_mellifera_core_36_2c.translation_stable_id
where translation_id in (select translation_id from translation);

create table analysis as select * from apis_mellifera_core_36_2c.analysis;

create table analysis_description as select * from apis_mellifera_core_36_2c.analysis_description
where analysis_id in (select analysis_id from analysis);



--CREATE SOME EMPTY TABLES--
create table qtl_synonym as select * from apis_mellifera_core_36_2c.qtl_synonym limit 0;
create table qtl as select * from apis_mellifera_core_36_2c.qtl limit 0;
create table qtl_feature as select * from apis_mellifera_core_36_2c.qtl_feature limit 0;
create table alt_allele as select * from apis_mellifera_core_36_2c.alt_allele limit 0;
create table seq_region_attrib as select * from apis_mellifera_core_36_2c.seq_region_attrib limit 0;
create table translation_attrib as select * from apis_mellifera_core_36_2c.translation_attrib limit 0;
create table transcript_attrib as select * from apis_mellifera_core_36_2c.transcript_attrib limit 0;
create table attrib_type as select * from apis_mellifera_core_36_2c.attrib_type limit 0;
create table misc_attrib as select * from apis_mellifera_core_36_2c.misc_attrib limit 0;
create table misc_feature as select * from apis_mellifera_core_36_2c.misc_feature limit 0;
create table misc_feature_misc_set as select * from apis_mellifera_core_36_2c.misc_feature_misc_set limit 0;
create table misc_set as select * from apis_mellifera_core_36_2c.misc_set limit 0;
create table map as select * from apis_mellifera_core_36_2c.map limit 0;
create table marker_map_location as select * from apis_mellifera_core_36_2c.marker_map_location limit 0;
create table assembly_exception as select * from apis_mellifera_core_36_2c.assembly_exception limit 0;





























