package WormBase::Update::Intermine::Reactome;


use Moose;
use Net::FTP;

extends qw/WormBase::Update/;

# The symbolic name of this step
has 'step' => (
    is => 'ro',
    default => 'fetch and process reactome',
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

has 'reactome_biopax_uri' => (
    is      => 'ro',
    default => 'http://www.reactome.org/download/current/biopax.zip',
    );


sub run {
    my $self = shift;    
    $self->log->info("Downloading interpro.xml.gz");
    my $datadir = $self->datadir;
    chdir $datadir or $self->log->logdie("cannot chdir to local data directory: $datadir");
    
    my $release = $self->release;
    
    $self->_make_dir("$datadir/reactome");
    chdir "$datadir/reactome" or $self->log->logdie("cannot chdir to local data directory: $datadir/interpro");
    
    my $uri = $self->reactome_biopax_uri;
    system("wget $uri")        && $self->log->logdie->("cannot download the biopax file from Reactome");
    system("unzip biopax.zip") && $self->log->logdie->("cannot unpack the biopax file");
    
    # Update the datadir current symlink
    $self->update_staging_dir_symlink();
}

1;
