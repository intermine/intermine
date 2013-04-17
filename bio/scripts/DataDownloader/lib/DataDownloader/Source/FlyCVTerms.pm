package DataDownloader::Source::FlyCVTerms;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => 'Fly Ontologies',
    DESCRIPTION =>
        'Ontologies from FlyBase - CV terms',
    SOURCE_LINK => 'http://flybase.org',
    SOURCE_DIR => 'ontologies/fly-cv',
    SOURCES => [
        {
            SUBTITLE => 'Controlled Vocabulary',
            HOST => 'ftp.flybase.net',
            REMOTE_DIR => "releases/current/precomputed_files/ontologies",
            FILE => 'flybase_controlled_vocabulary.obo.gz',
            EXTRACT => 1,
        },
    ],
};

1;
