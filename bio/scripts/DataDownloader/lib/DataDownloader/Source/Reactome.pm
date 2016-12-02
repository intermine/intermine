package DataDownloader::Source::Reactome;

use Moose;
extends 'DataDownloader::Source::ABC';

use constant {
    TITLE  => 'Reactome',
    DESCRIPTION => "Pathways from Reactome",
    SOURCE_LINK => "http://www.reactome.org/",
    SOURCE_DIR => 'reactome',
    SOURCES => [{
        FILE   => 'UniProt2Reactome_All_Levels.txt',
        SERVER => 'http://www.reactome.org/download/current/',
    }],
};

1;
