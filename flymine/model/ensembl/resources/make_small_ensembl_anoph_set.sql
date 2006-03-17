drop database ensembl_anoph_small;
create database ensembl_anoph_small;

USE ensembl_anoph_small;

create table coord_system as select * from anopheles_gambiae_core_37_3.coord_system;
create table meta_coord as select * from anopheles_gambiae_core_37_3.meta_coord;

--Get some genes - first get the smallest chromosome's seq_region_id
create table gene_seq_region_id as select seq_region_id from (
    select seq_region_id, count(seq_region_id) as the_count
    from anopheles_gambiae_core_37_3.gene group by seq_region_id order by the_count
) bob limit 1;

create table gene as select * from anopheles_gambiae_core_37_3.gene
where seq_region_id in (select seq_region_id from gene_seq_region_id) limit 20;

create table karyotype as select * from anopheles_gambiae_core_37_3.karyotype
where seq_region_id in (select distinct seq_region_id from gene);

create table transcript as select * from anopheles_gambiae_core_37_3.transcript
where gene_id in (select gene_id from gene);

create table exon_transcript as select * from anopheles_gambiae_core_37_3.exon_transcript
where transcript_id in (select transcript_id from transcript);

create table exon as select * from anopheles_gambiae_core_37_3.exon
where exon_id in (select exon_id from exon_transcript);

create table xref as select * from anopheles_gambiae_core_37_3.xref
where xref_id in (select display_xref_id from gene);
insert into xref select * from anopheles_gambiae_core_37_3.xref
where xref_id in (select display_xref_id from transcript);

create table external_db as select * from anopheles_gambiae_core_37_3.external_db
where external_db_id in (select external_db_id from xref);

create table translation as select * from anopheles_gambiae_core_37_3.translation
where transcript_id in (select transcript_id from transcript)
and start_exon_id in (select exon_id from exon)
and end_exon_id in (select exon_id from exon);

create table assembly as select * from anopheles_gambiae_core_37_3.assembly
where asm_seq_region_id in (select distinct seq_region_id from gene_seq_region_id) limit 20;

//create the table with no rows - add others as we go...
create table seq_region as select * from anopheles_gambiae_core_37_3.seq_region
where seq_region_id in (select seq_region_id from transcript);

insert into seq_region select * from anopheles_gambiae_core_37_3.seq_region
where seq_region_id in (select seq_region_id from assembly) limit 20;

insert into seq_region select * from anopheles_gambiae_core_37_3.seq_region
where seq_region_id in (select cmp_seq_region_id from assembly);

create index idx_sr_id on seq_region(seq_region_id);

create table simple_feature as select * from anopheles_gambiae_core_37_3.simple_feature
where seq_region_id in (select seq_region_id from seq_region);

create table repeat_feature as select * from anopheles_gambiae_core_37_3.repeat_feature
where seq_region_id in (select seq_region_id from seq_region)
and analysis_id in (select analysis_id from anopheles_gambiae_core_37_3.analysis where logic_name = 'TRF');

create table repeat_consensus as select * from anopheles_gambiae_core_37_3.repeat_consensus
where repeat_consensus_id in (select repeat_consensus_id from repeat_feature);

create table marker_feature as select * from anopheles_gambiae_core_37_3.marker_feature
where seq_region_id in (select seq_region_id from seq_region);

create table marker as select * from anopheles_gambiae_core_37_3.marker
where marker_id in (select marker_id from marker_feature);

create table marker_synonym as select * from anopheles_gambiae_core_37_3.marker_synonym
where marker_synonym_id in (select display_marker_synonym_id from marker);

create table exon_stable_id as select * from anopheles_gambiae_core_37_3.exon_stable_id
where exon_id in (select exon_id from exon);

create table gene_stable_id as select * from anopheles_gambiae_core_37_3.gene_stable_id
where gene_id in (select gene_id from gene);

create table transcript_stable_id as select * from anopheles_gambiae_core_37_3.transcript_stable_id
where transcript_id in (select transcript_id from transcript);

create table translation_stable_id as select * from anopheles_gambiae_core_37_3.translation_stable_id
where translation_id in (select translation_id from translation);


create table analysis as select * from anopheles_gambiae_core_37_3.analysis;
--where analysis_id in (select analysis_id from gene);
--insert into analysis select * from anopheles_gambiae_core_37_3.analysis
--where analysis_id in (select analysis_id from simple_feature);


create table analysis_description as select * from anopheles_gambiae_core_37_3.analysis_description
where analysis_id in (select analysis_id from analysis);



--CREATE SOME EMPTY TABLES--
create table qtl_synonym as select * from anopheles_gambiae_core_37_3.qtl_synonym limit 0;
create table qtl as select * from anopheles_gambiae_core_37_3.qtl limit 0;
create table qtl_feature as select * from anopheles_gambiae_core_37_3.qtl_feature limit 0;
create table alt_allele as select * from anopheles_gambiae_core_37_3.alt_allele limit 0;
create table seq_region_attrib as select * from anopheles_gambiae_core_37_3.seq_region_attrib limit 0;
create table translation_attrib as select * from anopheles_gambiae_core_37_3.translation_attrib limit 0;
create table transcript_attrib as select * from anopheles_gambiae_core_37_3.transcript_attrib limit 0;
create table attrib_type as select * from anopheles_gambiae_core_37_3.attrib_type limit 0;
create table misc_attrib as select * from anopheles_gambiae_core_37_3.misc_attrib limit 0;
create table misc_feature as select * from anopheles_gambiae_core_37_3.misc_feature limit 0;
create table misc_feature_misc_set as select * from anopheles_gambiae_core_37_3.misc_feature_misc_set limit 0;
create table misc_set as select * from anopheles_gambiae_core_37_3.misc_set limit 0;
create table map as select * from anopheles_gambiae_core_37_3.map limit 0;
create table marker_map_location as select * from anopheles_gambiae_core_37_3.marker_map_location limit 0;
create table assembly_exception as select * from anopheles_gambiae_core_37_3.assembly_exception limit 0;








