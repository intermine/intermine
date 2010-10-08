#!/usr/bin/perl

use strict;
use warnings;
use Carp qw(confess);
use Webservice::InterMine;
use InterMine::Model 0.9401;
use Log::Handler;
require 'resources/lib/updater.pm';
use AppConfig qw(:expand :argcount);


my $config = AppConfig->new({
        CREATE => 1,
        GLOBAL => {
            EXPAND   => EXPAND_ALL,
            ARGCOUNT => ARGCOUNT_ONE,
        }
    });

$config->file('resources/updater.config');
$config->getopt();

my $old_service_url = $config->oldserviceurl();
my $model_file = $config->newmodel();
my $log_file = $config->logfile();
my $changes = $config->changesfile();
my $output_file = shift;

my $OUT;
if ($output_file) {
    open($OUT, '>', $output_file) 
        or confess "Could not open $output_file for writing, $!";
}
select $OUT if ($output_file);
die "Log file not defined" if (not defined $log_file);
my $log = Log::Handler->new();
$log->add(
    file => {
        filename => $log_file,
        maxlevel => 'debug',
        minlevel => 'emergency',
        mode     => 'append',
        newline  => 1,
    }
);

for ($old_service_url, $model_file,) {
    usage() unless $_;
}

my $model = InterMine::Model->new(file => $model_file);
my $service = Webservice::InterMine->get_service($old_service_url);

my $updater = Updater->new(
    model   => $model,
    logger  => $log,
    changes => $changes,
);

print '<templates>';
for my $t ($service->get_templates) {
    my $xml = $t->source_string;
    $t->{model} = $model;
    my ($updated_t, $is_broken) = $updater->update_query($t, $old_service_url);
    unless ($is_broken) {
        $xml = $updated_t->to_xml;
    }
    print $xml;
}
print '</templates>';
exit;

