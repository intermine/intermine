use strict;
use warnings;
use Module::Build;

my $builder = Module::Build->new(
    module_name         => 'InterMine::Util',
    license             => 'perl',
    dist_author         => q{InterMine <dev@intermine.org>},
    dist_version_from   => 'lib/InterMine/Util.pm',
    build_requires => {
        'Test::More' => 0,
    },
    add_to_cleanup      => [ 'InterMine-Util-*' ],
    create_makefile_pl => 'traditional',
);

$builder->create_build_script();
