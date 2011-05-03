package DataDownloader::Source::FlyBaseFasta;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => "FlyBase Sequence data",
    DESCRIPTION => "Fasta Files from FlyBase",
    SOURCE_LINK => 'http://flybase.org',
    HOST => "ftp.flybase.net",
    SOURCE_DIR => "flybase/fasta",
};

use constant FILE_TYPES => 
    qr/all-CDS|all-gene-|all-five_prime_UTR|all-three_prime_UTR/;

sub BUILD {
    my $self = shift;
    my @resources;
    my $ftp = $self->connect("genomes");
    my @species = $ftp->ls;
    $ftp->quit;
    for my $s (@species) {
        if (length($s) == 4) {
            my $ftp = $self->connect("genomes/$s/current/fasta");
            for my $file ($ftp->ls) {
                if ($file =~ FILE_TYPES) {
                    push @resources, {
                        HOST => 'ftp.flybase.net',
                        REMOTE_DIR => "genomes/$s/current/fasta",
                        FILE => $file, 
                        EXTRACT => 1,
                    };
                }
            }
            $ftp->quit;
        }
    }
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
