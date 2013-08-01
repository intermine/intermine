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
    BIOMART_SERVER => 'http://www.genenames.org/biomart/martservice',
    COMPARE     => 1,
};

use constant BIOMART_QUERY => q{<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Query>
<Query  virtualSchemaName = "default" formatter = "TSV" header = "0" uniqueRows = "0" count = "" datasetConfigVersion = "0.7" >
            
    <Dataset name = "hgnc" interface = "default" >
        <Filter name = "gd_record" value = "primary"/>
        <Filter name = "gd_status" value = "Approved"/>
        <Attribute name = "gd_app_sym" />
        <Attribute name = "gd_hgnc_id_key" />
        <Attribute name = "md_eg_id" />
        <Attribute name = "md_ensembl_id" />
        <Attribute name = "md_mim_id" />
        <Attribute name = "gd_app_name" />
        <Attribute name = "gd_prev_sym" />
        <Attribute name = "gd_prev_name" />
        <Attribute name = "gd_name_aliases" />
        <Attribute name = "gd_aliases" />
        <Attribute name = "gd_pub_chrom_map" />
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
