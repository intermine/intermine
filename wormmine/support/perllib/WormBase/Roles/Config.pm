package WormBase::Roles::Config;

# Shared configuration for WormBase intermine updates...

use Moose::Role;
use Net::OpenSSH;

# Whether or not we should update the data directory symlink.
# Set to no to avoid updating the symlink of the data dir.
has 'update_data_dir_symlink' => (
    is => 'ro',
    default => 'no',
    );



####################################
#
# Full path to the staging directory
#
#  eg. /usr/local/wormbase/intermine/data
#
#  Set datadir entity of project.xml 
#  to the same path.
#
####################################
has 'intermine_staging' => (
    is         => 'ro',
    default    => '/usr/local/wormbase/intermine/data',
    );


####################################
#
# The Production Manager
#
####################################

has 'production_manager' => ( is => 'ro', default => 'tharris') ;

sub ssh {
    my ($self,$node) = @_;
    my $manager = $self->production_manager;
    my $ssh = Net::OpenSSH->new("$manager\@$node");
    $ssh->error and die "Can't ssh to $manager\@$node: " . $ssh->error;	
    return $ssh;
}


####################################
#
# Available species
# Should be provided by webservices.
#
####################################
has 'species' => ( 
    is => 'ro',
    isa => 'ArrayRef',
    lazy_build => 1 );

sub _build_species {
    my $self = shift;
    my @data = ( 
	{ symbolic_name => 'a_suum',
	  taxon_id      => '6253',
	},
	{ symbolic_name => 'b_malayi',
	  taxon_id      => '6279',
	},
	{ symbolic_name => 'b_xylophilus',
	  taxon_id      => '6326',
	},
	{ symbolic_name => 'c_angaria',
	  taxon_id      => '96668',
	},
	{ symbolic_name => 'c_brenneri',
	  taxon_id      => '135651',
	},
	{ symbolic_name => 'c_briggsae',
	  taxon_id      => '6238',
	},
	{ symbolic_name => 'c_elegans',
	  taxon_id      => '6239',
	},
	{ symbolic_name => 'c_japonica',
	  taxon_id      => '281687',
	},
	{ symbolic_name => 'c_remanei',
	  taxon_id      => '31234',
	},
	{ symbolic_name => 'c_sp5',
	  taxon_id      => 'unknown',
	},
	{ symbolic_name => 'c_sp7',
	  taxon_id      => 'unknown',
	},
	{ symbolic_name => 'c_sp9',
	  taxon_id      => 'unknown',
	},
	{ symbolic_name => 'c_sp11',
	  taxon_id      => 'unknown',
	},
	{ symbolic_name => 'h_bacteriophora',
	  taxon_id      => '37862',
	},
	{ symbolic_name => 'h_contortus',
	  taxon_id      => '6289',
	},
	{ symbolic_name => 'm_hapla',
	  taxon_id      => '6305',
	},
	{ symbolic_name => 'm_incognita',
	  taxon_id      => '6306',
	},
	{ symbolic_name => 'p_pacificus',
	  taxon_id      => '54126',
	},
	{ symbolic_name => 's_ratti',
	  taxon_id      => '34506',
	},
	{ symbolic_name => 't_spiralis',
	  taxon_id      => '6334',
	}
	);
   
    my @species;
    my $release = $self->release;
    foreach (@data) {
	$_->{release} = $release;
	my $species = WormBase->create('Species',$_);
	push @species,$species;
    }

    return \@species;
}


####################################
#
# Couch DB
#
####################################

# We precache directly to our production database. Not sure how intelligent this is.
# This works because the staging database is +1 that in production.
# Meanwhile, the production database can continue to cache to that database.
has 'couchdbmaster'     => ( is => 'rw', default => '206.108.125.165:5984' );

# Or: assume that we are running on the staging server
# Create couchdb on localhost then replicate it.
#has 'couchdbmaster'     => ( is => 'rw', default => '127.0.0.1:5984' );

# The precache_host is the host we will send queries to.
# Typically, this would be the staging server as it will
# have the newest version of the database.

# Adjust here to crawl the live site, too.  The app itself will cache content in
# a single couchdb (PUT requests directed to a single host by proxy).
#has 'precache_host'     => ( is => 'rw', default => 'http://staging.wormbase.org/');

# Prewarming the cache, we should direct requests against the development site.
# This app would actually cache on localhost.

# Later, we might want to crawl the live site at a low rate, too.
has 'precache_host'     => ( is => 'rw', default => 'http://staging.wormbase.org/');


# WormBase 2.0: used in deploy_sofware
has 'local_couchdb_nodes' => (
    is => 'ro',
    isa => 'ArrayRef',
    default => sub {
	[qw/206.108.125.165
            206.108.125.164
            206.108.125.163
            206.108.125.162
            206.108.125.166/],
    },
    );

has 'remote_couchdb_nodes' => (
    is => 'ro',
    isa => 'ArrayRef',
    default => sub {
	[qw//]},
    );

####################################
#
# WormBase root, tmp dir, support dbs
#
####################################

has 'tmp_dir'       => ( is => 'ro', lazy_build => 1 );			 
sub _build_tmp_dir {
    my $self = shift;
    my $dir = $self->wormbase_root . "/tmp/staging";
    $self->_make_dir($dir);
    return $dir;
}

has 'release' => (
    is        => 'rw',
#    lazy_build => 1,
    );

#sub _build_release {
#    my $self = shift;
#    my $release = $self->{release};
#
#    # If not provided, then we need to fetch it from Acedb.
#    unless ($release) {
	

has 'release' => (
    is        => 'rw',
    );


sub release_id {
    my $self    = shift;
    my $release = $self->release;
    $release =~ /WS(.*)/ if $release;
    return $1;
} 


####################################
#
# AceDB
#
####################################

has 'acedb_root' => (
    is => 'ro',
    lazy_build => 1 );

sub _build_acedb_root {
    my $self = shift;
    return $self->wormbase_root . "/acedb";
}

has 'acedb_group' => (
    is => 'ro',
    default => 'acedb' );


has 'acedb_user' => (
    is => 'ro',
    default => 'acedb' );



####################################
#
# MYSQL
#
####################################

has 'drh' => (
    is => 'ro',
    lazy_build => 1 );

sub _build_drh {	
    my $self = shift;       
    my $drh = DBI->install_driver('mysql');
    return $drh;
}


has 'mysql_data_dir' => ( is => 'ro',  default => '/usr/local/mysql/data' );
has 'mysql_user'     => ( is => 'ro',  default => 'root'      );
has 'mysql_pass'     => ( is => 'ro',  default => '3l3g@nz'   );
has 'mysql_host'     => ( is => 'ro',  default => 'localhost' );


####################################
#
# Local FTP
#
####################################

has 'ftp_root' => (
    is      => 'ro',
    default => '/usr/local/ftp/pub/wormbase'
    );

# The releases/ directory
has 'ftp_releases_dir' => (
    is         => 'ro',
    lazy_build => 1,
    );

# Where the production FTP site lives.
# Assumes that the user running the update script
# has access and that the ftp_root is the 
# same as above.
has 'production_ftp_host' => (
    is         => 'ro',
    default    => 'ftp.wormbase.org',
    );

sub _build_ftp_releases_dir {
    my $self = shift;
    return $self->ftp_root . "/releases";
}

# The releases/ directory
has 'ftp_database_tarballs_dir' => (
    is         => 'ro',
    lazy_build => 1,
    );

sub _build_ftp_database_tarballs_dir {
    my $self = shift;
    return $self->ftp_root . "/releases";
}


# This is the VIRTUAL species directory at /species
has 'ftp_species_dir' => (
    is         => 'ro',
    lazy_build => 1,
    );

sub _build_ftp_species_dir {
    my $self = shift;    
    return $self->ftp_root . "/species";
}

####################################
#
# Remote FTP
#
####################################

has 'remote_ftp_server' => (
    is => 'ro',
    default => 'ftp.sanger.ac.uk',
    );

has 'contact_email' => (
    is => 'ro',
    default => 'todd@wormbase.org',
    );
    
has 'remote_ftp_root' => (
    is => 'ro',
    default => 'pub2/wormbase'
    );

has 'remote_ftp_releases_dir' => (
    is         => 'ro',
    lazy_build => 1,
    );

sub _build_remote_ftp_releases_dir {
    my $self = shift;
#    return $self->remote_ftp_root . "/releases.test";
    return $self->remote_ftp_root . '/releases';
}

####################################
#
# Production related configuration
#
####################################


sub _reset_dir {
    my ($self,$target) = @_;
        
    $target =~ /\S+/ or return;
    
#    $self->_remove_dir($target) or return;
    $self->_make_dir($target) or return;    
    return 1;
}

sub _remove_dir {
    my ($self,$target) = @_;

    $target =~ /\S+/ or return;
    $self->log->error("trying to remove $target directory which doesn't exist") unless -e $target;
    system ("rm -rf $target") or $self->log->warn("couldn't remove the $target directory");
    return 1;
}

sub _make_dir {
  my ($self,$target) = @_;
  
  $target =~ /\S+/ or return;
  if (-e $target) {
    return 1;
  }
  mkdir $target, 0775;
  return 1;
}



1;
