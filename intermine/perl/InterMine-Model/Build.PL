use Module::Build;

my $build = Module::Build->new(
    module_name   => 'InterMine::Model',
    license       => 'perl',
    dist_author   => 'Alex Kalderimis <dev@intermine.org>',
    dist_abstract => 'A class based representation of an InterMine DB schema',
    create_readme => 1,
    recursive_test_files => 1,
    create_makefile_pl   => 'traditional',
    requires             => {
        'perl'                          => '5.8.3',
        'MooseX::Role::WithOverloading' => 0,
        'InterMine::TypeLibrary',       => 0.1,
        'MooseX::Types'                 => 0,
        'Moose'                         => 0.95,
        'XML::Parser::PerlSAX'          => 0.08,
    },
    build_requires => {
        'Test::Exception' => 0,
        'Test::More'      => 0,
    },
);
$build->create_build_script;
