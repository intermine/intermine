package DataDownloader::Source::HPO;

use Moose;
extends 'DataDownloader::Source::ABC';

# Human Phenotype Ontology and anotation 


use constant {
    TITLE       => 
        'Human Phenotype Ontology',
    DESCRIPTION => 
        "Human Phenotype Ontology and anotation",
    SOURCE_LINK => 
        "http://www.human-phenotype-ontology.org/",
    SOURCE_DIR => "hpo",
};

sub BUILD {
    my $self = shift;
    $self->set_sources([
        {
            SERVER  => 'http://compbio.charite.de/hudson/job/hpo/lastStableBuild/artifact/ontology/release/',
            FILE    => "hp.obo",
        },
        {
            SERVER  => 'http://compbio.charite.de/hudson/job/hpo.annotations/lastStableBuild/artifact/misc/',
            FILE    => "negative_phenotype_annotation.tab",
        },
        {
            SERVER  => 'http://compbio.charite.de/hudson/job/hpo.annotations/lastStableBuild/artifact/misc/',
            FILE    => "phenotype_annotation.tab",
        }, 
        {
            SERVER  => 'http://compbio.charite.de/hudson/job/hpo.annotations/lastStableBuild/artifact/misc/',
            FILE    => "phenotype_annotation_hpoteam.tab",
        },
        {
            SERVER  => 'http://compbio.charite.de/hudson/job/hpo.annotations.monthly/lastStableBuild/artifact/annotation/',
            FILE    => "ALL_SOURCES_ALL_FREQUENCIES_diseases_to_genes_to_phenotypes.txt",
        },
    ]);
}

1;