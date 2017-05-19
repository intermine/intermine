package WormBase::Update::Intermine::Uniprot;

use Moose;

extends qw/WormBase::Update/;

# The symbolic name of this step
has 'step' => (
    is => 'ro',
    default => 'fetch uniprot data',
    );

# my $query = "http://www.uniprot.org/uniprot/?query=organism:$taxon&format=fasta&include=yes";
# UniProt/SwissProt: manually curated
has 'uniprot_swissprot_uri' => (
    is      => 'ro',
    default => 'http://www.uniprot.org/uniprot/?query=reviewed:yes+AND+organism:%s&force=yes&format=xml',
#    default => 'http://www.uniprot.org/uniprot/?query=reviewed%3ayes+AND+organism%3a%s&force=yes&format=xml',
    );

# UniProt/Trembl: automatic annotations
has 'uniprot_trembl_uri' => (
    is      => 'ro',
    default => 'http://www.uniprot.org/uniprot/?query=organism:%s&force=yes&format=xml',
#    default => 'http://www.uniprot.org/uniprot/?query=organism%3a%s&force=yes&format=xml&compress=yes',
#    default => 'http://www.uniprot.org/uniprot/?query=organism:%s&format=xml&compress=yes',
    );

has 'uniprot_fasta_uri' => (
    is      => 'ro',
    default => 'http://www.uniprot.org/uniprot/?query=reviewed:yes+AND+organism:%s&force=yes&format=fasta',
    );

has 'datadir' => (
    is => 'ro',
    lazy_build => 1);

sub _build_datadir {
    my $self = shift;
    my $release = $self->release;
    my $datadir   = join("/",$self->intermine_staging,$release);
    $self->_make_dir($datadir);
    $self->_make_dir("$datadir/uniprot");
    return "$datadir/uniprot";
}

sub run {
    my $self = shift;    
    my $datadir = $self->datadir;
    chdir $datadir or $self->log->logdie("cannot chdir to local data directory: $datadir");

    my $all_species = $self->species;
    foreach my $species (@$all_species) {
	my $taxon_id = $species->taxon_id;
	my $name     = $species->symbolic_name;

	my $swissprot     = $taxon_id . "_uniprot_sprot.xml";
	my $swissprot_uri = sprintf($self->uniprot_swissprot_uri,$taxon_id);
	$self->mirror_uri({ uri    => $swissprot_uri,,
			    output => $swissprot,
			    msg    => "fetching uniprot:swissprot entries for $name ($taxon_id)"});
	$self->check_file($swissprot);

	my $trembl     = $taxon_id . "_uniprot_trembl.xml";
	my $trembl_uri = sprintf($self->uniprot_trembl_uri,$taxon_id);
	$self->mirror_uri({ uri    => $trembl_uri,
			    output => $trembl,
			    msg    => "fetching uniprot:trembl entries for $name ($taxon_id)" });
	$self->check_file($trembl);

	my $fasta     = $taxon_id . "_uniprot_trembl.fasta";
	my $fasta_uri = sprintf($self->uniprot_fasta_uri,$taxon_id);
	$self->mirror_uri({ uri    => $fasta_uri,
			    output => $fasta,
			    msg    => "fetching uniprot:fasta entries for $name ($taxon_id)" });
	$self->check_file($fasta);
    }
    
    # Update the datadir current symlink
    $self->update_staging_dir_symlink();    
    
}

# Remove empty files? For now, just remove in project.xml
sub check_file {
    my ($self,$file) = @_;
}



1;
