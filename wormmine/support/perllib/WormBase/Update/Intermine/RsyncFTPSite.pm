package WormBase::Update::Staging::RsyncFTPSite;

use Moose;
extends qw/WormBase::Update/;

# The symbolic name of this step
has 'step' => (
    is      => 'ro',
    default => 'rsync the staged FTP site to the production FTP site',
);

sub run {
    my $self = shift;       
    my $release = $self->release;

    if ($release) {
	my $releases_dir = $self->ftp_releases_dir;
	chdir($releases_dir);
	$self->update_symlink({target => $release,
			       symlink => 'current-dev.wormbase.org-release',
			      });
	
	# Update symlinks to the development version
	$self->update_ftp_site_symlinks('development');
	$self->rsync_ftp_directory();       
    }
}


# Rsync the staging server's FTP directory
# to the production FTP directory (assuming that the
# two are running on different machines).
sub rsync_ftp_directory {
    my $self = shift;
    
    my $production_host  = $self->production_ftp_host;
    my $ftp_root         = $self->ftp_root;
    $self->log->info("rsyncing to FTP site to $production_host");
    
#	$self->system_call("rsync -Cavv --exclude httpd.conf --exclude cache --exclude sessions --exclude databases --exclude tmp/ --exclude extlib --exclude ace_images/ --exclude html/rss/ $app_root/ ${node}:$wormbase_root/shared/website/classic",'rsyncing classic site staging directory into production');
    $self->system_call("rsync -Cav $ftp_root/ ${production_host}:$ftp_root",'rsyncing staging FTP site to the production host');
}



1;
