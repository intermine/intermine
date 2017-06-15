package DataDownloader::Source::MousePheno;

use Moose;
extends 'DataDownloader::Source::ABC';

use constant {
    TITLE => 'MousePheno',
    DESCRIPTION => 'Mouse phenotypical data from MGI',
    SOURCE_LINK => 'http://www.informatics.jax.org',
    SOURCE_DIR => 'metabolic/mouse-pheno',
    SOURCES => [
        {
            SERVER => 'http://www.informatics.jax.org/downloads/reports',            
            FILE => 'MGI_PhenotypicAllele.rpt',
        },
        {
            SERVER => 'http://www.informatics.jax.org/downloads/reports',
            FILE => 'MGI_PhenoGenoMP.rpt',
        },
        {
            SERVER => 'http://www.informatics.jax.org/downloads/reports',
            FILE => 'MGI_QTLAllele.rpt',
        },
        {
            SERVER => 'http://www.informatics.jax.org/downloads/reports',
            FILE => 'MPheno_OBO.ontology',
        },
    ],
};

override make_source => sub {
    my ($self, $args) = @_;
    my $_ = $args->{FILE};
    my $source = super();
    if (/ontology$/) {
      s/ontology$/obo/;
        $source->set_destination($source->get_destination_dir->file($_));
    }
    return $source;
};

1;