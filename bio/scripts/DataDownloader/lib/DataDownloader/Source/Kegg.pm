package DataDownloader::Source::Kegg;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use DataDownloader::Util qw(search_webpage);

use constant {
    TITLE => 'Kegg',
    DESCRIPTION => 'KEGG pathways and D. melanogaster genes involving them',
    SOURCE_LINK => 'http://www.geneome.jp/kegg',
    SOURCE_DIR => 'kegg',
    SOURCES => [
        {
            SUBTITLE => 'Pathway Info',
            HOST => 'ftp.genome.jp',
            REMOTE_DIR => 'pub/kegg/pathway',
            FILE => 'map_title.tab',
        },
        {
            SUBTITLE => 'D-Mel Gene Map',
            HOST => 'ftp.genome.jp',
            REMOTE_DIR => 'pub/kegg/pathway/organisms/dme',
            FILE => 'dme_gene_map.tab',
        },
    ],
};

override generate_version => sub {
    my $self        = shift;
    my $version_url = "http://www.genome.jp/kegg/docs/relnote.html";
    my $expression  = qr/Release (\d\d.\d){1},/;

    return search_webpage( $version_url, $expression );
};

1;
