package DataDownloader::Source::KeggPathway;

use Moose;
extends 'DataDownloader::Source::ABC';
use IO::Handle;

use SOAP::Lite;

use constant {
    TITLE       => 'KEGG pathways',
    DESCRIPTION => 'Pathways from KEGG',
    SOURCE_LINK => 'http://www.genome.jp/kegg',
    SOURCE_DIR  => 'kegg',
    COMPARE     => 1,
    KEGG_WSDL   => 'http://soap.genome.jp/KEGG.wsdl',
};

sub fetch_all_data {
    my $self = shift;
    my $results = SOAP::Lite->service(KEGG_WSDL)->list_pathways("dem");
    my $out = $self->get_destination_dir->file("kegg_pathways.tsv")->openw();
    $self->debug("Found " . @$results . " pathways");
    foreach my $path (@{$results}) {
        $self->debug("PATHWAYS ENTRY:" . join(", ", map {"$_: " . $path->{$_}} keys %$path));
        $out->print(join("\t", @{$path}{qw/entry_id definition/}), "\n");
    }

}

1;
