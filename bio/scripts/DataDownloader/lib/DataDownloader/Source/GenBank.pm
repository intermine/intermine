package DataDownloader::Source::GenBank;

use Moose;
extends 'DataDownloader::Source::FtpBase';
use MooseX::FollowPBP;
use Ouch qw(:traditional);

use constant {
    TITLE => 'GenBank',
    DESCRIPTION => 'An annotated collection of all publicly available DNA sequences',
    SOURCE_LINK => 'http://www.ncbi.nlm.nih.gov/genbank/',
    SOURCE_DIR => 'genbank', 
    HOST => 'ftp.ncbi.nlm.nih.gov',
    REMOTE_DIR => 'genomes',
};

sub BUILD {
    my $self    = shift;

    my $organisms = $self->get_options->{organisms} || [];

    for my $org (@$organisms) {
        ptint $org
    }
}

1;
