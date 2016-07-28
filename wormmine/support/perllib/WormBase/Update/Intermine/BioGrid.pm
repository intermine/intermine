package WormBase::Update::Intermine::BioGrid;


use Moose;
use Net::FTP;

extends qw/WormBase::Update/;

# The symbolic name of this step
has 'step' => (
    is => 'ro',
    default => 'fetch and process biogrid',
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
    
    my $release = $self->release;
    
    $self->_make_dir("$datadir/biogrid");
    chdir "$datadir/biogrid" or $self->log->logdie("cannot chdir to local data directory: $datadir/biogrid");
    
    my $uri = $self->biogrid_uri;
    system("wget $uri")          && $self->log->logdie->("cannot download the biopax file from Biogrid");
    system("unzip BIOGRID*.zip") && $self->log->logdie->("cannot unpack the biogrid file");
    system("mkdir temp ; mv * temp/.") && $self->log->logdie->("cannot move old files out of the way");
    system("mv temp/*Caeno* .") && $self->log->logdie->("cannot mv the C. elegans file to root.");
    
    # Update the datadir current symlink
    $self->update_staging_dir_symlink();
}

1;
