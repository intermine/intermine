package WormBase::Update::Intermine::GenomicAnnotations;

use Moose;
use Bio::SeqIO;

extends qw/WormBase::Update/;

# The symbolic name of this step
has 'step' => (
    is => 'ro',
    default => 'fetch genomic annotations in gff3',
    );

has 'datadir' => (
    is => 'ro',
    lazy_build => 1);

sub _build_datadir {
    my $self = shift;
    my $release = $self->release;
    my $datadir   = join("/",$self->intermine_staging,$release);
    $self->_make_dir($datadir);
    return "$datadir";
}

sub run {
    my $self = shift;    
    my $datadir = $self->datadir;
    chdir $datadir or $self->log->logdie("cannot chdir to local data directory: $datadir");

    my $release     = $self->release;
    my $all_species = $self->species;
    my $ftp_host    = $self->production_ftp_host;
    
    foreach my $species (@$all_species) {	
	my $taxonid = $species->taxon_id;
	my $name    = $species->symbolic_name;
	next unless $name =~ /elegans/;

	$self->_make_dir("$datadir/genomic_annotations");
	my $gff_dir = "$datadir/genomic_annotations/$name";
	$self->_make_dir($gff_dir);
	chdir $gff_dir or $self->log->logdie("cannot chdir to local data directory: $gff_dir");

	my $gff3 = "ftp://$ftp_host/pub/wormbase/releases/$release/species/$name/$name.$release.annotations.gff3.gz";
	my $gff3_mirrored = "$name.$release.annotations.gff3.gz";
	my $gff3_output   = "$name.$taxonid.current.annotations.gff3";
	$self->mirror_uri({ uri    => $gff3,
			    output => $gff3_mirrored,
			    msg    => "mirroring genomic annotations for $name" });
	
	$self->process_gff($gff3_mirrored,$gff3_output);
    }

    # Update the datadir current symlink
    $self->update_staging_dir_symlink();
}


sub process_gff {
    my ($self,$file,$output_file) = @_;

    open IN,"/bin/gunzip -c $file |" or $self->log->logdie("Couldn't open $file for processing: $!");
    my %data;

    while (<IN>) {
	next if /^#/;      # ignore comments
	s/^CHROMOSOME_//i; # remove chromosome identifiers
	s/^chr//i;         # 
	my $type = $self->get_type($_);
	my $source = $self->get_source($_);
	next if $source eq 'history';
	
	$data{$type} = () unless( exists $data{$type} );
	push(@{$data{$type}}, $_);
    }
    close IN;
    system("mv $file $file.original.gz");
    
    open OUT,"| /bin/gzip -c > $output_file";
#    my @order = qw(gene mRNA exon CDS); 
my @order = qw(gene rRNA_primary_transcript nc_primary_transcript miRNA_primary_transcript primary_transcript transposable_element_insertion_site transposable_element tRNA pseudogenic_transcript snoRNA snRNA miRNA mRNA exon CDS three_prime_UTR five_prime_UTR complex_substitution substitution SNP deletion insertion_site operon);
    my @types = keys %data;
    
    foreach my $desired_order (@order) {
	foreach my $line (@{$data{$desired_order}}) {
	    chomp $line;
	    my ($ref,$source,$type,$start,$stop,$score,$strand,$phase,$attributes) = split("\t",$line);
	    
	    # Let's only keep WormBase features for now.
	    # Kev's prototypical GFF3 has source of Wormbase.
	    # but my GFF2->GFF3 does not.
	    # next unless $source eq 'WormBase';
	    
	    # Mangle our attributes, removing the prefixes that will break integration.
	    $attributes =~ s/Name=Gene:/Name=/g;
#		$attributes =~ s/ID=RGD/ID=/g;
#		$attributes =~ s/Parent=RGD/Parent=/g;
	    $attributes =~ s/=\s+/=/g;
	    $attributes =~ s/,\t/,/g;
	    $attributes =~ s/\s+?;/;/g;
#		$attributes =~ s/associatedGene[\d\D]+?ID/ID/g;
#		$attributes =~ s/\t//g;
#		$attributes =~ s/\w+=;//g;
#		$attributes =~ s/,\s+/,/g;
	    
	    print OUT join("\t",$ref,$source,$type,$start,$stop,$score,$strand,$phase,$attributes),"\n";
	}
    }
    close OUT;
#    system("sort -k1,1 -k3,3n -k4,4n -T /tmp tmp.gff > c_elegans.current.annotations.sorted.gff3");
#    system("gzip c_elegans.current.annotations.sorted.gff3");
#    unlink "tmp.gff";

}

sub get_type {
    my ($self,$line) = @_;
    my @data = split(/\t/,$line);
    return $data[2];
}


sub get_source {
    my ($self,$line) = @_;
    my @data = split(/\t/,$line);
    return $data[1];
}





=pod

Allele	complex_substitution
Allele	deletion
Allele	insertion_site
Allele	SNP
Allele	substitution
Allele	transposable_element_insertion_site
binding_site	binding_site
binding_site_region	binding_site
BLAT_Caen_EST_BEST	translated_nucleotide_match
BLAT_Caen_EST_OTHER	translated_nucleotide_match
BLAT_Caen_mRNA_BEST	translated_nucleotide_match
BLAT_Caen_mRNA_OTHER	translated_nucleotide_match
BLAT_EST_BEST	EST_match
BLAT_EST_OTHER	EST_match
BLAT_mRNA_BEST	cDNA_match
BLAT_mRNA_OTHER	cDNA_match
BLAT_ncRNA_BEST	nucleotide_match
BLAT_ncRNA_OTHER	nucleotide_match
BLAT_NEMATODE	translated_nucleotide_match
BLAT_NEMBASE	translated_nucleotide_match
BLAT_OST_BEST	expressed_sequence_match
BLAT_OST_OTHER	expressed_sequence_match
BLAT_RST_BEST	expressed_sequence_match
BLAT_RST_OTHER	expressed_sequence_match
BLAT_TC1_BEST	nucleotide_match
BLAT_TC1_OTHER	nucleotide_match
BLAT_WASHU	translated_nucleotide_match
cDNA_for_RNAi	experimental_result_region
CGH_allele	deletion
Chronogram	reagent
deprecated_operon	operon
DNAseI_hypersensitive_site	DNAseI_hypersensitive_site
dust	low_complexity_region
Expr_pattern	reagent
Expr_profile	experimental_result_region
Genefinder	CDS
GenePair_STS	PCR_product
Genomic_canonical	assembly_component
history	exon
history	nc_primary_transcript
history	primary_transcript
history	pseudogenic_transcript
inverted	inverted_repeat
jigsaw	CDS
Link	assembly_component
mass_spec_genome	translated_nucleotide_match
mGene	CDS
Million_mutation	SNP
Mos_insertion_allele	transposable_element_insertion_site
Non_coding_transcript	exon
Non_coding_transcript	nc_primary_transcript
oligo	oligo
Oligo_set	reagent
operon	operon
Orfeome	PCR_product
pmid18538569	G_quartet
polyA_signal_sequence	polyA_signal_sequence
polyA_site	polyA_site
Promoterome	PCR_product
promoter	promoter
regulatory_region	regulatory_region
RepeatMasker	repeat_region
RNAi_primary	RNAi_reagent
RNAi_secondary	RNAi_reagent
RNASeq	base_call_error_correction
RNASEQ.Hillier.Aggregate	CDS
RNASEQ.Hillier	CDS
RNASeq_splice	intron
RNASeq	transcription_end_site
RNASeq	TSS
SAGE_tag_genomic_unique	SAGE_tag
SAGE_tag_most_three_prime	SAGE_tag
SAGE_tag	SAGE_tag
SAGE_tag_unambiguously_mapped	SAGE_tag
segmental_duplication	duplication
SL1	SL1_acceptor_site
SL2	SL2_acceptor_site
tandem	tandem_repeat
TEC_RED	nucleotide_match
Transposon_CDS	exon
Transposon_CDS	transposable_element
Transposon_Pseudogene	exon
Transposon_Pseudogene	transposable_element
Transposon	transposable_element
tRNAscan-SE-1.23	exon
tRNAscan-SE-1.23	tRNA
twinscan	CDS
UTRome	three_prime_UTR
Vancouver_fosmid	nucleotide_match
WormBase	CDS
WormBase	exon
WormBase	gene
WormBase	intron
WormBase	miRNA
WormBase	miRNA_primary_transcript
WormBase	mRNA
WormBase	nc_primary_transcript
WormBase	pseudogenic_transcript
WormBase	rRNA_primary_transcript
WormBase	snoRNA
WormBase	snRNA
wublastx	protein_match

=cut


=pod


Not yet handled

BLAT_RST_BEST	expressed_sequence_match
RNAi_primary	RNAi_reagent
binding_site_region	binding_site
BLAT_EST_BEST	EST_match
SAGE_tag_most_three_prime	SAGE_tag
Oligo_set	reagent
DNAseI_hypersensitive_site	DNAseI_hypersensitive_site
SAGE_tag	SAGE_tag
Vancouver_fosmid	nucleotide_match
TEC_RED	nucleotide_match
RepeatMasker	repeat_region
RNASeq	TSS
SAGE_tag_genomic_unique	SAGE_tag
dust	low_complexity_region
binding_site	binding_site
Expr_pattern	reagent
BLAT_OST_BEST	expressed_sequence_match
inverted	inverted_repeat
wublastx	protein_match
GenePair_STS	PCR_product
SAGE_tag_unambiguously_mapped	SAGE_tag
polyA_signal_sequence	polyA_signal_sequence
BLAT_RST_OTHER	expressed_sequence_match
BLAT_mRNA_BEST	cDNA_match
BLAT_ncRNA_BEST	nucleotide_match
Chronogram	reagent
tandem	tandem_repeat
SL1	SL1_acceptor_site
BLAT_mRNA_OTHER	cDNA_match
Link	assembly_component
deprecated_operon	operon
Orfeome	PCR_product
oligo	oligo
BLAT_OST_OTHER	expressed_sequence_match
cDNA_for_RNAi	experimental_result_region

SL2	SL2_acceptor_site
BLAT_EST_OTHER	EST_match
RNASeq	base_call_error_correction
Expr_profile	experimental_result_region
polyA_site	polyA_site

RNASeq	transcription_end_site
BLAT_ncRNA_OTHER	nucleotide_match

pmid18538569	G_quartet

RNAi_secondary	RNAi_reagent
operon	operon

BLAT_Caen_mRNA_BEST	translated_nucleotide_match
BLAT_Caen_mRNA_OTHER	translated_nucleotide_match
BLAT_NEMBASE	translated_nucleotide_match
BLAT_NEMATODE	translated_nucleotide_match
BLAT_Caen_EST_BEST	translated_nucleotide_match
mass_spec_genome	translated_nucleotide_match
BLAT_WASHU	translated_nucleotide_match
BLAT_Caen_EST_OTHER	translated_nucleotide_match

BLAT_TC1_BEST	nucleotide_match
regulatory_region	regulatory_region
jigsaw	CDS
BLAT_TC1_OTHER	nucleotide_match
Promoterome	PCR_product
segmental_duplication	duplication
history	primary_transcript
promoter	promoter


=cut





1;
