package DataDownloader::Source::InterProGO;

use Moose;
extends 'DataDownloader::Source::ABC';

use constant {
    TITLE => "InterPro GO",
    DESCRIPTION => "Gene Annotation from InterPro",
    SOURCE_LINK => "http://www.geneontology.org",
    SOURCE_DIR => "interpro/ontology",
    SOURCES => [
        {
            URI => "http://www.geneontology.org/external2go/interpro2go",
            FILE => "gene_association.interpro",
        },
    ],
};

1;
