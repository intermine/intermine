package DataDownloader::Source::MousePheno;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => 'MousePheno',
    DESCRIPTION => 'Mouse phenotypical data from MGI',
    SOURCE_LINK => 'http://www.informatics.jax.org',
    SOURCE_DIR => 'metabolic/mouse-pheno',
    SOURCES => [
        {
            HOST => 'ftp.informatics.jax.org',
            REMOTE_DIR => '/pub/reports/',
            FILE => 'MGI_PhenotypicAllele.rpt',
            EXTRACT => 0,
        },
        {
            HOST => 'ftp.informatics.jax.org',
            REMOTE_DIR => '/pub/reports/',
            FILE => 'MGI_PhenoGenoMP.rpt',
            EXTRACT => 0,
        },
        {
            HOST => 'ftp.informatics.jax.org',
            REMOTE_DIR => '/pub/reports/',
            FILE => 'MGI_QTLAllele.rpt',
            EXTRACT => 0,
        },
        {
            HOST => 'ftp.informatics.jax.org',
            REMOTE_DIR => '/pub/reports/',
            FILE => 'MPheno_OBO.ontology',
            EXTRACT => 0,
        },
    ],
};

1;