package DataDownloader::Source::HumanGeneIdentifiers;

use Moose;
use MooseX::FollowPBP;
extends 'DataDownloader::Source::ABC';
use URI;
use autodie qw(open close);
use Ouch;

use constant {
    TITLE       => 'Human Gene Identifiers',
    DESCRIPTION => 'Human Gene identifiers from HGNC biomart',
    SOURCE_LINK => 'http://www.ensembl.org/',
    SOURCE_DIR  => 'human/identifiers',
    BIOMART_SERVER => 'http://www.ensembl.org/biomart/martview',
    COMPARE     => 1,
};

use constant BIOMART_QUERY => q{<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Query>
<Query  virtualSchemaName = "default" formatter = "TSV" header = "0" uniqueRows = "0" count = "" datasetConfigVersion = "0.6" >
			
	<Dataset name = "hsapiens_gene_ensembl" interface = "default" >
		<Attribute name = "ensembl_gene_id" />
		<Attribute name = "description" />
		<Attribute name = "source" />
		<Attribute name = "hgnc_symbol" />
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
            FILE => 'humangeneidentifiers.tsv',
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
