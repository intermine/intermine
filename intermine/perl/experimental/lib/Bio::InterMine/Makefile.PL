use strict;
use warnings;
use ExtUtils::MakeMaker;

WriteMakefile(
    NAME                => 'Bio::InterMine::Query',
    AUTHOR              => q{InterMine <dev@flymine.org>},
    VERSION_FROM        => 'lib/Bio/InterMine/Query.pm',
    ABSTRACT_FROM       => 'lib/Bio/InterMine/Query.pm',
    ($ExtUtils::MakeMaker::VERSION >= 6.3002
      ? ('LICENSE'=> 'lgpl')
      : ()),
    PL_FILES            => {},
    PREREQ_PM => {
        'Test::More' => 0,
    },
    dist                => { COMPRESS => 'gzip -9f', SUFFIX => 'gz', },
    clean               => { FILES => 'Bio::InterMine-*' },
);
