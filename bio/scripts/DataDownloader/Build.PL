use strict;
use warnings;
use Module::Build;

my $builder = Module::Build->new(
    module_name         => 'DataDownloader',
    license             => 'lgpl',
    dist_author         => q{Alex Kalderimis <perl@intermine.org>},
    dist_version_from   => 'lib/DataDownloader.pm',
    requires => {
        'Moose' => '2.0',
        'MooseX::ABC' => '0.05',
        'MooseX::FollowPBP' => '0.05', 
        'MooseX::FileAttribute' => 0,
        'Net::FTP' => 0,
        'Log::Handler' => 0,
        'DateTime' => 0,
        'Module::Find' => 0,
        'Web::Scraper' => 0, 
        'Ouch' => 0,
        'Number::Format' => 0,
        'PerlIO::gzip' => 0,
        'Perl6::Junction' => 0,
    },
    build_requires => {
        'Test::More' => 0,
    },
    add_to_cleanup      => [ 'DataDownloader-*' ],
    create_makefile_pl => 'traditional',
);

$builder->create_build_script();
