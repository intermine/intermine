package DataDownloader::Source::WormIdentifiers;

use Moose;
use MooseX::FollowPBP;
extends 'DataDownloader::Source::ABC';
use URI;
use autodie qw(open close);
use Ouch;

use constant {
    TITLE       => 'Worm Identifiers',
    DESCRIPTION => 'Wormbase identifiers from biomart',
    SOURCE_LINK => 'http://www.biomart.org',
    SOURCE_DIR  => 'worm-identifiers',
    BIOMART_SERVER => 'http://www.biomart.org/biomart/martservice',
    COMPARE     => 1,
};

use constant BIOMART_QUERY => q{<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Query>
<Query virtualSchemaName="default" 
       formatter="TSV" 
       header="0" 
       uniqueRows="0" 
       count="" 
       datasetConfigVersion="0.6" >
    <Dataset name="wormbase_gene" interface="default" >
        <Filter name = "species_selection" value = "Caenorhabditis elegans"/>
        <Filter name = "identity_status" value = "Live"/>
        <Attribute name="gene" />
        <Attribute name="cgc_name" />
        <Attribute name="sequence_name" />
    </Dataset>
</Query>
};

sub BUILD {
    my $self = shift;
    my $uri = URI->new(BIOMART_SERVER);
    $uri->query_form(query => BIOMART_QUERY);
    $self->set_sources([
        { 
            URI => "$uri", 
            FILE => 'wormidentifiers.tsv',
            POST_PROCESSOR => sub {
                my ($self, $temp, $destination) = @_;
                open(my $in, '<:utf8', $temp);
                open(my $out, '>:utf8', $destination);
                while (my $content = <$in>) {
                    if ($content =~ /^Query ERROR:/) {
                        ouch DownloadError => "BioMart failed us: " . $content;
                    }
                    $out->print($content);
                }
            },
        }
    ]);
}

1;
