package DataDownloader::Source::DO;

use Moose;
extends 'DataDownloader::Source::ABC';

use constant {
    TITLE => "Disease Ontology",
    DESCRIPTION => "Disease Ontology",
    SOURCE_LINK => "http://www.geneontology.org",
    SOURCE_DIR => "do",
    SOURCES => [
        {
            URI => "https://github.com/DiseaseOntology/HumanDiseaseOntology/blob/master/src/ontology",
            FILE => "HumanDO.obo",
        },
    ],
};

1;

