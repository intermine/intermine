use strict;
use warnings;
use Module::Build;

my $builder = Module::Build->new(
    module_name        => 'Webservice::InterMine::Bio',
    license            => 'perl',
    dist_author        => q{Alex Kalderimis <dev@intermine.org>},
    dist_version_from  => 'lib/Webservice/InterMine/Bio.pm',
    build_requires     => { 'Test::More' => 0, },
    configure_requires => { 'Module::Build' => '0.36' },
    requires           => {
        Moose                   => '2.0',
        'PerlIO::gzip'          => 0,
        'File::Temp'            => 0,
        'URI'                   => 0,
        'IO::Handle'            => 0,
        'namespace::autoclean'  => 0,
        'Webservice::InterMine' => '0.9810',
    },
    recommends => {
        'Bio::SeqIO'                 => 0,
        'Bio::DB::SeqFeature::Store' => 0,
    },
    add_to_cleanup     => ['Webservice-InterMine-Bio-*'],
    create_makefile_pl => 'traditional',
);

$builder->create_build_script();
