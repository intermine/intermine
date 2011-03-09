use strict;
use warnings;
use Module::Build;

my $builder = Module::Build->new(
    module_name          => 'InterMine::Item',
    license              => 'perl',
    dist_author          => q{InterMine <dev@intermine.org>},
    dist_version_from    => 'lib/InterMine/Item.pm',
    dist_abstract        => 'Tools for writing InterMine-Items-XML files',
    recursive_test_files => 1,
    requires => {
        'InterMine::Model' => 0.94,
        'XML::Writer' => 0,
    },
    build_requires => {
        'Test::More' => 0,
        'Test::Warn' => 0,
        'Test::Exception' => 0,
        'Test::XML' => 0,
    },
    add_to_cleanup      => [ 'InterMine-Item-*' ],
    create_makefile_pl => 'traditional',
);

$builder->create_build_script();
