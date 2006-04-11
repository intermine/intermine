drop database ensembl_apis_mellifera_clean;
create database ensembl_apis_mellifera_clean;

USE ensembl_apis_mellifera_clean;

create table coord_system as select * from apis_mellifera_core_37_2d.coord_system;
create table meta_coord as select * from apis_mellifera_core_37_2d.meta_coord;

--#Get some suitable seq_region_id's - i.e. Chromosomes...
create table gene_seq_region_id as select seq_region_id from (
    select seq_region_id from apis_mellifera_core_37_2d.seq_region
    where coord_system_id = 2 and name not like '%Un%'
) bob;
create index idx_gsrid_sr_id on gene_seq_region_id(seq_region_id);

create table gene as select * from apis_mellifera_core_37_2d.gene
where seq_region_id in (select seq_region_id from gene_seq_region_id);
create index idx_g_id on gene(gene_id);
create index idx_g_x_id on gene(display_xref_id);
create index idx_g_sr_id on gene(seq_region_id);

--#Ditch the temp table
drop table gene_seq_region_id;

--#EMPTY FOR APIS MELLIFERA
create table karyotype as select * from apis_mellifera_core_37_2d.karyotype;

create table transcript as select * from apis_mellifera_core_37_2d.transcript
where gene_id in (select gene_id from gene);
create index idx_trscrpt_id on transcript(transcript_id);
create index idx_trs_seq_reg_id on transcript(seq_region_id);
create index idx_trs_xref_id on transcript(display_xref_id);

create table exon_transcript as select * from apis_mellifera_core_37_2d.exon_transcript
where transcript_id in (select transcript_id from transcript);
create index idx_et_trscrpt_id on exon_transcript(transcript_id);
create index idx_et_exon_id on exon_transcript(exon_id);

create table exon as select * from apis_mellifera_core_37_2d.exon
where exon_id in (select exon_id from exon_transcript);
create index idx_exon_id on exon(exon_id);

create table xref as select * from apis_mellifera_core_37_2d.xref
where xref_id in (select display_xref_id from gene);
insert into xref select * from apis_mellifera_core_37_2d.xref
where xref_id in (select display_xref_id from transcript)
and xref_id not in (select xref_id from xref);

create table external_db as select * from apis_mellifera_core_37_2d.external_db
where external_db_id in (select external_db_id from xref);
create index idx_e_db_id on external_db(external_db_id);

create table translation as select * from apis_mellifera_core_37_2d.translation
where transcript_id in (select transcript_id from transcript)
and start_exon_id in (select exon_id from exon)
and end_exon_id in (select exon_id from exon);
create index idx_trans_id on translation(translation_id);

--#Create the seq_region table with no rows - add rows as we go...
create table seq_region as select * from apis_mellifera_core_37_2d.seq_region limit 0;
//##Insert the rows related to the gene table (transcript and exon share the same seq_regions...)
insert into seq_region select * from apis_mellifera_core_37_2d.seq_region
where seq_region_id in (select distinct seq_region_id from gene);

--#Make the assembly table that maps the chromosomes(groups) to contigs(chunks)
create table assembly as select * from apis_mellifera_core_37_2d.assembly
where asm_seq_region_id in (select seq_region_id from seq_region);
create index idx_asm_asr_id on assembly(asm_seq_region_id);
create index idx_asm_csr_id on assembly(cmp_seq_region_id);

insert into seq_region select * from apis_mellifera_core_37_2d.seq_region
where seq_region_id in (select cmp_seq_region_id from assembly);
--#Make a useful index on seq_region...
create index idx_sr_id on seq_region(seq_region_id);

create table simple_feature as select * from apis_mellifera_core_37_2d.simple_feature
where seq_region_id in (select seq_region_id from seq_region);
create index idx_sf_id on simple_feature(simple_feature_id);

create table repeat_feature as select * from apis_mellifera_core_37_2d.repeat_feature
where seq_region_id in (select seq_region_id from seq_region);
create index idx_rf_id on repeat_feature(repeat_feature_id);
create index idx_rf_sr_id on repeat_feature(seq_region_id);
create index idx_rf_rc_id on repeat_feature(repeat_consensus_id);

create table repeat_consensus as select * from apis_mellifera_core_37_2d.repeat_consensus
where repeat_consensus_id in (select repeat_consensus_id from repeat_feature);
create index idx_rc_id on repeat_consensus(repeat_consensus_id);

create table marker_feature as select * from apis_mellifera_core_37_2d.marker_feature
where seq_region_id in (select seq_region_id from seq_region);

create table marker as select * from apis_mellifera_core_37_2d.marker
where marker_id in (select marker_id from marker_feature);

create table marker_synonym as select * from apis_mellifera_core_37_2d.marker_synonym
where marker_id in (select marker_id from marker);

create table exon_stable_id as select * from apis_mellifera_core_37_2d.exon_stable_id
where exon_id in (select exon_id from exon);
create index idx_esid_e_id on exon_stable_id(exon_id);
create index idx_esid_s_id on exon_stable_id(stable_id);

create table gene_stable_id as select * from apis_mellifera_core_37_2d.gene_stable_id
where gene_id in (select gene_id from gene);
create index idx_gsid_e_id on gene_stable_id(gene_id);
create index idx_gsid_s_id on gene_stable_id(stable_id);

create table transcript_stable_id as select * from apis_mellifera_core_37_2d.transcript_stable_id
where transcript_id in (select transcript_id from transcript);
create index idx_tspsid_t_id on transcript_stable_id(transcript_id);
create index idx_tspsid_s_id on transcript_stable_id(stable_id);

create table translation_stable_id as select * from apis_mellifera_core_37_2d.translation_stable_id
where translation_id in (select translation_id from translation);
create index idx_tslsid_t_id on translation_stable_id(translation_id);
create index idx_tslsid_s_id on translation_stable_id(stable_id);

create table analysis as select * from apis_mellifera_core_37_2d.analysis;
create index idx_a_id on analysis(analysis_id);

create table analysis_description as select * from apis_mellifera_core_37_2d.analysis_description
where analysis_id in (select analysis_id from analysis);
create index idx_ad_a_id on analysis_description(analysis_id);

create table dna as select * from apis_mellifera_core_37_2d.dna
where seq_region_id in (select seq_region_id from seq_region);
create index idx_d_sr_id on dna(seq_region_id);

--CREATE SOME EMPTY TABLES--
create table qtl_synonym as select * from apis_mellifera_core_37_2d.qtl_synonym limit 0;
create table qtl as select * from apis_mellifera_core_37_2d.qtl limit 0;
create table qtl_feature as select * from apis_mellifera_core_37_2d.qtl_feature limit 0;
create table alt_allele as select * from apis_mellifera_core_37_2d.alt_allele limit 0;
create table seq_region_attrib as select * from apis_mellifera_core_37_2d.seq_region_attrib limit 0;
create table translation_attrib as select * from apis_mellifera_core_37_2d.translation_attrib limit 0;
create table transcript_attrib as select * from apis_mellifera_core_37_2d.transcript_attrib limit 0;
create table attrib_type as select * from apis_mellifera_core_37_2d.attrib_type limit 0;
create table misc_attrib as select * from apis_mellifera_core_37_2d.misc_attrib limit 0;
create table misc_feature as select * from apis_mellifera_core_37_2d.misc_feature limit 0;
create table misc_feature_misc_set as select * from apis_mellifera_core_37_2d.misc_feature_misc_set limit 0;
create table misc_set as select * from apis_mellifera_core_37_2d.misc_set limit 0;
create table map as select * from apis_mellifera_core_37_2d.map limit 0;
create table marker_map_location as select * from apis_mellifera_core_37_2d.marker_map_location limit 0;
create table assembly_exception as select * from apis_mellifera_core_37_2d.assembly_exception limit 0;




