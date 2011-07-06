package DataDownloader::Source::PsiOntology;

use Moose;
extends 'DataDownloader::Source::ABC';

use constant {
    TITLE       => 'PSI Ontology',
    DESCRIPTION => 'Molecular Interaction (PSI MI 2.5) from HUPO',
    SOURCE_LINK => 'http://www.psidev.info',
    SOURCE_DIR  => 'psi/ontology',
    COMPARE     => 1,
    SOURCES     => [{
        SERVER => "http://obo.cvs.sourceforge.net/viewvc/*checkout*/obo/obo/ontology/genomic-proteomic/protein",
        FILE   => 'psi-mi.obo',
    }],
};

1;
