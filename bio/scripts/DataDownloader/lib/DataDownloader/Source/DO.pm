package DataDownloader::Source::DO;

use Moose;
extends 'DataDownloader::Source::ABC';

use constant {
    TITLE => "Disease Ontology",
    DESCRIPTION => "Disease Ontology",
    SOURCE_LINK => "http://www.disease-ontology.org",
    SOURCE_DIR => "do",
    SOURCES => [
        {
            SERVER => "https://raw.githubusercontent.com/DiseaseOntology/HumanDiseaseOntology/master/src/ontology",
            FILE => "doid-non-classified.obo",
        },
    ],
};

1;
