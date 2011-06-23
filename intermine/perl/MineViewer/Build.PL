use Module::Build;

my $build = Module::Build->new(
    module_name   => 'MineViewer',
    dist_abstract => 'A webapp for view data in intermine data-warehouses',
    license       => 'perl',
    dist_author   => 'Alex Kalderimis <dev@intermine.org>',
    create_readme => 1,
    recursive_test_files => 1,
    create_makefile_pl   => 'traditional',
    requires             => {
        'Carp'                        => 0,
        'Dancer'                      => 1.3,
        'Dancer::Plugin::DBIC'        => 0,
        'Dancer::Plugin::ProxyPath'   => 0.02,
        'DBIx::Class::Schema::Loader' => 0,
        'FindBin'                     => 0,
        'Lingua::EN::Inflect'         => 0,
        'Memoize'                     => 0,
        'Moose'                       => 2.0,
        'MooseX::NonMoose'            => 0,
        'namespace::autoclean'        => 0,
        'Plack::Builder'              => 0,
        'Plack::Handler::FCGI'        => 0,
        'Plack::Runner'               => 0,
        'Try::Tiny'                   => 0,
        'Webservice::InterMine'       => 0.9700,
        'Webservice::InterMine::Bio'  => 0.9700,
    },
    build_requires => {
        'Test::Exception' => 0,
        'Test::More'      => 0.9,
        'Dancer::Test'    => 0,
    },
);
$build->create_build_script;
