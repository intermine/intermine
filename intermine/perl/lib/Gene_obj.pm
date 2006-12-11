#!/usr/bin/env perl

package Gene_obj;
use strict;
use Nuc_translator;
use Gene_ontology;
use Storable qw (store retrieve freeze thaw dclone);

=head1 NAME

package Gene_obj

=cut



=head1 DESCRIPTION

    Gene_obj(s) encapsulate the elements of both gene structure and gene function. The gene structure is stored in a hierarchical fashion as follows:

    Gene  =========================================================

    Exon  =========     =========         =========        ========

    CDS      ======     =========         ======

   
    where a Gene is a container for Exon(s), and each Exon is a container for a CDS, and an Exon can contain a single CDS component.  An Exon lacking a CDS exon is an untranslated exon or UTR exon.  The region of an Exon which extends beyond the CDS is also considered a UTR.
  
    
    There are several ways to instantiate gene objects.  A simple example is described:

    Exon and CDS component coordinates can be assigned as hashes.

    ie. 
    
    my %mrna = ( 100 => 200,
	         300 => 500 );

    my %CDS = ( 150=>200,
		300=>450);

    my $sequence = "GACTACATTTAATAGGGCCC"; #string representing the genomic sequence
    my $gene = new Gene_obj();
    
    $gene->{com_name} = "hypothetical protein";

    $gene->populate_gene_obj(\%CDS, \%mRNA, \$sequence);
    print $gene->toString();

    
    
    Alternatively, the individual components of genes (Exons and CDSs) can be instantiated separately and used to build the Gene from the ground up (See packages mRNA_exon_obj and CDS_exon_obj following this Gene_obj documentation).
    
    my $cds_exon = new CDS_exon_obj (150, 200);
    
    my $mRNA_exon = new mRNA_exon_obj (100, 200);
   
    $mRNA_exon->set_CDS_exon_obj($cds_exon);

    my $gene_obj = new Gene_obj ();

    $gene_obj->{gene_name} = "hypothetical gene";
    $gene_obj->{com_name} = "hypothetical protein";
  
    $gene_obj->add_mRNA_exon_obj($mRNA_exon);

    $gene_obj->refine_gene_object();

    $gene_obj->create_all_sequence_types (\$sequence);  #ref to genomic sequence string.    

    print $gene_obj->toString();


    The API below describes useful functions for navigating and manipulating the Gene object along with all of its attributes.
    


=cut






=over 4

=item new()

B<Description:> Constructor for Gene_obj 

B<Parameters:> none

B<Returns:> $gene_obj


The Gene_obj contains several attributes which can be manipulated directly (or by get/set methods if they exist).  These attributes include:

    asmbl_id # identifier for the genomic contig for which this gene is anchored.
    TU_feat_name #feat_names are TIGR temporary identifiers.
    Model_feat_name # temp TIGR identifier for gene models
    locus  #identifier for a gene (TU) ie. T2P3.5
    pub_locus  #another identifier for a gene (TU)   ie. At2g00010
    model_pub_locus #identifier for a gene model (model)  ie. At2g00010.1
    model_locus #analagous to locus, but for model rather than gene (TU)
    alt_locus   #alternative locus 
    gene_name # name for gene
    com_name  # name for gene product 
    comment #internal comment
    pub_comment #comment related to gene
    ec_num   # enzyme commission number
    gene_sym  # gene symbol
    is_5prime_partial # 0|1  missing start codon.
    is_3prime_partial # 0|1  missing stop codon.
    is_pseudogene # 0|1
    curated_com_name # 0|1
    curated_gene_structure # 0|1
    
    ## Other attributes set internally  Access-only, do not set directly.
        
    gene_length  # length of gene span (int).
    mid_pt  # holds midpoint of gene-span
    strand  # [+-]
    protein_seq # holds protein sequence
    protein_seq_length
    CDS_sequence  #holds CDS sequence (translated to protein); based on CDS_exon coordinates
    CDS_seq_length 
    cDNA_sequence  #holds cDNA sequence; based on mRNA exon coordinates.
    cDNA_seq_length 
    gene_sequence #holds unspliced transcript
    gene_sequence_length #length of unspliced transcript
    gene_type # "protein-coding", #default type for gene object.  Could be changed to "rRNA|snoRNA|snRNA|tRNA" to accommodate other gene or feature types.
    num_additional_isoforms # int 
    
    
=back

=cut



sub new {
    shift;
    my $self = { asmbl_id => 0, #genomic contig ID
		 locus => 0,       #text
		 pub_locus => 0,   #text  ie. At2g00010
		 model_pub_locus =>0, #text ie. At2g00010.1
		 model_locus => 0, #text ie. F12G15.1
		 alt_locus => 0,   #text
		 gene_name => 0, #text
		 com_name => 0,    #text
		 comment => 0,
		 curated_com_name => 0,
		 curated_gene_structure => 0,
		 pub_comment => 0, #text
		 ec_num => 0, #text (enzyme commission number)
		 gene_type => "protein-coding", #default type for gene object.  Could be changed to "rRNA|snoRNA|snRNA|tRNA" to accomodate other gene or feature types.
		 gene_sym => 0, #text (gene symbol)
		 mRNA_coords => 0, #assigned to anonymous hash of end5->end3 relative to the parent sequence
		 CDS_coords => 0,  #assigned to anonymous hash of end5->end3 relative to the parent sequence
		 mRNA_exon_objs => 0,  # holds arrayref to mRNA_obj, retrieve only thru method: get_exons()
		 model_span => [],     # holds array ref to (end5,end3) for CDS range of gene.
		 gene_span => [],      # holds array ref to (end5,end3) for mRNA range of gene.
		 gene_length => 0,     # length of gene span (int).
		 mid_pt => 0,         # holds midpoint of gene-span
		 strand => 0,      # [+-]
		 gi => 0,          #text
		 prot_acc => 0,     #text
		 is_pseudogene => 0, # toggle indicating pseudogene if 1.
		 is_5prime_partial => 0, #boolean indicating missing 5' part of gene.
		 is_3prime_partial => 0, #boolean
		 protein_seq => 0,    # holds protein sequence
		 protein_seq_length => 0,
		 CDS_sequence => 0,    #holds CDS sequence (translated to protein); based on CDS_exon coordinates
		 CDS_seq_length => 0,
		 cDNA_sequence => 0,   #holds cDNA sequence; based on mRNA exon coordinates.
		 cDNA_seq_length => 0,
		 gene_sequence => 0, #holds unspliced transcript
		 gene_sequence_length => 0, #length of unspliced transcript
		 TU_feat_name => 0,    #feat_names are TIGR temporary identifiers.
		 Model_feat_name =>0,
		 classification => 'annotated_genes', #type of seq_element.
		 gene_synonyms => [],    #list of synonymous model feat_names
		 GeneOntology=>[], #list of Gene_ontology assignment objects.  ...see GeneOntology.pm
		 
		 ## Additional functional attributes:
		 secondary_gene_names => [],
		 secondary_product_names => [],
		 secondary_gene_symbols => [],
		 secondary_ec_numbers =>[],
		 

		 ## Alternative splicing support.  
		 num_additional_isoforms => 0,  # number of additional isoforms stored in additonal_isoform list below
		 additional_isoforms => [] # stores list of Gene_objs corresponding to the additional isoforms.
		     
	     };
    bless($self);
    return ($self);
}




=over 4

=item erase_gene_structure()

B<Description:> Removes the structural components of a gene (ie. exons, CDSs, coordinate spans, any corresponding sequences)

B<Parameters:> none

B<Returns:> none 

=back

=cut


## erase gene structure
sub erase_gene_structure {
    my $self = shift;
    $self->{mRNA_exon_objs} = 0;
    $self->{model_span} = [];
    $self->{gene_span} = [];
    $self->{gene_length} = 0;
    $self->{strand} = 0;
    $self->{protein_seq} = 0;
    $self->{CDS_sequence} = 0;
    $self->{CDS_seq_length} = 0;
    $self->{cDNA_sequence} = 0;
    $self->{cDNA_seq_length} = 0;
}


=over 4

=item clone_gene()

B<Description:> Clones this Gene_obj by copying attributes from this Gene to a new gene.  Does NOT do a deep clone for all attributes.  See dclone() for a more rigorous cloning method.  This method is safer because all references are not cloned, only the critical ones.

B<Parameters:> none

B<Returns:> new Gene_obj

=back

=cut



## all objects are cloned.  References to data only are not.
sub clone_gene {
    my $self = shift;
    my $clone = new Gene_obj();


    ## Copy over the non-ref attribute values.
    foreach my $key (keys %$self) {
	my $value = $self->{$key};
	
	## Not copying over refs.
	if (ref $value) {
	    next;
	}
	
	## Not copying over attributes of length > 200, such as protein/nucleotide sequences
	my $length = length($value);
	if ($length > 200) { next;}
	
	# passed tests above, copying attribute.
	$clone->{$key} = $value;
	
    }
    
    ## copy over the gene synonyms.
    my @gene_syns = @{$self->{gene_synonyms}};
    $clone->{gene_synonyms} = \@gene_syns;
    
    
    ## copy the GO assignments:
    my @GO_assignments = $self->get_gene_ontology_objs();
    if (@GO_assignments) {
	foreach my $go_assignment (@GO_assignments) {
	    my $go_clone = dclone($go_assignment);
	    $clone->add_gene_ontology_objs($go_clone);
	}
    }
    

    ## copy gene structure.
    my @exons = $self->get_exons();
    foreach my $exon (@exons) {
	$clone->add_mRNA_exon_obj($exon->clone_exon());
    }
    
    foreach my $isoform ($self->get_additional_isoforms()) {
	my $isoform_clone = $isoform->clone_gene();
	$clone->add_isoform($isoform_clone);
    }
    
    $clone->refine_gene_object();

    return ($clone);
}




=over 4

=item deep_clone()

B<Description:> Provides a deep clone of a gene_obj.  Only references supported in Gene_obj documentation are supported.  Those added in a rogue way are undef()d

B<Parameters:> none

B<Returns:> $gene_obj

uses the Storable dclone() function to deep clone the Gene_obj

=back

=cut


## all objects are cloned.  References to data only are not.
sub deep_clone {
    my $self = shift;
    my $clone = dclone($self);
    
    my %supported_refs = (model_span => 1,
			  gene_span => 1,
			  gene_synonyms => 1,
			  Gene_ontology => 1,
			  additional_isoforms=>1,
			  mRNA_exon_objs => 1);
    
    foreach my $gene_obj ($clone, $clone->get_additional_isoforms()) {
	
	my @keys = keys %$gene_obj;
	foreach my $key (@keys) {
	    my $value = $gene_obj->{$key};
	    if (ref $value && !$supported_refs{$key}) {
		$gene_obj->{$key} = undef;
	    }
	}
    }
    
    return ($clone);
}


=over 4

=item populate_gene_obj()

B<Description:> Given CDS and mRNA coordinates stored in hash form, a gene object is populated with mRNA and CDS exons.  This is one available way to populate a newly instantiated Gene_obj.

B<Parameters:> $cds_hash_ref, $mRNA_hash_ref, <$seq_ref>

$mRNA_hash_ref is a reference to a hash holding the end5 => end3 coordinates of the Exons

$cds_hash_ref same as mRNA_has_ref except holds the CDS end5 => end3 coordinates.

$seq_ref is a reference to a string containing the genomic sequence.  This is an optional parameter.


B<Returns:> none

=back

=cut



## Do several things at once: assign CDS and mRNA coordinates, and build gene sequences.
## The \$seq_ref is optional in case you want to create the sequence types.
sub populate_gene_obj {
    my ($self, $cds_ref, $mRNA_ref, $seq_ref) = @_;
    $self->set_CDS_coords ($cds_ref);
    $self->set_mRNA_coords ($mRNA_ref);
    $self->refine_gene_object();
    if (ref $seq_ref) {
	$self->create_all_sequence_types($seq_ref);
    }
    ## reinitialize the hashrefs:
    $self->{mRNA_coords} = 0;
    $self->{CDS_coords} = 0;
    

}



=over 4

=item AAToNucleotideCoords()

B<Description:> Converts an amino acid -based coordinate to a genomic sequence -based coordinate.

B<Parameters:> $aa_coord

B<Returns:> $genomic_coord

undef is returned if the aa_coord could not be converted.


=back

=cut


sub AAToNucleotideCoords{
    my($self) = shift;
    my($aacoord) = shift;
    my($debug) = shift;
    my($PCDS_coords) = {};
    my($A2NMapping) = {};
    my($currAA) = 1; 
    my $strand = $self->{strand};
    my @exons = $self->get_exons();
    my($cds_count)=0;
    my($translated_bp)=-1;
    my($lastcarryover)=0; 
    my($end_bp);
    foreach my $exon (sort {
	if($strand eq "+"){
	    $a->{end5}<=>$b->{end5};
	}
	else{
	    $b->{end5}<=>$a->{end5};
	}
    } @exons) {
	my $cds = $exon->get_CDS_obj();
	if ($cds) {
	    my @cds_coords = $cds->get_CDS_end5_end3();
	    my($bpspread) = abs($cds_coords[0]-$cds_coords[1]);
	    $bpspread+=$lastcarryover;
	    my($nextAA) = int($bpspread/3); # last complete AA in CDS
	    $lastcarryover = $bpspread%3;
	    $PCDS_coords->{$currAA} = $currAA+$nextAA-1;
	    if($strand eq "+"){
		$A2NMapping->{$currAA} = $cds_coords[0]<$cds_coords[1]?$cds_coords[0]:$cds_coords[1];
	    }
	    else{
		$A2NMapping->{$currAA} = $cds_coords[0]<$cds_coords[1]?$cds_coords[1]:$cds_coords[0];
	    }
	    print "DEBUG: $strand $cds_count AA range ($currAA - $PCDS_coords->{$currAA}) nucleotide start($A2NMapping->{$currAA})\n" if($debug);
	    $currAA = $currAA+$nextAA;
	    $cds_count++;
	    if($strand eq "+"){
		$end_bp = $cds_coords[0]<$cds_coords[1]?$cds_coords[1]:$cds_coords[0];
	    }
	    else{
		$end_bp = $cds_coords[0]<$cds_coords[1]?$cds_coords[0]:$cds_coords[1];
	    }
	}
    }
    # PCDS_coords key/value are start/stop aa counts for each cds;
    # A2NMapping stores cds AA start key to cds nucleotide start
    $cds_count=0;
    foreach my $PCDS_end5 (sort {
	     $a<=>$b;
	}(keys %$PCDS_coords)) {
	my($PCDS_end3) = $PCDS_coords->{$PCDS_end5};
	    if($aacoord>=$PCDS_end5 && $aacoord<=$PCDS_end3){
		my($nucleotide_start) = $A2NMapping->{$PCDS_end5}; 
		my($aa_offset) = $aacoord - $PCDS_end5;
		my($nucleotide_offset) = $aa_offset*3;
		print "DEBUG: CDS offset $aa_offset AA $nucleotide_offset bp\n" if($debug);
		if($strand eq "+"){
		    $translated_bp = $nucleotide_start+$nucleotide_offset;
		}
		else{
		    $translated_bp = $nucleotide_start-$nucleotide_offset;
		}
		print "DEBUG: Mapping $aacoord to $translated_bp in cds $cds_count\n" if($debug);
		print "DEBUG: CDS $PCDS_end5 - $PCDS_end3 nucleotide start $A2NMapping->{$PCDS_end5}, nuc offset $nucleotide_offset\n" if($debug); 
	    }
	
	$cds_count++;
	}
    #}
    if($translated_bp == -1){
	$translated_bp = undef;
	print STDERR "Unable to translate AA coordinate: $aacoord. Off end. Using undef\n" if($debug);
    }
    return $translated_bp;
}



## private method, used by populate_gene_obj()
# sets CDS_coords instance member to a hash reference of CDS coordinates.   $hash{end5} = end3
sub set_CDS_coords {
    my $self = shift;
    my $hash_ref = shift;
    if (ref ($hash_ref) eq 'HASH') {
	$self->{CDS_coords} = $hash_ref;
    } else {
	print STDERR "Cannot set CDS_coords, must have hash reference\n";
    }
}




=over 4

=item get_gene_span()

B<Description:> Retrieves the coordinates which span the length of the gene along the genomic sequence.

B<Parameters:> none

B<Returns:> (end5, end3)

These coordinates represent the minimal and maximal exonic coordinates of the gene.  Orientation can be inferred by the relative values of end5 and end3.


=back

=cut


## All return gene end5, end3 ###
sub get_gene_span {
    my $self = shift;
    return (@{$self->{gene_span}});
}




## private
sub get_seq_span {
    my $self = shift;
    return ($self->get_gene_span());
}



=over 4

=item get_coords()

B<Description:> See get_gene_span()

B<Parameters:> none

B<Returns:> (end5, end3)

=back

=cut


sub get_coords {
    my $self = shift;
    return ($self->get_gene_span());
}



=over 4

=item get_model_span()

B<Description:> Retrieves the coordinates spanned by the protein-coding region of the gene along the genomic sequence.

B<Parameters:> none

B<Returns:> (end5, end3)

These coordinates are determined by the min and max of the CDS components of the gene.

=back

=cut




sub get_model_span {
    my $self = shift;
    return (@{$self->{model_span}});
}  






#private
# sets mRNA_coords instance member to a hash reference of CDS coordinates.   $hash{end5} = end3
sub set_mRNA_coords {
    my $self = shift;
    my $hash_ref = shift;
    if (ref ($hash_ref) eq 'HASH') {
	$self->{mRNA_coords} = $hash_ref;
    } else {
	print STDERR "Cannot set CDS_coords, must have hash reference\n";
    }
}







=over 4

=item refine_gene_object()

B<Description:> This method performs some data management operations and should be called at any time modifications have been made to the gene structure (ie. exons added or modified, model isoforms added, etc).  It performs the following orientations:

    -Sets (or resets)  gene span and model span coordinates, strand orientation, gene length, mid-point.

B<Parameters:> none

B<Returns:> none

=back

=cut

## Once mRNA_coords and CDS_coords have been assigned, this will populate the remaining elements in the gene object.

sub refine_gene_object {
    my ($self) = shift;
    #check to see if mRNA_coords field is populated.  If not, initialize.
    if ($self->{mRNA_coords} == 0) {
	$self->{mRNA_coords} = {};
    }
    my ($CDS_coords, $mRNA_coords) = ($self->{CDS_coords},  $self->{mRNA_coords});
    
    unless ($CDS_coords && $mRNA_coords) {
	#maybe created exon objects already
	if ($self->{mRNA_exon_objs}) {
	    $self->trivial_refinement();
	}
	return;
    }
    # intialize mRNA_exon_objs to array ref.
    $self->{mRNA_exon_objs} = [];
    #retrieve coordinate data.
    my %mRNA = %$mRNA_coords;
    my %CDS = %$CDS_coords;
    my @mRNAcoords = keys %mRNA;
    my @CDScoords = keys %CDS;
    my (%new_mRNA, %new_CDS);
    ## if correlation between mRNA exons and CDS exons, then map CDS's to mRNA's, otherwise, replicate CDSs as mRNAs
    if ($#mRNAcoords >= $#CDScoords) {

	foreach my $mRNA_end5 (keys %mRNA) {
	    my $mRNA_end3 = $mRNA{$mRNA_end5};
	    #find overlapping cds exon to mRNA exon
	    #easy to compare if in same orientation for all comparisons
	    my ($m1, $m2) = ($mRNA_end5 < $mRNA_end3) ? ($mRNA_end5, $mRNA_end3) : ($mRNA_end3, $mRNA_end5);
	    #create mRNA_exon_obj
	    my $mRNA_exon_obj = mRNA_exon_obj->new ($mRNA_end5, $mRNA_end3);
	    $new_mRNA{$mRNA_end5} = $mRNA_end3;
	    foreach my $CDS_end5 (keys %CDS) {
		my $CDS_end3 = $CDS{$CDS_end5};
		my ($c1, $c2) = ($CDS_end5 < $CDS_end3) ? ($CDS_end5, $CDS_end3) : ($CDS_end3, $CDS_end5);
		## do overlap comparison; CDS must be contained within mRNA exon
		if ( ($c1 >= $m1) && ($c2 <= $m2)) {
		    # found the contained CDS
		    $mRNA_exon_obj->{CDS_exon_obj} = CDS_exon_obj->new ($CDS_end5, $CDS_end3); 
		    $self->add_mRNA_exon_obj($mRNA_exon_obj);
		    $new_CDS{$CDS_end5} = $CDS_end3;
		    last;
		}
	    }
	}
    } else { # remap CDSs to mRNAS
	foreach my $CDS_end5 (keys %CDS) {
	    my $CDS_end3 = $CDS{$CDS_end5};
	    my $mRNA_exon_obj = mRNA_exon_obj->new ($CDS_end5, $CDS_end3);
	    $mRNA_exon_obj->{CDS_exon_obj} = CDS_exon_obj->new ($CDS_end5, $CDS_end3); 
	    $self->add_mRNA_exon_obj($mRNA_exon_obj);
	    $new_mRNA{$CDS_end5} = $CDS_end3;
	    $new_CDS{$CDS_end5} = $CDS_end3;
	}
    } 
    
    $self->trivial_refinement();
   
}





=over 4

=item get_exons()

B<Description:>Retrieves a list of exons belonging to this Gene_obj 

B<Parameters:> none

B<Returns:> @exons

@exons is an ordered list of mRNA_exon_obj; the first exon of the list corresponds to the first exon of the spliced gene.

=back

=cut

sub get_exons {
    my ($self) = shift;
    if ($self->{mRNA_exon_objs} != 0) {
	my @exons = (@{$self->{mRNA_exon_objs}});
	@exons = sort {$a->{end5}<=>$b->{end5}} @exons;
	if ($self->{strand} eq '-') {
	    @exons = reverse (@exons);
	}
	return (@exons);
    } else {
	my @x = ();
	return (@x); #empty array 
    }
}


## private
sub get_segments {
    my $self = shift;
    return ($self->get_exons());
}



=over 4

=item number_of_exons()

B<Description:> Provides the number of exons contained by the Gene

B<Parameters:> none

B<Returns:> int

=back

=cut



sub number_of_exons {
    my $self = shift;
    my $exon_number = $#{$self->{mRNA_exon_objs}} + 1;
    return ($exon_number);
}







=over 4

=item get_intron_coordinates()

B<Description:> Provides an ordered list of intron coordinates

B<Parameters:> none

B<Returns:> ( [end5,end3], ....) 

A list of arrayRefs are returned providing the coordinates of introns, ordered from first intron to last intron within the gene.

=back

=cut


sub get_intron_coordinates {
    my $gene_obj = shift;
    my $strand = $gene_obj->get_orientation();
    my @exons = $gene_obj->get_exons();
    ## exon list should already be sorted.
    my @introns = ();
    
    my $num_exons = $#exons + 1;
    if ($num_exons > 1) { #only genes with multiple exons will have introns.
	if ($strand eq '+') {
	    my $first_exon = shift @exons;
	    while (@exons) {
		my $next_exon = shift @exons;
		my ($first_end5, $first_end3) = $first_exon->get_coords();
		my ($next_end5, $next_end3) = $next_exon->get_coords();
		my $intron_end5 = $first_end3 + 1;
		my $intron_end3 = $next_end5 -1;
		if ($intron_end5 < $intron_end3) {
		    push (@introns, [$intron_end5, $intron_end3]);
		}
		$first_exon = $next_exon;
	    }
	} elsif ($strand eq '-') {
	    my $first_exon = shift @exons;
	    while (@exons) {
		my $next_exon = shift @exons;
		my ($first_end5, $first_end3) = $first_exon->get_coords();
		my ($next_end5, $next_end3) = $next_exon->get_coords();
		my $intron_end5 = $first_end3 - 1;
		my $intron_end3 = $next_end5 +1;
		if ($intron_end5 > $intron_end3) {
		    push (@introns, [$intron_end5, $intron_end3]);
		}
		$first_exon = $next_exon;
	    }
	    
	} else {
	    die "Strand for gene_obj is not specified." . $gene_obj->toString();
	}
    }
    return (@introns);
}





#private
sub trivial_refinement {
    my $self = shift;
    my @exons = $self->get_exons();
    my (%mRNAexons, %CDSexons);
    foreach my $exon (@exons) {
	my ($exon_end5, $exon_end3) = $exon->get_mRNA_exon_end5_end3();
	$mRNAexons{$exon_end5} = $exon_end3;
	my $cds;
	if ($cds = $exon->get_CDS_obj()) {
	    my ($cds_end5, $cds_end3) = $cds->get_CDS_end5_end3();
	    $CDSexons{$cds_end5} = $cds_end3;
	}
    }
    my @mRNAexonsEnd5s = sort {$a<=>$b} keys %mRNAexons;
    my @CDSexonsEnd5s = sort {$a<=>$b} keys %CDSexons;
    my $strand = 0; #initialize.
    foreach my $mRNAend5 (@mRNAexonsEnd5s) {
	my $mRNAend3 = $mRNAexons{$mRNAend5};
	if ($mRNAend5 == $mRNAend3) {next;}
	$strand = ($mRNAend5 < $mRNAend3) ? '+':'-';
	last;
    }
    $self->{strand} = $strand;
    my ($gene_end5, $gene_end3, $model_end5, $model_end3);
    if ($strand eq '+') {
	($gene_end5, $gene_end3)  = ($mRNAexonsEnd5s[0], $mRNAexons{$mRNAexonsEnd5s[$#mRNAexonsEnd5s]});
	($model_end5, $model_end3) = ($CDSexonsEnd5s[0], $CDSexons{$CDSexonsEnd5s[$#CDSexonsEnd5s]});
    } else {
	($gene_end5, $gene_end3)  = ($mRNAexonsEnd5s[$#mRNAexonsEnd5s], $mRNAexons{$mRNAexonsEnd5s[0]});
	($model_end5, $model_end3) = ($CDSexonsEnd5s[$#CDSexonsEnd5s], $CDSexons{$CDSexonsEnd5s[0]}); 
    }

    $self->{gene_span} = [$gene_end5, $gene_end3];
    $self->{gene_length} = abs ($gene_end3 - $gene_end5) + 1;
    $self->{mid_pt} = int (($gene_end5 + $gene_end3)/2);
    $self->{model_span} = [$model_end5, $model_end3]; 
             
    ## Refine isoforms if they exist.
    if (my @isoforms = $self->get_additional_isoforms()) {
	my @gene_span_coords = $self->get_gene_span();
	foreach my $isoform (@isoforms) {
	    $isoform->refine_gene_object();
	    push (@gene_span_coords, $isoform->get_gene_span());
	}
	@gene_span_coords = sort {$a<=>$b} @gene_span_coords;
	my $lend = shift @gene_span_coords;
	my $rend = pop @gene_span_coords;
	my $strand = $self->{strand};
	if ($strand eq '-') {
	    ($lend, $rend) = ($rend, $lend);
	}
	my $gene_length = abs ($lend -$rend) + 1;
	foreach my $gene ($self, @isoforms) {
	    $gene->{gene_span} = [$lend, $rend];
	    $gene->{gene_length} = $gene_length;
	}
    }
      
}




=over 4

=item add_mRNA_exon_obj()

B<Description:> Used to add a single mRNA_exon_obj to the Gene_obj 

B<Parameters:> mRNA_exon_obj

B<Returns:> none

=back

=cut

sub add_mRNA_exon_obj {
    my ($self) = shift;
    my ($mRNA_exon_obj) = shift;
    if (!ref($self->{mRNA_exon_objs})) {
	$self->{mRNA_exon_objs} = [];
    } 
    my $index = $#{$self->{mRNA_exon_objs}};
    $index++;
    $self->{mRNA_exon_objs}->[$index] = $mRNA_exon_obj;
}

#private
## forcibly set protein sequence value
sub set_protein_sequence {
    my $self = shift;
    my $protein = shift;
    if ($protein) {
	$self->{protein_seq} = $protein;
	$self->{protein_seq_length} = length ($protein);
    } else {
	print STDERR "No incoming protein sequence to set to.\n" . $self->toString();
    }
}

#private
## forcibly set CDS sequence value
sub set_CDS_sequence {
    my $self = shift;
    my $cds_seq = shift;
    if ($cds_seq) {
	$self->{CDS_sequence} = $cds_seq;
	$self->{CDS_sequence_length} = length ($cds_seq);
    } else {
	print STDERR "No incoming CDS sequence to set to\n" . $self->toString();
    }
}

#private
sub set_cDNA_sequence {
    my $self = shift;
    my $cDNA_seq = shift;
    if ($cDNA_seq) {
	$self->{cDNA_sequence} = $cDNA_seq;
	$self->{cDNA_sequence_length} = length($cDNA_seq);
    } else {
	print STDERR "No incoming cDNA sequence to set to.\n" . $self->toString();
    }
}

#private
sub set_gene_sequence {
    my $self = shift;
    my $seq = shift;
    if ($seq) {
	$self->{gene_sequence} = $seq;
	$self->{gene_sequence_length} = length ($seq);
    } else {
	print STDERR "No incoming gene sequence to set to\n" . $self->toString();
    }
}


=over 4

=item create_all_sequence_types()

B<Description:> Given a scalar reference to the genomic sequence, the CDS, cDNA, unspliced transcript and protein sequences are constructed and populated within the Gene_obj

B<Parameters:> $genomic_seq_ref, [%params]

B<Returns:> 0|1

returns 1 upon success, 0 upon failure

By default, the protein and CDS sequence are populated.  If you want the unspliced genomic sequence, you need to specify this in the attributes:

    %params = ( potein => 1,
		CDS => 1,
		cDNA => 1,
		unspliced_transcript => 0)


=back

=cut


## Create all gene sequences (protein, cds, cdna, genomic)
sub create_all_sequence_types {
    my $self = shift;
    my $big_seq_ref = shift;
    my %atts = @_;
    
    unless (ref($big_seq_ref) eq 'SCALAR') {
	print STDERR "I require a sequence reference to create sequence types\n";
	return (undef());
    }
    $self->create_cDNA_sequence($big_seq_ref) unless (exists($atts{cDNA}) && $atts{cDNA} == 0);
    $self->create_CDS_sequence ($big_seq_ref) unless (exists ($atts{CDS}) && $atts{CDS} == 0);
    $self->create_protein_sequence($big_seq_ref) unless (exists ($atts{protein}) && $atts{protein} == 0);
    $self->create_gene_sequence($big_seq_ref, 1) if ($atts{unspliced_transcript}==1); #highlight exons by default.
    
    if (my @isoforms = $self->get_additional_isoforms()) {
	foreach my $isoform (@isoforms) {
	    $isoform->create_all_sequence_types($big_seq_ref, %atts);
	}
    }
    return(1);
}

#private
## Create cDNA sequence
sub create_cDNA_sequence {
    my $self = shift;
    my $seq_ref = shift;
    my $sequence_ref;
    unless ($seq_ref) {
	print STDERR "The parent sequence must be specified for the cDNA creation method\n";
	return;
    }
    ## hopefully the sequence came in as a reference.  If not, make one to it.
    ## Don't want to pass chromosome sequences in by value!
    if (ref($seq_ref)) {
	$sequence_ref = $seq_ref;
    } else {
	$sequence_ref = \$seq_ref;
    }
    my @exons = $self->get_exons();
    my $strand = $self->{strand};
    my $cDNA_seq = "";
    foreach my $exon_obj (sort {$a->{end5}<=>$b->{end5}} @exons) {
	my $c1 = $exon_obj->{end5};
	my $c2 = $exon_obj->{end3};
	## sequence retrieval coordinates must be in forward orientation
	my ($coord1, $coord2) = ($strand eq '+') ? ($c1, $c2) : ($c2, $c1);
	$cDNA_seq .= substr ($$sequence_ref, ($coord1 - 1), ($coord2 - $coord1 + 1));
    }
    if ($strand eq '-') {
	$cDNA_seq = &reverse_complement($cDNA_seq);
    }
    $self->set_cDNA_sequence($cDNA_seq);
    return ($cDNA_seq);
}

#private
## create a CDS sequence, and populate the protein field.
sub create_CDS_sequence {
    my $self = shift;
    my $seq_ref = shift;
    my $sequence_ref;
    unless ($seq_ref) {
	print STDERR "The parent sequence must be specified for the CDS creation method\n";
	return;
    }
    ## hopefully the sequence came in as a reference.  If not, make one to it.
    ## Don't want to pass chromosome sequences in by value!
    if (ref($seq_ref)) {
	$sequence_ref = $seq_ref;
    } else {
	$sequence_ref = \$seq_ref;
    }
    my @exons = $self->get_exons();
    my $strand = $self->{strand};
    my $cds_seq = "";
    foreach my $exon_obj (sort {$a->{end5}<=>$b->{end5}} @exons) {
	my $CDS_obj = $exon_obj->get_CDS_obj();
	if (ref $CDS_obj) {
	    my ($c1, $c2) = $CDS_obj->get_CDS_end5_end3();
	    ## sequence retrieval coordinates must be in forward orientation
	    my ($coord1, $coord2) = ($strand eq '+') ? ($c1, $c2) : ($c2, $c1);
	    $cds_seq .= substr ($$sequence_ref, ($coord1 - 1), ($coord2 - $coord1 + 1));
	}
    }
    if ($strand eq '-') {
	$cds_seq = &reverse_complement($cds_seq);
    }
    $self->set_CDS_sequence($cds_seq);
    ## create protein sequence
    $self->create_protein_sequence();
    return ($cds_seq);
}


#private
## Translation requires parent nucleotide sequence (bac, chromosome, whatever).
sub create_protein_sequence {
    my $self = shift;
    my ($cds_sequence);
    ## if have cds-sequence, then just use that.
    $cds_sequence = $self->get_CDS_sequence();
    unless ($cds_sequence) {
	my $seq_ref = shift;
	unless (ref($seq_ref) eq 'SCALAR') {
	    print STDERR "I require an assembly sequence ref if the CDS is unavailable\n";
	    return;
	}
	$cds_sequence = $self->create_CDS_sequence($seq_ref);
    }
    my $protein = &Nuc_translator::get_protein ($cds_sequence); 
    $self->set_protein_sequence($protein);
    return ($protein);
}

#private
## Create the unspliced nucleotide transcript
sub create_gene_sequence {
    my $self = shift;
    my $big_seq_ref = shift;
    my $highlight_exons_flag = shift; #upcases exons, lowcases introns.
    unless (ref ($big_seq_ref) eq 'SCALAR') {
	print STDERR "I require a reference to the assembly sequence!!\n";
	return (undef());
    }
    my $strand = $self->{strand};
    my ($gene_seq);
    if ($highlight_exons_flag) {
	my @exons = sort {$a->{end5}<=>$b->{end5}} $self->get_exons();
	my $exon = shift @exons;
	my ($lend, $rend) = sort {$a<=>$b} $exon->get_coords();
	$gene_seq = uc (substr ($$big_seq_ref, $lend - 1, $rend - $lend + 1));
	my $prev_rend = $rend;
	while (@exons) {
	    $exon = shift @exons;
	    ## Add intron, then exon
	    my ($lend, $rend) = sort {$a<=>$b} $exon->get_coords();
	    $gene_seq .= lc (substr ($$big_seq_ref, $prev_rend, $lend - $prev_rend-1));
	    $gene_seq .= uc (substr ($$big_seq_ref, $lend - 1, $rend - $lend + 1));
	    $prev_rend = $rend;
	}

    } else { #just get the sequence spanned by min and max coords
	my ($coord1, $coord2) = sort {$a<=>$b} $self->get_gene_span();
	$gene_seq = substr ($$big_seq_ref, ($coord1 - 1), ($coord2 - $coord1 + 1));
    }

    $gene_seq = &reverse_complement($gene_seq) if ($strand eq '-');
    $self->set_gene_sequence($gene_seq);
    return ($gene_seq);
}
	
## retrieving the sequences

=over 4

=item get_protein_sequence()

B<Description:> Retrieves the protein sequence

B<Parameters:> none

B<Returns:> $protein

Note: You must have called create_all_sequence_types($genomic_ref) before protein sequence is available for retrieval.


=back

=cut

sub get_protein_sequence {
    my $self = shift;
    return ($self->{protein_seq});
}


=over 4

=item get_CDS_sequence()

B<Description:> Retrieves the CDS sequence.  The CDS sequence is the protein-coding nucleotide sequence.

B<Parameters:> none

B<Returns:> $cds

Note: You must have called create_all_sequence_types($genomic_ref) before protein sequence is available for retrieval.

=back

=cut


sub get_CDS_sequence {
    my $self = shift;
    return ($self->{CDS_sequence});
}

=over 4

=item get_cDNA_sequence()

B<Description:> Retrieves the tentative cDNA sequence for the Gene.  The cDNA includes the CDS with potential UTR extensions.

B<Parameters:> none

B<Returns:> $cdna

Note: You must have called create_all_sequence_types($genomic_ref) before protein sequence is available for retrieval.


=back

=cut



sub get_cDNA_sequence {
    my $self = shift;
    return ($self->{cDNA_sequence});
}



=over 4

=item get_gene_sequence()

B<Description:> Retrieves the unspliced transcript of the gene.

B<Parameters:> none

B<Returns:> $unspliced_transcript

=back

=cut


sub get_gene_sequence {
    my $self = shift;
    return ($self->{gene_sequence});
}




=over 4

=item get_gene_synonyms()

B<Description:> Retrieves the Model_feat_name(s) for the synonomous gene models found on other BACs or contigs.

B<Parameters:> none

B<Returns:> @model_feat_names


For Arabidopsis, gene models are found within overlapping regions of BAC sequences, in which the gene models are annotated on both corresponding BACs.  Given a Gene_obj for a model on one BAC, the synomous gene on the overlapping BAC can be identified via this method.


=back

=cut


sub get_gene_synonyms {
    my $self = shift;
    return (@{$self->{gene_synonyms}});
}



=over 4

=item clear_sequence_info()

B<Description:> Clears the sequence fields stored within a Gene_obj, including the CDS, cDNA, gene_sequence, and protein sequence.  Often, these sequence fields, when populated, can consume large amounts of memory in comparison to the coordinate and functional annotation data.  This method is useful to clear this memory when the sequences are not needed.  The create_all_sequence_types($genomic_seq_ref) can be called again later to repopulate these sequences when they are needed.

B<Parameters:> none

B<Returns:> none

=back

=cut


## sequences consume huge amounts of memory in comparison to other gene features.
## want to clear them from time to time to save memory.
sub clear_sequence_info {
    my $self = shift;
    $self->{ protein_seq => 0,   
	     CDS_sequence => 0,
	     cDNA_sequence => 0, 
	     gene_sequence => 0};
}



=over 4

=item set_gene_type()

B<Description:> Sets the type of gene.  Expected types include: 

    protein-coding #default setting
    rRNA
    snoRNA
    snRNA
    tRNA
    
    ...or others as needed.  Nothing is restricted.

B<Parameters:> $type

B<Returns:> none

=back

=cut


####
sub set_gene_type {
    my ($self) = shift;
    my ($gene_type) = shift;
    $self->{gene_type} = $gene_type;
}


=over 4

=item adjust_gene_coordinates()

B<Description:> Used to add or subtract a specified number of bases from each gene component coordinate.

B<Parameters:> $adj_amount

$adj_amoount is a positive or negative integer.

B<Returns:> none

=back

=cut


####
# add value to all gene component coordinates
sub adjust_gene_coordinates {
    my $self = shift;
    my $adj_amount = shift;
    my @exons = $self->get_exons();
    foreach my $exon (@exons) {
	my ($end5, $end3) = $exon->get_coords();
	$exon->set_coords($end5 + $adj_amount, $end3 + $adj_amount);
	my $cds = $exon->get_CDS_obj();
	if (ref $cds) {
	    my ($end5, $end3) = $cds->get_coords();
	    $cds->set_coords($end5 + $adj_amount, $end3 + $adj_amount);
	}
    }

    ## don't forget about alt splicing isoforms!
    my @isoforms = $self->get_additional_isoforms();
    foreach my $isoform (@isoforms) {
	$isoform->adjust_gene_coordinates($adj_amount);
    }
    $self->refine_gene_object();
}




=over 4

=item toString()

B<Description:> Textually describes the Gene_obj including coordinates and attributes.

B<Parameters:> <%attributes_list> 

%attributes_list is optional and can control whether certain attributes are included in the textual output

Default settings are:

    %attributes_list = ( 
			 -showIsoforms => 1,  #set to 0 to avoid isoform info to the text output.
			 -showSeqs => 0  #set to 1 for avoiding protein, cds, genomic, cdna seqs as output.
			 )

B<Returns:> $text

=back

=cut




## retrieve text output describing the gene.
sub toString {
    my $self = shift;
    my %atts = @_;
    # atts defaults:
    #       -showIsoforms=>1
    #       -showSeqs => 0
    
    my $output = "";
    foreach my $key (keys %$self) {
	my $value = $self->{$key};
	if (ref $value) {
	    if ($key =~ /secondary/ && ref $value eq "ARRAY") {
		foreach my $val (@$value) {
		    $output .= "\t\t$key\t$val\n";
		}
	    }
	    
	    
	} else {
	    if ($self->{is_pseudogene} && $key =~ /cds|cdna|protein/i && $key =~ /seq/) {
		next;
	    }
	    if ((!$atts{-showSeqs}) && $key =~/seq/) { next; }
	    if ( ($value eq '0' || !defined($value)) && $key !~/^is_/) { next;} #dont print unpopulated info.
	    $output .= "\t$key:\t$value\n";
	}
    }
    $output .= "\tgene_synonyms: @{$self->{gene_synonyms}}\n";
        
    $output .=  "\tmRNA_coords\t";  

    if (ref ($self->{mRNA_coords}) eq "HASH") {
	foreach my $end5 (sort {$a<=>$b} keys %{$self->{mRNA_coords}}) {
	    $output .=  "$end5-$self->{mRNA_coords}->{$end5} ";
	}
    }
    $output .= "\n"
            . "\tCDS_coords\t";
    if (ref ($self->{CDS_coords}) eq "HASH") {
	foreach my $end5 (sort {$a<=>$b} keys %{$self->{CDS_coords}}) {
	    $output .= "$end5-$self->{CDS_coords}->{$end5} ";
	}
    }
    
    my @exons = $self->get_exons();
    foreach my $exon (@exons) {
	$output .=  "\n\t\tRNA-exon: $exon->{end5}, $exon->{end3}\t";
	my $cds = $exon->{CDS_exon_obj};
	if ($cds) {
	    $output .= "CDS-exon: $cds->{end5}, $cds->{end3}";
	}
    }

    if (ref $self->{gene_span}) {
	my ($gene_end5, $gene_end3) = @{$self->{gene_span}};
	$output .= "\n\tgene_span: $gene_end5-$gene_end3";
    }
    if (ref $self->{model_span}) {
	my ($model_end5, $model_end3) = @{$self->{model_span}};
	$output .= "\n\tmodel_span: $model_end5-$model_end3";
    }
    my @gene_ontology_objs = $self->get_gene_ontology_objs();
    if (@gene_ontology_objs) {
	$output .= "\n\tGene Ontology Assignments:\n";
	foreach my $go_assignment (@gene_ontology_objs) {
	    $output .= "\t" . $go_assignment->toString();
	}
    }
    
    unless (defined ($atts{-showIsoforms}) && $atts{-showIsoforms} == 0) {
	foreach my $isoform ($self->get_additional_isoforms()) {
	    $output .= "\n\n\tISOFORM:\n" . $isoform->toString();
	}
    }
    $output .= "\n\n"; #spacer at terminus
    return ($output);
}


####
## Splice site validation section
####

=over 4

=item validate_splice_sites()

B<Description:> Validates the presence of consensus splice sites 

B<Parameters:> $genomic_seq_ref

$genomic_seq_ref is a scalar reference to the string containing the genomic sequence.

B<Returns:> $errors

If the empty string ("") is returned, then no inconsistencies were identified.

=back

=cut

    
####
sub validate_splice_sites {
    my $self = shift;
    my $asmbl_seq_ref = shift;
    unless (ref ($asmbl_seq_ref)) {
	print STDERR "I require a sequence reference\n";
	return (undef());
    }
    my $error_string = "";
    my $strand = $self->{strand};
    my @exons = $self->get_exons();
    my $num_exons = $#exons + 1;
    if ($num_exons == 1) {
	#no splice sites to confirm.
	return ("");
    }
    for (my $i = 1; $i <= $num_exons; $i++) {
	my $exon_type;
	if ($i == 1) { 
	    $exon_type = "initial";
	} elsif ($i == $num_exons) {
	    $exon_type = "terminal";
	} else {
	    $exon_type = "internal";
	}
	my $exon = $exons[$i - 1]; 
	my ($exon_end5, $exon_end3) = $exon->get_mRNA_exon_end5_end3();
	my ($coord1, $coord2) = sort {$a<=>$b} ($exon_end5, $exon_end3);
	## get two coordinate sets corresponding to potential splice sites
	my $splice_1_start = $coord1-2-1;
	my $splice_2_start = $coord2-1+1;
	#print "confirming splice sites at "  . ($splice_1_start +1) . " and " . ($splice_2_start + 1) . "\n"if $SEE;
	my $splice_1 = substr ($$asmbl_seq_ref, $splice_1_start, 2);
	my $splice_2 = substr ($$asmbl_seq_ref, $splice_2_start, 2);
	my ($acceptor, $donor) = ($strand eq '+') ? ($splice_1, $splice_2) : (&reverse_complement($splice_2), &reverse_complement($splice_1)); 
	my $check_acceptor = ($acceptor =~ /ag/i);
	my $check_donor = ($donor =~ /gt|gc/i);
	## associate results of checks with exon type.
	if ($exon_type eq "initial" || $exon_type eq "internal") {
	    unless ($check_donor) {
		$error_string .= "non-consensus $donor donor splice site at $coord1\n";
	    }
	}
	
	if ($exon_type eq "internal" || $exon_type eq "terminal") {
	    unless ($check_acceptor) {
		$error_string .=  "\tnon-consensus $acceptor acceptor splice site at $coord2\n";
	    }
	}
    }
    return ($error_string);
}



=over 4

=item get_annot_text()

B<Description:> Provides basic functional annotation for a Gene_obj 

B<Parameters:> none

B<Returns:> $string

$string includes locus, pub_locus, com_name, and pub_comment

=back

=cut


####
sub get_annot_text {
    my $self = shift;
    my $locus = $self->{locus};
    my $pub_locus = $self->{pub_locus};
    my $com_name = $self->{com_name};
    my $pub_comment = $self->{pub_comment};
    my $text = "";
    foreach my $token ($locus, $pub_locus, $com_name, $pub_comment) {
	if ($token) {
	    $text .= "$token ";
	}
    }
    return ($text);
}



=over 4

=item add_isoform()

B<Description:> Adds a Gene_obj to an existing Gene_obj as an alternative splicing variant.

B<Parameters:> Gene_obj

B<Returns:> none

=back

=cut

sub add_isoform {
    my $self = shift;
    my @gene_objs = @_;
    foreach my $gene_obj (@gene_objs) {
	$self->{num_additional_isoforms}++;
	push (@{$self->{additional_isoforms}}, $gene_obj);
    }
}





=over 4

=item has_additional_isoforms()

B<Description:> Provides number of additional isoforms.  Typically used as a boolean.

B<Parameters:> none

B<Returns:> number of additional isoforms (int)

If no additional isoforms exist, returns 0


boolean usage:

0 = false (has no more)
nonzero = true (has additional isoforms)

=back

=cut

sub has_additional_isoforms {
    my $self = shift;
    return ($self->{num_additional_isoforms});
}



=over 4

=item delete_isoforms()

B<Description:> removes isoforms stored in this Gene_obj (assigning to a new anonymous arrayref)

B<Parameters:> Gene_obj

B<Returns:> none

=back

=cut

sub delete_isoforms {
    my $self = shift;
    $self->{num_additional_isoforms} = 0;
    $self->{additional_isoforms} = [];
}





=over 4

=item get_additional_isoforms()

B<Description:> Retrieves the additional isoforms for a given Gene_obj

B<Parameters:> none

B<Returns:> @Gene_objs

If no additional isoforms exist, an empty array is returned.

=back

=cut


sub get_additional_isoforms {
    my $self = shift;
    return (@{$self->{additional_isoforms}});
}



=over 4

=item get_orientation()

B<Description:> Retrieves the strand orientation of the Gene_obj

B<Parameters:> none

B<Returns:> +|-

=back

=cut


sub get_orientation {
    my $self = shift;
    return ($self->{strand});
}


=over 4

=item add_gene_ontology_objs()

B<Description:> Adds a list of Gene_ontology objects to a Gene_obj

B<Parameters:> @Gene_ontology_objs

@Gene_ontology_objs is a list of objects instantiated from Gene_ontology.pm

B<Returns:> none

=back

=cut


sub add_gene_ontology_objs {
    my ($self, @ontology_objs) = @_;
    push (@{$self->{GeneOntology}}, @ontology_objs);
}



=over 4

=item get_gene_ontology_objs()

B<Description:> Retrieves Gene_ontology objs assigned to the Gene_obj

B<Parameters:> none

B<Returns:> @Gene_ontology_objs

@Gene_ontology_objs are objects instantiated from package Gene_ontology  (See Gene_ontology.pm)

=back

=cut


sub get_gene_ontology_objs {
    my $self = shift;
    if (ref ($self->{GeneOntology})) {
	return (@{$self->{GeneOntology}});
    } else {
	return (());
    }
}


=over 4

=item set_5prime_partial()

B<Description:> Sets the status of the is_5prime_partial attribute

B<Parameters:> 1|0

B<Returns:> none


5prime partials are partial on their 5prime end and lack start codons.


=back

=cut

sub set_5prime_partial() {
    my $self = shift;
    my $value = shift;
    $self->{is_5prime_partial} = $value;
}



=over 4

=item set_3prime_partial()

B<Description:> Sets the is_3prime_partial status

B<Parameters:> 1|0

B<Returns:> none

3prime partials are partial on their 3prime end and lack stop codons.

=back

=cut


sub set_3prime_partial() {
    my $self = shift;
    my $value = shift;
    $self->{is_3prime_partial} = $value;
}



=over 4

=item is_5prime_partial()

B<Description:> Retrieves the 5-prime partial status of the gene.

B<Parameters:> none

B<Returns:> 1|0

=back

=cut


sub is_5prime_partial() {
    my $self = shift;
    return ($self->{is_5prime_partial});
}


=over 4

=item is_3prime_partial()

B<Description:> Retrieves the 3-prime partial status of the gene.

B<Parameters:> none

B<Returns:> 1|0

=back

=cut


sub is_3prime_partial() {
    my $self = shift;
    return ($self->{is_3prime_partial});
}


=over 4

=item trim_UTRs()

B<Description:> Trims the UTR of the Gene_obj so that the Exon coordinates are identical to the CDS coordinates.  Exons which lack CDS components and are completely UTR are removed. 

B<Parameters:> none

B<Returns:> none

=back

=cut


sub trim_UTRs {
    my $self = shift;
    
    ## adjust exon coordinates to CDS coordinates.
    ## if cds doesn't exist, rid exon:
    
    my @new_exons;
    
    my @exons = $self->get_exons();
    foreach my $exon (@exons) {
	if (my $cds = $exon->get_CDS_obj()) {
	    my ($exon_end5, $exon_end3) = $exon->get_coords();
	    my ($cds_end5, $cds_end3) = $cds->get_coords();
	    
	    if ($exon_end5 != $cds_end5 || $exon_end3 != $cds_end3) {
		$exon->set_coords($cds_end5, $cds_end3);
	    }
	    push (@new_exons, $exon);
	}
    }
    $self->{mRNA_exon_objs} = 0; #clear current gene structure
    $self->{mRNA_exon_objs} = \@new_exons; #replace gene structure
    $self->refine_gene_object(); #update
    return ($self);
}



=over 4

=item get_gene_names()

B<Description:> Retrieves gene names  (primary gene name followed by secondary gene names, ';' delimited.

B<Parameters:> none

B<Returns:> string
				       
     see $gene_obj->{gene_name}
     see $gene_obj->get_secondary_names()

secondary gene names sorted lexicographically


=back

=cut




####
sub get_gene_names {
    my $gene_obj = shift;
    my @gene_names;
    if ($gene_obj->{gene_name}) {
	push (@gene_names, $gene_obj->{gene_name});
    }
    if (my @secondary_names = $gene_obj->get_secondary_gene_names()) {
	push (@gene_names, @secondary_names);
    }
    my $ret_gene_names = join ("; ", @gene_names);
    return ($ret_gene_names);
}



=over 4

=item get_secondary_gene_names()

B<Description:> Retrieves secondary gene names as a ';' delimited string.

B<Parameters:> none

B<Returns:> string

=back

=cut


####
sub get_secondary_gene_names {
    my ($gene_obj) = @_;
    return (sort @{$gene_obj->{secondary_gene_names}});
}




=over 4

=item get_product_names()

B<Description:> Retrieves product name, with the primary product name followed by secondary product names, delimited by ';'

B<Parameters:> none

B<Returns:> string

    see $gene_obj->{com_name} for primary product name
    see $gene_obj->get_secondary_product_names()

=back

=cut


####
sub get_product_names {
    my $gene_obj = shift;
    my @product_names;
    if ($gene_obj->{com_name}) {
	push (@product_names, $gene_obj->{com_name});
    }
    if (my @secondary_names = $gene_obj->get_secondary_product_names()) {
	push (@product_names, @secondary_names);
    }
    my $ret_product_names = join ("; ", @product_names);
    return ($ret_product_names);
}



=over 4

=item get_secondary_product_names()

B<Description:> Retrieves secondary product names, delimited by ';' and sorted lexicographically.

B<Parameters:> none 

B<Returns:> string

=back

=cut


####
sub get_secondary_product_names {
    my ($gene_obj) = @_;
    return (sort @{$gene_obj->{secondary_product_names}});
}



=over 4

=item get_gene_symbols()

B<Description:> Retrieves primary gene symbol followed by secondary gene symbols, delimited by ';'

B<Parameters:> none

B<Returns:> string

    see $gene_obj->{gene_sym}
    see $gene_obj->get_secondary_gene_symbols()

=back

=cut



####
sub get_gene_symbols {
    my $gene_obj = shift;
    my @gene_symbols;
    if ($gene_obj->{gene_sym}) {
	push (@gene_symbols, $gene_obj->{gene_sym});
    }
    if (my @secondary_symbols = $gene_obj->get_secondary_gene_symbols()) {
	push (@gene_symbols, @secondary_symbols);
    }
    my $ret_gene_symbols = join ("; ", @gene_symbols);
    return ($ret_gene_symbols);
}


=over 4

=item get_secondary_gene_symbols()

B<Description:> Retrieves secondary gene symbols, delimited by ';' and sorted lexicographically

B<Parameters:> none

B<Returns:> string

=back

=cut


####
sub get_secondary_gene_symbols {
    my ($gene_obj) = @_;
    return (sort @{$gene_obj->{secondary_gene_symbols}});
}



=over 4

=item get_ec_numbers()

B<Description:> Retrieves primary EC number followed by secondary EC numbers, ';' delimited

B<Parameters:> none

B<Returns:> string

    see $gene_obj->{ec_num}
    see $gene_obj->get_secondary_ec_numbers()
    
=back

=cut



####
sub get_ec_numbers {
    my $gene_obj = shift;
    my @ec_numbers;
    if ($gene_obj->{ec_num}) {
	push (@ec_numbers, $gene_obj->{ec_num});
    }
    if (my @secondary_ec_numbers = $gene_obj->get_secondary_ec_numbers()) {
	push (@ec_numbers, @secondary_ec_numbers);
    }
    my $ret_ec_numbers = join ("; ", @ec_numbers);
    return ($ret_ec_numbers);
}



=over 4

=item get_secondary_ec_numbers()

B<Description:> Retrieves secondary EC numbers, ';' delimited and sorted lexicographically

B<Parameters:> none

B<Returns:> string


=back

=cut


####
sub get_secondary_ec_numbers {
    my ($gene_obj) = @_;
    return (sort @{$gene_obj->{secondary_ec_numbers}});
}



=over 4

=item add_secondary_gene_names()

B<Description:> Adds secondary gene name(s) 

B<Parameters:> (gene_name_1, gene_name_2, ....)

Single gene name or list of gene names is allowed


B<Returns:> none

=back

=cut



####
sub add_secondary_gene_names {
    my ($gene_obj, @gene_names) = @_;
    push (@{$gene_obj->{secondary_gene_names}}, @gene_names);
}


=over 4

=item add_secondary_product_names()

B<Description:> Adds secondary product names

B<Parameters:> (product_name_1, product_name_2, ...)

Single or list of product names as parameter

B<Returns:> none

Primary gene name added directly as an attribute like so
    $gene_obj->{gene_name} = name

=back

=cut


####
sub add_secondary_product_names {
    my ($gene_obj, @product_names) = @_;
    &trim_leading_trailing_ws(\@product_names);
    push (@{$gene_obj->{secondary_product_names}}, @product_names);
}


=over 4

=item add_secondary_gene_symbols()

B<Description:> Add secondary gene symbols

B<Parameters:> (gene_symbol_1, gene_symbol_2, ...)

String or list context

B<Returns:> none

Primary gene_symbol added directly as attribute like so:
    $gene_obj->{gene_sym} = symbol

=back

=cut


####
sub add_secondary_gene_symbols {
    my ($gene_obj, @gene_symbols) = @_;
    &trim_leading_trailing_ws(\@gene_symbols);
    push (@{$gene_obj->{secondary_gene_symbols}}, @gene_symbols);
}





=over 4

=item add_secondary_ec_numbers()

B<Description:> Add secondary Enzyme Commission (EC) numbers

B<Parameters:> (EC_1, EC_2, ...)

String or list context

B<Returns:> none


Primary EC number added directly as an attribute like so:
    $gene_obj->{ec_num} = EC_number

=back

=cut


####
sub add_secondary_ec_numbers {
    my ($gene_obj, @ec_numbers) = @_;
    &trim_leading_trailing_ws(\@ec_numbers);
    push (@{$gene_obj->{secondary_ec_numbers}}, @ec_numbers);
}


## Private, remove leading and trailing whitespace characters:
sub trim_leading_trailing_ws {
    my ($ref) = @_;
    if (ref $ref eq "SCALAR") {
	$$ref =~ s/^\s+|\s+$//g;
    } elsif (ref $ref eq "ARRAY") {
	foreach my $element (@$ref) {
	    $element =~ s/^\s+|\s+$//g;
	}
    } else {
	my $type = ref $ref;
	die "Currently don't support trim_leading_trailing_ws(ref type: $type)\n";
    }
}


######################################################################################################################################
######################################################################################################################################


=head1 NAME

package mRNA_exon_obj

=cut

=head1 DESCRIPTION

    The mRNA_exon_obj represents an individual spliced mRNA exon of a gene.  The coordinates of the exon can be manipulated, and the mRNA_exon_obj can contain a single CDS_exon_obj.  A mRNA_exon_obj lacking a CDS_exon_obj component is an untranslated (UTR) exon.

    A mature Gene_obj is expected to have at least one mRNA_exon_obj component.

=cut


package mRNA_exon_obj;

use strict;
use Storable qw (store retrieve freeze thaw dclone);

=over 4

=item new()

B<Description:> Instantiates an mRNA_exon_obj

B<Parameters:> <(end5, end3)>

The end5 and end3 coordinates can be optionally passed into the constructor to set these attributes.  Alternatively, the set_coords() method can be used to set these values.

B<Returns:> $mRNA_exon_obj

=back

=cut




sub new {
    shift;
    my $self = { end5 => 0,   # stores end5 of mRNA exon
		 end3 => 0,   # stores end3 of mRNA exon
		 CDS_exon_obj => 0,   # stores object reference to CDS_obj
		 feat_name => 0    # stores TIGR temp id
	     };

    # end5 and end3 can be included as parameters in constructor.
    if (@_) {
	my ($end5, $end3) = @_;
	if (defined($end5) && defined($end3)) {
	    $self->{end5} = $end5;
	    $self->{end3} = $end3;
	}
    }

    bless ($self);
    return ($self);
}



=over 4

=item get_CDS_obj()

B<Description:> Retrieves the CDS_exon_obj component of this mRNA_exon_obj

B<Parameters:> none

B<Returns:> $cds_exon_obj

If no CDS_exon_obj is attached, returns 0

=back

=cut



sub get_CDS_obj {
    my $self = shift;
    return ($self->{CDS_exon_obj});
}



=over 4

=item get_mRNA_exon_end5_end3()

B<Description:> Retrieves the end5, end3 coordinates of the exon

**Method Deprecated**, use get_coords()

B<Parameters:> none

B<Returns:> (end5, end3)

=back

=cut


sub get_mRNA_exon_end5_end3 {
    my $self = shift;
    return ($self->{end5}, $self->{end3});
}



=over 4

=item set_CDS_exon_obj()

B<Description:> Sets the CDS_exon_obj of the mRNA_exon_obj

B<Parameters:> $cds_exon_obj

B<Returns:> none

=back

=cut

sub set_CDS_exon_obj {
    my $self = shift;
    my $ref = shift;
    if (ref($ref)) {
	$self->{CDS_exon_obj} = $ref;
    }
}


=over 4

=item add_CDS_exon_obj()

B<Description:> Instantiates and adds a new CDS_exon_obj to the mRNA_exon_obj given the CDS coordinates.

B<Parameters:> (end5, end3)

B<Returns:> none

=back

=cut


sub add_CDS_exon_obj {
    my $self = shift;
    my ($end5, $end3) = @_;
    my $cds_obj = CDS_exon_obj->new ($end5, $end3);
    $self->set_CDS_exon_obj($cds_obj);
}


=over 4

=item set_feat_name()

B<Description:> Sets the feat_name attribute of the mRNA_exon_obj

B<Parameters:> $feat_name

B<Returns:> none

=back

=cut



sub set_feat_name {
    my $self = shift;
    my $feat_name = shift;
    $self->{feat_name} = $feat_name;
}


=over 4

=item clone_exon()

B<Description:> Creates a deep clone of this mRNA_exon_obj, using dclone() of Storable.pm

B<Parameters:> none

B<Returns:> $mRNA_exon_obj

=back

=cut
    
    

sub clone_exon {
    my $self = shift;
  
    my $clone_exon = dclone($self);
        
    return ($clone_exon);
}



=over 4

=item get_CDS_end5_end3 ()

B<Description:> Retrieves end5, end3 of the CDS_exon_obj component of this mRNA_exon_obj

B<Parameters:> none

B<Returns:> (end5, end3)

An empty array is returned if no CDS_exon_obj is attached.

=back

=cut


sub get_CDS_end5_end3 {
    my $self = shift;
    my $cds_obj = $self->get_CDS_obj();
    if ($cds_obj) {
	return ($cds_obj->get_CDS_end5_end3());
    } else {
	return ( () );
    }
}


=over 4

=item get_coords()

B<Description:> Retrieves the end5, end3 coordinates of this mRNA_exon_obj

B<Parameters:> none

B<Returns:> (end5, end3)

=back

=cut


sub get_coords {
    my $self = shift;
    return ($self->get_mRNA_exon_end5_end3());
}


=over 4

=item set_coords()

B<Description:> Sets the end5, end3 coordinates of the mRNA_exon_obj

B<Parameters:> (end5, end3)

B<Returns:> none

=back

=cut


## simpler coord setting (end5, end3)
sub set_coords {
    my $self = shift;
    my $end5 = shift;
    my $end3 = shift;
    $self->{end5} = $end5;
    $self->{end3} = $end3;
}


=over 4

=item get_orientation()

B<Description:> Retrieves the orientation of the mRNA_exon_obj based on the relative values of end5, end3

B<Parameters:> none

B<Returns:> +|-|undef

If end5 == end3, strand orientation cannot be inferred based on coordinates alone, so undef is returned.

=back

=cut



sub get_orientation {
    # determine positive or reverse orientation
    my $self = shift;
    my @coords = $self->get_mRNA_exon_end5_end3();
    if (@coords) {
	if ($coords[0] < $coords[1]) {
	    return ('+');
	} elsif ($coords[1] < $coords[0]) {
	    return ('-');
	} else {
	    return (undef());
	}
    }
}





=over 4

=item toString()

B<Description:> Provides a textual description of the mRNA_exon_obj 

B<Parameters:> none

B<Returns:> $text

=back

=cut

sub toString {
    my $self = shift;
    my @coords = $self->get_mRNA_exon_end5_end3();
    my $feat_name = $self->{feat_name};
    my $text = "";
    if ($feat_name) {
	$text .= "feat_name: $feat_name\t";
    }
    $text .= "end5 " . $coords[0] . "\tend3 " . $coords[1] . "\n";
    return ($text);
}










##########################################################################################################################
##########################################################################################################################



=head1 NAME

package CDS_exon_obj

=cut


=head1 DESCRIPTION

    The CDS_exon_obj represents the protein-coding portion of an mRNA_exon_obj.

=cut



package CDS_exon_obj;

use strict;
use Storable qw (store retrieve freeze thaw dclone);



=over 4

=item new()

B<Description:>  Cosntructor for the CDS_exon_obj

B<Parameters:> <(end5, end3)>

The (end5, end3) parameter is optional.  Alternatively, the set_coords() method can be used to set these values.

B<Returns:> $cds_exon_obj

=back

=cut


sub new {
    shift;
    my $self = { end5 => 0,   #stores end5 of cds exon
		 end3 => 0,    #stores end3 of cds exon
		 feat_name => 0 #tigr's temp id
	     };

    # end5 and end3 are allowed constructor parameters
    if (@_) {
	my ($end5, $end3) = @_;
	if (defined ($end5) && defined ($end3)) {
	    $self->{end5} = $end5;
	    $self->{end3} = $end3;
	}
    }
    bless ($self);
    return ($self);
}



=over 4

=item set_feat_name()

B<Description:> Sets the feat_name attribute value of the CDS_exon_obj 

B<Parameters:> $feat_name

B<Returns:> none

=back

=cut


sub set_feat_name {
    my $self = shift;
    my $feat_name = shift;
    $self->{feat_name} = $feat_name;
}


=over 4

=item get_CDS_end5_end3()

B<Description:> Retrieves the end5, end3 coordinates of the CDS_exon_obj

** Method deprecated **, use get_coords()


B<Parameters:> none

B<Returns:> (end5, end3)

=back

=cut


sub get_CDS_end5_end3 {
    my $self = shift;
    return ($self->{end5}, $self->{end3});
}



=over 4

=item set_coords()
    
B<Description:> Sets the (end5, end3) values of the CDS_exon_obj 

B<Parameters:> (end5, end3)

B<Returns:> none

=back

=cut



sub set_coords {
    my $self = shift;
    my $end5 = shift;
    my $end3 = shift;
    $self->{end5} = $end5;
    $self->{end3} = $end3;
}

=over 4

=item get_coords()

B<Description:> Retrieves the (end5, end3) coordinates of the CDS_exon_obj

B<Parameters:> none

B<Returns:> (end5, end3)


The get_coords() method behaves similarly among Gene_obj, mRNA_exon_obj, and CDS_exon_obj, and is generally preferred to other existing methods for extracting these coordinate values.  Other methods persist for backwards compatibility with older applications, but have been largely deprecated.


=back

=cut



sub get_coords {
    my $self = shift;
    return ($self->get_CDS_end5_end3());
}


=over 4

=item get_orientation()

B<Description:> Retrieves the orientation of the CDS_exon_obj based on relative values of end5, end3

B<Parameters:> none

B<Returns:> +|-|undef

undef returned if end5 == end3

=back

=cut


sub get_orientation {
    # determine positive or reverse orientation
    my $self = shift;
    my @coords = $self->get_CDS_end5_end3();
    if (@coords) {
	if ($coords[0] < $coords[1]) {
	    return ('+');
	} elsif ($coords[1] < $coords[0]) {
	    return ('-');
	} else {
	    return (undef());
	}
    }
}



=over 4

=item toString()

B<Description:> Retrieves a textual description of the CDS_exon_obj

B<Parameters:> none

B<Returns:> $text

=back

=cut





sub toString {
    my $self = shift;
    my @coords = $self->get_CDS_end5_end3();
    my $feat_name = $self->{feat_name};
    my $text = "";
    if ($feat_name) {
	$text .= "feat_name: $feat_name\t";
    }
    $text .= "end5 " . $coords[0] . "\tend3 " . $coords[1] . "\n";
    return ($text);
}


1;














