package DataDownloader::Source::Compara;

use Moose;
use MooseX::FollowPBP;
with 'DataDownloader::Role::Source';

use Bio::EnsEMBL::Registry;
use autodie qw(open close);
use Ouch qw(:traditional);

use constant {
    TITLE => "Ensembl Compara",
    DESCRIPTION => "Ensembl Comparative Genomics Data",
    SOURCE_LINK => "http://www.ensembl.org",
    SOURCE_DIR => "ensembl/compara",
};

sub fetch_all_data {
    my $self = shift;

}

1;
