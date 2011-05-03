package DataDownloader::Source::InterproGo;

use Moose;
extends 'DataDownloader::Source::ABC';

use constant {
    TITLE => "Interpro Go",
    DESCRIPTION => "Gene Annotation from Interpro",
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
