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
            URI => "http://purl.obolibrary.org/obo",
            FILE => "doid.obo",
        },
    ],
};

1;

