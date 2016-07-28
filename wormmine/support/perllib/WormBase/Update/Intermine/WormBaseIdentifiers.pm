package WormBase::Update::Intermine::WormBaseIdentifiers;

use Moose;
use Bio::SeqIO;

extends qw/WormBase::Update::Intermine/;

# The symbolic name of this step
has 'step' => (
    is => 'ro',
    default => 'fetch wormbase identifiers',
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
    my $ftp_host    = $self->production_ftp_host;

    my $dir = "$datadir/identifiers";    	
    $self->_make_dir($dir);
    chdir $dir or $self->log->logdie("cannot chdir to local data directory: $dir");
    
    my $remote_file_uri = "ftp://$ftp_host/pub/wormbase/datasets-wormbase/gene_ids/wormbase.$release.gene_ids.txt.gz";
    my $local_file_name = "wormbase.$release.gene_ids.txt.gz";   
    $self->mirror_uri({ uri    => $remote_file_uri,
			output => $local_file_name,
			msg    => "mirroring gene IDs" });
    
    $self->process_file($local_file_name);

    # Update the datadir current symlink
    $self->update_staging_dir_symlink();
}




sub process_file {
    my ($self,$file,$output_file) = @_;
    system("/bin/gunzip -c $file > wormbase.current.gene_ids.txt") && $self->log->warn("Couldn't open $file for processing: $!");
}





1;
