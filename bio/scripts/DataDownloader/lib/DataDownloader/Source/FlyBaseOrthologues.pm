package DataDownloader::Source::FlyBaseOrthologues;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => "FlyBase Homology",
    DESCRIPTION => "Homology data from FlyBase",
    SOURCE_LINK => "http://flybase.org",
    HOST => "ftp.flybase.net",
    SOURCE_DIR => "flybase/homology",
    REMOTE_DIR => 'releases/current/precomputed_files/genes',
};

sub BUILD {
    my $self = shift;
    my $ftp = $self->connect;
    my @files = $ftp->ls;
    $ftp->quit;
    my @resources = 
        map {
          { 
            HOST => 'ftp.flybase.net',
            REMOTE_DIR => 'releases/current/precomputed_files/genes',
            FILE => $_,
            EXTRACT => 1,
          }
        }
        grep {/^gene_orthologs_fb_/} @files;
    $self->set_sources([@resources]);
}

sub generate_version {
    my $self = shift;
    my $ftp = $self->connect("releases/current");
    my @files = $ftp->ls;
    $ftp->quit;
    for my $file (@files) {
        return $file if ($file =~ /^FB/);
    }
}

1;
