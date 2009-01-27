use strict;
use warnings;
use ExtUtils::MakeMaker;

WriteMakefile(
    NAME                => 'InterMine',
    AUTHOR              => 'FlyMine <info@flymine.org>',
    VERSION_FROM        => 'lib/InterMine/ItemFactory.pm',
    PL_FILES            => {},
    PREREQ_PM => {
                  'File::Compare' => 0,
                  'IO::All' => 0,
                  'IO::String' => 0,
                  'LWP::UserAgent' => 0,
                  'Net::FTP' => 0,
                  'Test::Exception' => 0,
                  'Test::More' => 0,
                  'Test::XML' => 0,
                  'Text::CSV_XS' => 0,
                  'URI' => 0,
                  'XML::Parser::PerlSAX' => 0,
                  'XML::Writer' => 0,
                 },
    dist                => { COMPRESS => 'gzip -9f', SUFFIX => 'gz', },
    clean               => { FILES => 'InterMine-*' },
);
