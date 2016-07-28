package WormBase::Update::Intermine::PhenotypeOntology;

use Moose;

extends qw/WormBase::Update/;

# The symbolic name of this step
has 'step' => (
    is => 'ro',
    default => 'fetch phenotype ontology .obo and .association files',
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
    
    $self->_make_dir("$datadir/ontology_phenotype");
    chdir "$datadir/ontology_phenotype" or $self->log->logdie("cannot chdir to local data directory: $datadir/ontology_phenotype");

    my $ftp_host    = $self->production_ftp_host;
   
    $self->mirror_uri({ uri    => "ftp://$ftp_host/pub/wormbase/releases/$release/ONTOLOGY/gene_ontology.$release.obo",
			output => "phenotype_ontology.current.obo",
			msg    => "mirroring phenotype ontology .obo file" });
    
    $self->mirror_uri({ uri    => "ftp://$ftp_host/pub/wormbase/releases/$release/ONTOLOGY/gene_association.$release.wb",
			output => "phenotype_association.current.unsorted",
			msg    => "mirroring phenotype ontology associations file" });
    
    # Sort the gene associations file
    # system("sort -k2,2 gene_association.current.unsorted > gene_association.current");

    # Update the datadir current symlink
    $self->update_staging_dir_symlink();
}

1;
