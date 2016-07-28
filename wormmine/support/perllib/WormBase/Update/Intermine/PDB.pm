package WormBase::Update::Intermine::PDB;


use Moose;
use Net::FTP;

extends qw/WormBase::Update/;

# The symbolic name of this step
has 'step' => (
    is => 'ro',
    default => 'fetch and process PDB files',
    );

has 'datadir' => (
    is => 'ro',
    lazy_build => 1);

sub _build_datadir {
    my $self = shift;
    my $release = $self->release;
    my $datadir   = join("/",$self->intermine_staging,$release);
    $self->_make_dir($datadir);
    return $datadir;
}

has 'biogrid_uri' => (
    is      => 'ro',
    default => 'http://thebiogrid.org/downloads/archives/Release%20Archive/BIOGRID-3.1.85/BIOGRID-ORGANISM-3.1.85.psi25.zip',
    );


sub run {
    my $self = shift;    
    $self->log->info("Downloading interpro.xml.gz");
    my $datadir = $self->datadir;
    chdir $datadir or $self->log->logdie("cannot chdir to local data directory: $datadir");

    $self->_make_dir("$datadir/pdb");
    
    my $release = $self->release;

    my $all_species = $self->species;   
    foreach my $species (@$all_species) {	
	my $taxonid = $species->taxon_id;
	my $name    = $species->symbolic_name;

	chdir "$datadir/pdb" or $self->log->logdie("cannot chdir to local data directory: $datadir/pdb");
	$self->_make_dir("$datadir/pdb/taxonid");
	chdir "$datadir/pdb/$taxonid" or $self->log->logdie("cannot chdir to local data directory: $datadir/pdb/$taxonid");	
    
	my $uri = $self->biogrid_uri;
	system("wget $uri")          && $self->log->logdie->("cannot download the biopax file from Biogrid");
	system("unzip BIOGRID*.zip") && $self->log->logdie->("cannot unpack the biogrid file");
    }	
    
    # Update the datadir current symlink
    $self->update_staging_dir_symlink();
}


1;
