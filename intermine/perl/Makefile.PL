use strict;
use warnings;
use ExtUtils::MakeMaker;

WriteMakefile(
    NAME                => 'InterMine',
    AUTHOR              => 'FlyMine <perl@flymine.org>',
    VERSION_FROM        => 'lib/InterMine.pm',
    ABSTRACT_FROM       => 'lib/InterMine.pm',
    PL_FILES            => {},
    PREREQ_PM => {
        'Test::More' => 0,
    },
    dist                => { COMPRESS => 'gzip -9f', SUFFIX => 'gz', },
    clean               => { FILES => 'InterMine-*' },
);
