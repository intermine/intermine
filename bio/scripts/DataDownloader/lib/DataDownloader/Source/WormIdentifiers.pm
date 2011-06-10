package DataDownloader::Source::WormIdentifiers;

use Moose;
use MooseX::FollowPBP;
with 'DataDownloader::Role::Source';

use Webservice::InterMine 0.9700;
use File::Path qw(mkpath);

use constant {
    TITLE       => 'Worm Identifiers',
    DESCRIPTION => 'Wormbase identifiers from modMine',
    SOURCE_LINK => 'http://intermine.modencode.org',
    SOURCE_DIR  => 'worm-identifiers',
    SERVICE_URL => 'http://intermine.modencode.org/query',
    COMPARE     => 1,
};

sub fetch_all_data {
    my $self = shift;
    my $query = Webservice::InterMine->new_query( [SERVICE_URL] );
    $query->add_view(qw/
          Gene.primaryIdentifier Gene.symbol Gene.secondaryIdentifier
    /);
    $query->add_constraint(
        path  => 'Gene.source',
        op    => '=',
        value => 'WormBase'
    );

    my $dest_dir = $self->get_destination_dir;
    mkpath("$dest_dir") unless ( -d $dest_dir );
    my $dest = $dest_dir->file("wormbase-identifers.tsv");

    $self->debug("Downloading wormbase identifiers from modMine to $dest");
    $query->print_results( to => "$dest" );
}

1;
