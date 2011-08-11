package DataDownloader::Source::FlyOntologies;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => 'Fly Ontologies',
    DESCRIPTION =>
        'Ontologies from FlyBase - development and CV terms',
    SOURCE_LINK => 'http://flybase.org',
    SOURCE_DIR => 'ontologies',
    SOURCES => [
        {
            SUBTITLE => 'Fly Development',
            HOST => 'ftp.flybase.net',
            REMOTE_DIR => "releases/current/precomputed_files/ontologies",
            FILE => 'fly_development.obo.zip',
            EXTRACT => 1,
        },
        {
            SUBTITLE => 'Controlled Vocabulary',
            HOST => 'ftp.flybase.net',
            REMOTE_DIR => "releases/current/precomputed_files/ontologies",
            FILE => 'flybase_controlled_vocabulary.obo.zip',
            EXTRACT => 1,
        },
    ],
};

1;
