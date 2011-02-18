use Module::Build;

my $build = Module::Build->new(
    module_name   => 'Registry',
    license       => 'perl',
    dist_author   => 'Alex Kalderimis <alex@gmail.com>',
    dist_abstract => 'The InterMine archiving webapp',
    create_readme => 1,
    recursive_test_files => 1,
    create_makefile_pl   => 'traditional',
    requires             => {
        'perl'                          => '5.10.00',
        'perl5i::2'                     => 0,
        'Dancer'                        => 1.3,
        'Dancer::Plugin::ProxyPath'     => 0,
        'Dancer::Plugin::FormattedOutput' => 0,
        'Crypt::SaltedHash'             => 0,
        URI                             => 0,
        Template                        => 0,
        DateTime                        => 0,
        'DateTime::Format::ISO8601'     => 0,
        'File::Copy'                    => 0,
        'File::Slurp'                   => 0,
        Starman                         => 0.2,
        'Webservice::InterMine'         => '0.9501',
        'YAML'                          => 0,
    },
    build_requires => {
        'Test::More'      => 0,
    },
);
$build->create_build_script;
