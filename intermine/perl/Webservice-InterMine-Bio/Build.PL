use strict;
use warnings;
use Module::Build;

my $builder = Module::Build->new(
    module_name         => 'Webservice::InterMine::Bio',
    license             => 'perl',
    dist_author         => q{Alex Kalderimis <dev@intermine.org>},
    dist_version_from   => 'lib/Webservice/InterMine/Bio.pm',
    build_requires => {
        'Test::More' => 0,
    },
    add_to_cleanup      => [ 'Webservice-InterMine-Bio-*' ],
    create_makefile_pl => 'traditional',
);

$builder->create_build_script();
