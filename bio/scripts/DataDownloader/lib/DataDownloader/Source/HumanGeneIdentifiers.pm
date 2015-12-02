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
    SOURCE_LINK => 'http://www.genenames.org/',
    SOURCE_DIR  => 'human/identifiers',
    BIOMART_SERVER => 'http://biomart.genenames.org/martform/',
    COMPARE     => 1,
};

use constant BIOMART_QUERY => q{<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Query>
<Query client="biomartclient" processor="HTML" limit="-1" header="0"><Dataset name="hgnc_gene_mart" config="hgnc_gene_config"><Filter name="hgnc_gene__status_1010" value="Approved" filter_list=""/><Attribute name="hgnc_gene__approved_symbol_1010"/><Attribute name="hgnc_gene__hgnc_gene_id_1010"/><Attribute name="hgnc_gene__ncbi_gene__gene_id_1026"/><Attribute name="hgnc_gene__ensembl_gene__ensembl_gene_id_104"/><Attribute name="hgnc_gene__omim__omim_id_1027"/><Attribute name="hgnc_gene__approved_name_1010"/><Attribute name="hgnc_gene__hgnc_previous_symbol__previous_symbol_1012"/><Attribute name="hgnc_gene__hgnc_previous_name__previous_name_1011"/><Attribute name="hgnc_gene__hgnc_alias_name__alias_name_107"/><Attribute name="hgnc_gene__hgnc_alias_symbol__alias_symbol_108"/><Attribute name="hgnc_gene__chromosome_location_1010"/></Dataset></Query>

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
