drop database ensembl_anoph_small;
create database ensembl_anoph_small;

USE ensembl_anoph_small;

create table coord_system as select * from anopheles_gambiae_core_37_3.coord_system;
create table meta_coord as select * from anopheles_gambiae_core_37_3.meta_coord;

--Get some suitable seq_region_id's - i.e. Chromosomes...
create table gene_seq_region_id as select seq_region_id from (
    select seq_region_id from anopheles_gambiae_core_37_3.seq_region
    where coord_system_id = 1 and name not like '%_hap_%'
) bob LIMIT 1;
create index idx_gsrid_sr_id on gene_seq_region_id(seq_region_id);

create table gene as select * from anopheles_gambiae_core_37_3.gene
where seq_region_id in (select seq_region_id from gene_seq_region_id) LIMIT 20;
create index idx_g_id on gene(gene_id);
create index idx_g_x_id on gene(display_xref_id);
create index idx_g_sr_id on gene(seq_region_id);

--#Ditch the temp table
drop table gene_seq_region_id;

create table karyotype as select * from anopheles_gambiae_core_37_3.karyotype
where seq_region_id in (select distinct seq_region_id from gene);
create index idx_k_id on karyotype(karyotype_id);

create table transcript as select * from anopheles_gambiae_core_37_3.transcript
where gene_id in (select gene_id from gene);
create index idx_trscrpt_id on transcript(transcript_id);
create index idx_trs_seq_reg_id on transcript(seq_region_id);
create index idx_trs_xref_id on transcript(display_xref_id);

create table exon_transcript as select * from anopheles_gambiae_core_37_3.exon_transcript
where transcript_id in (select transcript_id from transcript);
create index idx_et_trscrpt_id on exon_transcript(transcript_id);
create index idx_et_exon_id on exon_transcript(exon_id);

create table exon as select * from anopheles_gambiae_core_37_3.exon
where exon_id in (select exon_id from exon_transcript);
create index idx_exon_id on exon(exon_id);

create table xref as select * from anopheles_gambiae_core_37_3.xref
where xref_id in (select display_xref_id from gene);
insert into xref select * from anopheles_gambiae_core_37_3.xref
where xref_id in (select display_xref_id from transcript)
and xref_id not in (select xref_id from xref);

create table external_db as select * from anopheles_gambiae_core_37_3.external_db
where external_db_id in (select external_db_id from xref);
create index idx_e_db_id on external_db(external_db_id);

create table translation as select * from anopheles_gambiae_core_37_3.translation
where transcript_id in (select transcript_id from transcript)
and start_exon_id in (select exon_id from exon)
and end_exon_id in (select exon_id from exon);
create index idx_trans_id on translation(translation_id);

--#Create the seq_region table with no rows - add rows as we go...
create table seq_region as select * from anopheles_gambiae_core_37_3.seq_region limit 0;
insert into seq_region select * from anopheles_gambiae_core_37_3.seq_region
where seq_region_id in (select distinct seq_region_id from gene);

--#Make the assembly table that maps the chromosomes(groups) to contigs(chunks)
create table assembly as select * from anopheles_gambiae_core_37_3.assembly
where asm_seq_region_id in (select seq_region_id from seq_region);
create index idx_asm_asr_id on assembly(asm_seq_region_id);
create index idx_asm_csr_id on assembly(cmp_seq_region_id);

insert into seq_region select * from anopheles_gambiae_core_37_3.seq_region
where seq_region_id in (select cmp_seq_region_id from assembly);
--#Make a useful index on seq_region...
create index idx_sr_id on seq_region(seq_region_id);

create table simple_feature as select * from anopheles_gambiae_core_37_3.simple_feature
where seq_region_id in (select seq_region_id from seq_region);
create index idx_sf_id on simple_feature(simple_feature_id);

create table repeat_feature as select * from anopheles_gambiae_core_37_3.repeat_feature
where seq_region_id in (select seq_region_id from seq_region);
create index idx_rf_id on repeat_feature(repeat_feature_id);
create index idx_rf_sr_id on repeat_feature(seq_region_id);
create index idx_rf_rc_id on repeat_feature(repeat_consensus_id);

create table repeat_consensus as select * from anopheles_gambiae_core_37_3.repeat_consensus
where repeat_consensus_id in (select repeat_consensus_id from repeat_feature);
create index idx_rc_id on repeat_consensus(repeat_consensus_id);

create table marker_feature as select * from anopheles_gambiae_core_37_3.marker_feature
where seq_region_id in (select seq_region_id from seq_region);

create table marker as select * from anopheles_gambiae_core_37_3.marker
where marker_id in (select marker_id from marker_feature);

create table marker_synonym as select * from anopheles_gambiae_core_37_3.marker_synonym
where marker_synonym_id in (select display_marker_synonym_id from marker);

create table exon_stable_id as select * from anopheles_gambiae_core_37_3.exon_stable_id
where exon_id in (select exon_id from exon);
create index idx_esid_e_id on exon_stable_id(exon_id);
create index idx_esid_s_id on exon_stable_id(stable_id);

create table gene_stable_id as select * from anopheles_gambiae_core_37_3.gene_stable_id
where gene_id in (select gene_id from gene);
create index idx_gsid_e_id on gene_stable_id(gene_id);
create index idx_gsid_s_id on gene_stable_id(stable_id);

create table transcript_stable_id as select * from anopheles_gambiae_core_37_3.transcript_stable_id
where transcript_id in (select transcript_id from transcript);
create index idx_tspsid_t_id on transcript_stable_id(transcript_id);
create index idx_tspsid_s_id on transcript_stable_id(stable_id);

create table translation_stable_id as select * from anopheles_gambiae_core_37_3.translation_stable_id
where translation_id in (select translation_id from translation);
create index idx_tslsid_t_id on translation_stable_id(translation_id);
create index idx_tslsid_s_id on translation_stable_id(stable_id);

create table analysis as select * from anopheles_gambiae_core_37_3.analysis;
create index idx_a_id on analysis(analysis_id);

create table analysis_description as select * from anopheles_gambiae_core_37_3.analysis_description
where analysis_id in (select analysis_id from analysis);
create index idx_ad_a_id on analysis_description(analysis_id);

create table dna as select * from anopheles_gambiae_core_37_3.dna
where seq_region_id in (select seq_region_id from seq_region);
create index idx_d_sr_id on dna(seq_region_id);

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



