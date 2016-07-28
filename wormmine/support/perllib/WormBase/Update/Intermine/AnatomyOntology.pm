package WormBase::Update::Intermine::AnatomyOntology;

use Moose;

extends qw/WormBase::Update/;

# The symbolic name of this step
has 'step' => (
    is => 'ro',
    default => 'fetch anatomy ontology .obo and .association files',
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

sub run {
    my $self = shift;    
    my $datadir = $self->datadir;
    chdir $datadir or $self->log->logdie("cannot chdir to local data directory: $datadir");

    my $release = $self->release;
    
    $self->_make_dir("$datadir/ontology_anatomy");
    chdir "$datadir/ontology_anatomy" or $self->log->logdie("cannot chdir to local data directory: $datadir/ontology_anatomy");

    my $ftp_host    = $self->production_ftp_host;
   
    $self->mirror_uri({ uri    => "ftp://$ftp_host/pub/wormbase/releases/$release/ONTOLOGY/anatomy_ontology.$release.obo",
			output => "anatomy_ontology.current.obo",
			msg    => "mirroring anatomy ontology .obo file" });
    
    $self->mirror_uri({ uri    => "ftp://$ftp_host/pub/wormbase/releases/$release/ONTOLOGY/anatomy_association.$release.wb",
			output => "anatomy_association.current.unsorted",
			msg    => "mirroring anatomy ontology associations file" });
    
    # Sort the gene associations file
#    system("sort -k2,2 anatomy_association.current.unsorted > anatomy_association.current");

    # Update the datadir current symlink
    $self->update_staging_dir_symlink();
}

1;
