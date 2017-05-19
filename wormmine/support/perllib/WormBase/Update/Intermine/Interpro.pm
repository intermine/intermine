package WormBase::Update::Intermine::Interpro;


use Moose;
use Net::FTP;

extends qw/WormBase::Update/;

# The symbolic name of this step
has 'step' => (
    is => 'ro',
    default => 'fetch and process interpro',
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

has 'ebi_server' => (
    is      => 'ro',
    default => 'ftp.ebi.ac.uk');

has 'interpro_path' => (
    is       => 'ro',
    default  => 'pub/databases/interpro');

has 'username' => (
    is      => 'ro',
    default => 'anonymous');

has 'pass'     => (
    is      => 'ro',
    default => 'todd@wormbase.org');

has 'connect_to_ftp' => (
    is         => 'ro',
    lazy_build => 1,
    );

sub _build_connect_to_ftp {
    my $self = shift;

    my $user       = $self->username;
    my $pass       = $self->pass;
    my $ftp_server = $self->ebi_server;

    my $ftp = Net::FTP->new($ftp_server,
			    Debug => 0,
			    Passive => 1) or $self->log->logdie("can't instantiate Net::FTP object");
    $ftp->login($user,$pass) or $self->log->logdie("cannot login to remote FTP server: $!");
    $ftp->binary()                           or $self->log->error("couldn't switch to binary mode for FTP");    
    return $ftp;
}


sub run {
    my $self = shift;    
    $self->log->info("Downloading interpro.xml.gz");
    my $datadir = $self->datadir;
    chdir $datadir or $self->log->logdie("cannot chdir to local data directory: $datadir");
    
    my $release = $self->release;
    
    $self->_make_dir("$datadir/interpro");
    chdir "$datadir/interpro" or $self->log->logdie("cannot chdir to local data directory: $datadir/interpro");
    
    
    my $ftp  = $self->connect_to_ftp;
    my $path = $self->interpro_path;
    $ftp->cwd($path) or $self->log->logdie("Couldn't chdir to remote $path") && return;
    
    my $r = $ftp->get('interpro.xml.gz');  # MatchDirs => $release); 
    $ftp->quit;
    $self->log->info("Downloading interpro.xml.gz: done");
    system("gunzip interpro.xml.gz");
    
    open IN,"interpro.xml"       or $self->log->logdie("Couldn't open the interpro file");
    open OUT,">release_data.txt" or $self->log->logdie("Couldn't open the interpro release date file");
    while(<IN>) {
	if (/dbname="INTERPRO"/) {
	    $_ =~ m/file_date="(\d\d-\w\w\w-\d\d)" version="(.*)"\/>/;
	    my $date    = $1;
	    my $version = $2;
	    print OUT <<END;
InterPro protein family and domain data
$version; $date
Protein family and domain data for D. 
melanogaster, A. gambiae and C. elegans
from Interpro (http://www.ebi.ac.uk/interpro).
END
;
	    last;
	}	
    }

    
    # Update the datadir current symlink
    $self->update_staging_dir_symlink();
}

1;
