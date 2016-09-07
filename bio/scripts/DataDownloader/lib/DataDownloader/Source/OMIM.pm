package DataDownloader::Source::OMIM;

use Moose;
extends 'DataDownloader::Source::ABC';

use constant {
    TITLE  => 'OMIM diseases',
    DESCRIPTION => "Online Mendelian Inheritance in Man",
    SOURCE_LINK => "https://omim.org",
    SOURCE_DIR => 'metabolic/omim',
    SOURCES => [{
        FILE   => 'mim2gene.txt',
        SERVER => 'https://omim.org/static/omim/data/',
    }],
};

1;
