#!/usr/bin/perl

use strict;
use warnings;

use Log::Handler updater => "LOG";

use XML::Rules;
use Data::Dumper;

use JSON;

use XML::Writer;

use lib $ENV{HOME}.'/svn/dev/intermine/perl/lib';
use InterMine::Template;
use InterMine::Model;
use IMUtils::QueryUpdater;



use Number::Format qw(:subs);
use Getopt::Long;

my($logfile, $outfile, $inputfile, $help, $new_model_file, $changes_file);
my $result = GetOptions("logfile=s"      => \$logfile,
			"outputfile=s"   => \$outfile,
			"inputfile=s"    => \$inputfile,
			"modelfile=s"    => \$new_model_file,
			"changesfile=s"  => \$changes_file,
			"help"           => \$help,
			"usage"          => \$help,
    );

my $model = InterMine::Model->new(file => $new_model_file);

#my $log = Log::Handler->new();
if ($logfile) {
    LOG->add(
	file => {
	    filename => $logfile,
	    maxlevel => 'debug',
	    minlevel => 'emergency',
	}
    );
}
else {
    LOG->add(
	screen => {
	    log_to => 'STDOUT',
	    maxlevel => 'debug',
	    minlevel => 'emergency',
	}
    );
}

# Read the details of the model changes from the .json config file
die 'No model change details supplied - please list a file with the --changesfile flag ' unless $changes_file;
open my $changesFH, '<', $changes_file or die "Could not open $changes_file, $!";
my $content;
$content .= $_ for <$changesFH>;
close $changesFH or die "could not close $changes_file, $!";

# Decode it into a hash reference
my $json  = new JSON;
my $changes_href = $json->decode($content);

my $updater = IMUtils::QueryUpdater->new($changes_href, $model);

sub process {
    my ($name, $hash_ref, $writer) = @_;
     my %attr;
    my @subtags;
    while (my ($k, $v) = each %$hash_ref) {
	if(ref $v) {
	    push @subtags, map {{$k => $_}} @$v;
	}
	else {
	    $attr{$k} = $v;
	}
    }
    $writer->startTag($name => %attr);
    foreach my $subtag (@subtags) {
	process(each %$subtag, $writer);
    }
    $writer->endTag($name);
}

my @rules = (
    _default => 'no content array',
    'template' => sub {	
	my ($name, $hash_ref) = @_;
	my $xml;

	my $writer = XML::Writer->new(OUTPUT => \$xml, DATA_MODE => 1, DATA_INDENT => 2);
	process($name, $hash_ref, $writer);
	my $t = InterMine::Template->new(string => $xml, model => $model, no_validation => 1);
	my ($new_t, $is_broken) = $updater->update_query($t);
	my @sub_rules = (_default => 'no content array');
	my $sub_parser = XML::Rules->new(rules => \@sub_rules);
	my $processed;
	if ($is_broken) {
	    $processed = $sub_parser->parse($new_t->get_source_string);
	}
	else {
	    $processed = $sub_parser->parse($new_t->to_template_xml);
	}
	return ('@'.$name => @{$processed->{$name}});
    },
 );

my $parser = XML::Rules->new(rules => \@rules);

my $output = $parser->parsefile($inputfile);

open (my $xmlfh, '>', $outfile) or die "$!";
my $writer = XML::Writer->new(OUTPUT => $xmlfh, DATA_MODE => 1, DATA_INDENT => 2);

process('updated_profiles' => $output, $writer);


