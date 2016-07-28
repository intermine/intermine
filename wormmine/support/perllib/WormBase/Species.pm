package WormBase::Species;

# A simple species package that makes tracking
# available resources for that species trivial.
# Each species is associated with a release to 
# make construction of these filenames possible.

use Moose;

with 'WormBase::Roles::Config';

has 'symbolic_name' => ( is => 'rw' );
has 'release'       => ( is => 'rw' );
has 'taxon_id'      => ( is => 'rw' );

# Database symbolic name. Mostly for MySQL
has 'db_symbolic_name' => (
    is => 'rw',
    lazy_build => 1 );

sub _build_db_symbolic_name {
    my $self    = shift;
    my $name    = $self->symbolic_name;
    my $release = $self->release;
    return $name . '_' . $release;
}





# The release directory for this species on the FTP site.
has 'release_dir' => ( is => 'ro', lazy_build => 1);
sub _build_release_dir {
    my $self    = shift;
    my $name    = $self->symbolic_name;
    my $release = $self->release;	
    my $dir = join("/",$self->ftp_releases_dir,$release,'species',$name);
    return $dir;
}


has 'blast_dir' => ( is => 'ro', lazy_build => 1 );
sub _build_blast_dir { 
    my $self = shift;
    my $release = $self->release;
    my $name    = $self->symbolic_name;
    my $path = join('/',$self->support_databases_dir,$release);
    $self->_make_dir($path);
    $self->_make_dir("$path/blast");
    $self->_make_dir("$path/blast/$name");
    return "$path/blast/$name";
}

has 'blat_dir' => ( is => 'ro', lazy_build => 1 );
sub _build_blat_dir { 
    my $self = shift;
    my $release = $self->release;
    my $name    = $self->symbolic_name;
    my $path = join('/',$self->support_databases_dir,$release);
    $self->_make_dir($path);
    $self->_make_dir("$path/blat");
    $self->_make_dir("$path/blat/$name");
    return "$path/blat/$name";
}



has 'epcr_dir' => ( is => 'ro', lazy_build => 1 );
sub _build_epcr_dir { 
    my $self = shift;
    my $release = $self->release;
    my $name    = $self->symbolic_name;
    my $path = join('/',$self->support_databases_dir,$release);
    $self->_make_dir($path);
    $self->_make_dir("$path/epcr");
    $self->_make_dir("$path/epcr/$name");
    return "$path/epcr/$name";
}



######################################################
#
#   Filenames
#
######################################################

# Discover the name of the fasta file for a given species.
# More appropriate as a Role.
has 'genomic_fasta' => (
    is => 'ro',
    lazy_build => 1);

sub _build_genomic_fasta {
    my $self    = shift;
    my $name    = $self->symbolic_name;
    my $release = $self->release;	
    my $fasta   = "$name.$release.genomic.fa.gz";
    return $fasta;
}

# Discover the name of the fasta file for a given species.
# More appropriate as a Role.
has 'protein_fasta' => (
    is => 'ro',
    lazy_build => 1);

sub _build_protein_fasta {
    my $self    = shift;
    my $name    = $self->symbolic_name;
    my $release = $self->release;	
    my $fasta   = "$name.$release.protein.fa.gz";
    return $fasta;
}

has 'ests_file' => (
    is => 'ro',
    lazy_build => 1);

sub _build_ests_file {
    my $self    = shift;
    my $name    = $self->symbolic_name;
    my $release = $self->release;	
    my $fasta   = "$name.$release.ests.fa.gz";
    return $fasta;
}


# Discover the name of the GFF file and its version.
has 'gff_file' => (
    is => 'rw',
    lazy_build => 1
);

sub _build_gff_file {
    my $self    = shift;
    my $name    = $self->symbolic_name;
    my $release = $self->release;	
    my $gff = join("/",$self->release_dir,"$name.$release.annotations.gff2.gz");
    if (-e $gff) {
	$self->gff_version('2');
    } else {
	$gff = join("/",$self->release_dir,"$name.$release.annotations.gff3.gz");
	if (-e $gff) {
	    $self->gff_version('3');
	}
    }
    $self->log->logdie(uc($name) . ": couldn't find a suitable GFF file") unless $gff;
    return $gff;
}
        
has 'gff_version' => (
    isa     => 'Str',
    is      => 'rw',
    );






1;
