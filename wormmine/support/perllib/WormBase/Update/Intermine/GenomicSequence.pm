package WormBase::Update::Intermine::GenomicSequence;

use Moose;
use Bio::SeqIO;

extends qw/WormBase::Update/;

# The symbolic name of this step
has 'step' => (
    is => 'ro',
    default => 'fetch genomic fasta sequence',
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

	$self->_make_dir("$datadir/genomic_fasta");
	my $fasta_dir = "$datadir/genomic_fasta/$name";
	$self->_make_dir($fasta_dir);
	chdir $fasta_dir or $self->log->logdie("cannot chdir to local data directory: $fasta_dir");
	my $fasta = "ftp://$ftp_host/pub/wormbase/releases/$release/species/$name/$name.$release.genomic.fa.gz";

	my $local_file = "$name.$release.genomic.fa.gz";
	$self->mirror_uri({ uri    => $fasta,
			    output => $local_file,
			    msg    => "mirroring genomic fasta for $name" });

	my $unzipped_file = "$name.$taxonid.current.genomic.fasta";
	$self->unzip_and_rename_file($local_file,$unzipped_file);
#	$self->split_fasta($output_file);
    }

    # Update the datadir current symlink
    $self->update_staging_dir_symlink();
}


1;
