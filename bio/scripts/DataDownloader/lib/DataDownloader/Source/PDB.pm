package DataDownloader::Source::PDB;

use Moose;
extends 'DataDownloader::Source::ABC';
require LWP::UserAgent;
require HTTP::Request;
use Time::Piece;
use Web::Scraper;
use Ouch;
use URI;

use constant {
    TITLE => "Protein Data Bank",
    DESCRIPTION => 'information about experimentally-determined structures of proteins, nucleic acids, and complex assemblies.',
    SOURCE_LINK => "http://www.rcsb.org/pdb/home/home.do",
    SOURCE_DIR => 'pdb',
    SEARCH_URL => 'http://www.rcsb.org/pdb/rest/search',
};

use constant ORGANISMS => {
    
    # change to a specific organism
    '83333' => 'Escherichia coli K-12',
};

sub BUILD {
    my $self = shift;
    my @sources;
    my $ua = LWP::UserAgent->new;
    while (my ($taxon, $organism_name) = each %{ $self->ORGANISMS }) {
        my $XML_query = qq(
<?xml version="1.0" encoding="UTF-8"?>

<orgPdbQuery>
<version>B0905</version>
<queryType>org.pdb.query.simple.OrganismQuery</queryType>
<description>Organism Search : Organism Name=$organism_name </description>

<organismName>$organism_name</organismName>
</orgPdbQuery>
        );
        my $request = HTTP::Request->new( POST => SEARCH_URL );
        $request->content_type( 'application/x-www-form-urlencoded' );
        $request->content( $XML_query );

        my $response = $ua->request( $request );
        ouch DownloadError => $response->status_line unless $response->is_success;
        push @sources, map {{
            SERVER => 'http://www.rcsb.org/pdb/files',
            FILE => $_ . '.pdb.gz',
            EXTRACT => 1,
            SUB_DIR => [$taxon],
            METHOD => 'HTTP',
        }} split(/\s+/, $response->content);
    }
    $self->set_sources(\@sources);
}

sub generate_version {
    my $self = shift;
    my $scraper = scraper {
        # This scraper might need updating if they change their page structure...
        process 'div#headerText > a', 'contents[]' => 'TEXT';
    };
    my $scraps = $scraper->scrape(URI->new(SOURCE_LINK));
    for my $content (@{$scraps->{contents}}) {
        if ($content =~ /\w+day\s/) {
            $content =~ s/^\s*//;
            $content =~ s/\s*at.*$//;
            my $t = Time::Piece->strptime($content, "%A %b %d, %Y");
            return $t->ymd;
        }
    }
    ouch DownloadError => "Could not determine PDB release";
}

1;
