package DataDownloader::Source::WormbaseRNAi;

use Moose;
extends 'DataDownloader::Source::ABC';
use URI;
use autodie qw(open close);
use Ouch;

use constant {
    TITLE => "Wormbase RNAi",
    DESCRIPTION => "RNAi data from Wormbase",
    SOURCE_LINK => 'http://www.wormbase.org/',
    SOURCE_DIR => 'wormbase-rnai',
    BIOMART_SERVER => 'http://www.biomart.org/biomart/martservice',
    COMPARE => 1,
};

use constant BIOMART_QUERY => q{<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Query>
<Query virtualSchemaName="default" 
       formatter="TSV" 
       header="0" 
       uniqueRows="0" 
       count="" 
       datasetConfigVersion="0.6" >
    <Dataset name="wormbase_rnai" interface="default" >
        <Attribute name="inhibits_gene" />
        <Attribute name="inhibits_gene_public_name" />
        <Attribute name="phenotype" />
        <Attribute name="phenotype_primary_name" />
        <Attribute name="phenotype_short_name" />
        <Attribute name="phenotype_observed" />
        <Attribute name="phenotype_not" />
        <Attribute name="phenotype_penetrance_a" />
        <Attribute name="phenotype_penetrance_b" />
        <Attribute name="rnai" />
        <Attribute name="remark_dmlist" />
    </Dataset>
</Query>
};

#   <Dataset name="wormbase_paper" interface="default" >
#       <Attribute name="pmid" />
#   </Dataset>

sub BUILD {
    my $self = shift;
    my $uri = URI->new(BIOMART_SERVER);
    $uri->query_form(query => BIOMART_QUERY);
    $self->set_sources([
        { 
            URI => "$uri", 
            FILE => 'wormrnai.txt',
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
