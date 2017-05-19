package WormBase::Admin::Role::FileTransfer;

# Methods for simple file transfers

use Moose::Role;
use Net::FTP::Recursive;

has 'hinxton_ftp' => (
    is => 'ro',
    lazy_build => 1,
    builder    => '_build_ftp',
    );

sub _build_hinxton_ftp {
    my $self = shift;

    my $contact_email = $self->contact_email;
    my $ftp_server    = $self->remote_ftp_server;

    my $ftp = Net::FTP::Recursive->new($ftp_server,
				       Debug => 0,
				       Passive => 1) or $self->logit->logdie("can't instantiate Net::FTP object");

    $ftp->login('anonymous', $contact_email) or $self->logit->logdie("cannot login to remote FTP server");
    $ftp->binary()                           or $self->logit->warn("couldn't switch to binary mode for FTP");    
    return $ftp;
}
    
sub mirror_new_wormbase_release {
    my ($self,$path,$local_mirror_path) = @_;
    
    my $release    = $self->release;
    my $release_id = $self->release_id;

    my $next_release = $release_id++;

    my $ftp = $self->hinxton_ftp;

    my $local_releases_path = $self->ftp_releases_path;
    my $remote_releases_path = $self->remote_ftp_path . '/releases';

    $self->logit->info("  mirroring directory $path from $ftp_server to $local_releases_path");
    chdir $local_releases_path or $self->logit->logdie("cannot chdir to local mirror directory: $local_releases_path");
    $ftp->cwd($remote_releases_path) or $self->logit->error("cannot chdir to remote dir ($remote_releases_path)") && return;

    # Recursively download the NEXT release.  This saves having to check all the others.
    my $r = $ftp->rget(MatchDirs => $next_release); 
    $ftp->quit;
    $self->logit->info("  mirroring directory WS$next_release: complete");
}


1;
