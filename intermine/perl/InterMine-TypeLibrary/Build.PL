use strict;
use warnings;
use Module::Build;

my $builder = Module::Build->new(
    module_name         => 'InterMine::TypeLibrary',
    license             => 'perl',
    dist_author         => q{InterMine <dev@intermine.org>},
    dist_abstract       => "A library of MooseX types used by InterMine",
    dist_version_from   => 'lib/InterMine/TypeLibrary.pm',
    requires => {
        'MooseX::Types' => 0,
        'Math::BigInt' => 0,
        'Scalar::Util' => 0,
    },
    build_requires => {
        'Test::More' => 0,
    },
    add_to_cleanup      => [ 'InterMine-TypeLibrary-*' ],
    create_makefile_pl => 'traditional',
);

$builder->create_build_script();
