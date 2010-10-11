use Module::Build;

my $build = Module::Build->new(
    module_name          => 'Webservice::InterMine',
    dist_abstract        => 'Modules for interacting with InterMine Webservice APIs',
    license              => 'perl',
    dist_author          => 'Alex Kalderimis <dev@intermine.org>',
    create_readme        => 1,
    recursive_test_files => 1,
    create_makefile_pl   => 'traditional',
    requires             => {
        'perl'                          => '5.8.3',
        'InterMine::Model',             => 0.9401,
        'InterMine::TypeLibrary',       => 0.9401,
        'List::MoreUtils'               => 0.22,
        'LWP'                           => 5.8,
        'Module::Find'                  => 0.05,
        'Moose'                         => 0.95,
        'MooseX::Role::WithOverloading' => 0.03,
        'MooseX::Types'                 => 0.22,
        'Text::CSV_XS'                  => 0.62,
        'URI'                           => 1.37,
        'XML::Parser::PerlSAX'          => 0.08,
        'XML::DOM'                      => 0,
    },
    recommends => {
        'YAML::Syck'              => 1.07,
        'Spreadsheet::WriteExcel' => 0,
        'Perl::Tidy'                => 20070801,
    },
    build_requires => {
        'Test::Class'               => 0,
        'Test::Exception'           => 0,
        'Test::More'                => 0.9,
        'Test::XML'                 => 0,
        'Test::MockObject'          => 0,
        'Test::MockObject::Extends' => 0,
        'XML::Rules'                => 0,
    },
);
$build->create_build_script;
