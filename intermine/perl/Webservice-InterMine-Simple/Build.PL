use Module::Build;

my $build = Module::Build->new(
    module_name          => 'Webservice::InterMine::Simple',
    dist_abstract        => 'Low-Dependency Modules for interacting with InterMine Webservice APIs',
    license              => 'perl',
    dist_author          => 'Alex Kalderimis <dev@intermine.org>',
    create_readme        => 1,
    recursive_test_files => 1,
    create_makefile_pl   => 'traditional',
    configure_requires   => { 'Module::Build' => '0.36' },
    requires             => {
        'LWP'                           => 5.8,
    },
    build_requires => {
        'Test::More'                => 0.9,
    },
);
$build->create_build_script;
