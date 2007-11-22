use strict;
use warnings;
use ExtUtils::MakeMaker;

WriteMakefile(
    NAME                => 'InterMine',
    AUTHOR              => 'FlyMine <info@flymine.org>',
    VERSION_FROM        => 'lib/InterMine/ItemFactory.pm',
    PL_FILES            => {},
    PREREQ_PM => {
                  'Test::XML' => 0,
                  'XML::Parser::PerlSAX' => 0,
                  'XML::Writer' => 0,
                  'Test::More' => 0,
                  'Config::Properties' => 0,
                 },
    dist                => { COMPRESS => 'gzip -9f', SUFFIX => 'gz', },
    clean               => { FILES => 'InterMine-*' },
);
