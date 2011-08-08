use Module::Build;

my $build = Module::Build->new(
    module_name   => 'InterMine::Model',
    license       => 'perl',
    dist_author   => 'Alex Kalderimis <dev@intermine.org>',
    dist_abstract => 'A class based representation of an InterMine DB schema',
    recursive_test_files => 1,
#    create_makefile_pl   => 'traditional', # No makefiles until the version of Test::More is sane
    requires             => {
        'perl'                          => '5.8.3',
        'Carp'                          => 0,
        'Moose::Util::TypeConstraints'  => 2.0,
        'MooseX::Role::WithOverloading' => 0.09,
        'MooseX::Types'                 => 0,
        'Moose'                         => 2.0,
        'Scalar::Util'                  => 0,
        'XML::Parser::PerlSAX'          => 0.08,
        'XML::Parser::Expat'            => 0,
        'Time::HiRes'                   => 0,
    },
    build_requires => {
        'Test::Exception' => 0,
        'Test::More'      => 0.95,
    },
);
$build->add_build_element('xml');
$build->create_build_script;
