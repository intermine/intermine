#!/usr/bin/perl

use strict;
use warnings;
use Carp qw(confess);
use Webservice::InterMine;
use InterMine::Model 0.9401;
use Webservice::InterMine::TemplateFactory;
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

$config->define(qw/oldserviceurl oldtemplates newmodel logfile changesfile/);
$config->file('resources/updater.config');
$config->getopt();
my $old_service_url = $config->oldserviceurl();
my $old_templates = $config->oldtemplates();
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
sub usage {
    print 
"Please define the keys 'newmodelfile' and 'oldserviceurl' in 'resources/config'\n";
    exit;
}
for ($old_service_url, $model_file,) {
    usage() unless $_;
}

my $model = InterMine::Model->new(file => $model_file);
my $template_factory;
my $service = Webservice::InterMine->get_service($old_service_url);
if ($old_templates) {
    $template_factory = Webservice::InterMine::TemplateFactory->new(
        service => $service,
        model => $model,
        source_file => $old_templates,
    );
} else {
    $template_factory = $service->_templates;
}

my $updater = Updater->new(
    model   => $model,
    logger  => $log,
    changes => $changes,
);
# And update, and print out the templates
print '<template-queries>';
for my $t ($template_factory->get_templates) {
    $t->suspend_validation;
    $t->{model} = $model;
    my ($updated_t, $is_broken) = $updater->update_query($t, $old_service_url);
    if ($is_broken) {
        $log->warning($t->name, "IS BROKEN:", $t->source_string);
    } else {
        print $updated_t->to_xml;
    }
}
print '</template-queries>';
exit;

