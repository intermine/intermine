#!/usr/bin/perl

use strict;
use warnings;
use Carp;
use Getopt::Long;

## Modules to be installed
use Log::Handler;

my $DEL    = '[DELETION]';
my $CHANGE = '[CHANGE]';

my($log_file, $out_file, $in_file, $help, 
   $new_model_file, $changes_file);
my $lib = $ENV{HOME}.'/svn/dev';
my $result = GetOptions("logfile=s"       => \$log_file,
			"outputfile=s"    => \$out_file,
			"inputfile=s"     => \$in_file,
			"modelfile=s"     => \$new_model_file,
			"changesfile=s"   => \$changes_file,
			"help"            => \$help,
			"usage"           => \$help,
			"svndirectory=s"  => \$lib,
    );


sub usage {
    print for (<DATA>); 
}

if (not ($in_file and $new_model_file and $changes_file) 
    or  ($help)) {
    usage();
    exit;
}

die "Cannot direct output and logging to stdout at the same time,\n".
    "Please specify a destination file with --outputfile=[file],\n".
    "or a log file with --logfile=[file]\n" unless ($out_file or $log_file);

# use the InterMine libraries from the use defined (or default) directory
eval qq{
use lib "$lib/intermine/perl/lib";
use InterMine::Model;
use IMUtils::UpdatePath;
};
croak "$@" if ($@);

my $model = InterMine::Model->new(file => $new_model_file);

# Set up logging, to screen if there is no file specified
my $log = Log::Handler->new();
if ($log_file) {
    $log->add(
	file => {
	    filename => $log_file,
	    maxlevel => 'debug',
	    minlevel => 'emergency',
	}
	);
}
else {
    $log->add(
	screen => {
	    log_to => 'STDOUT',
	    maxlevel => 'debug',
	    minlevel => 'emergency',
	}
    );
}

IMUtils::UpdatePath->set_up(model => $model, log => $log, changes => $changes_file);


open(my $INFH, '<', $in_file) or die "Cannot open $in_file, $!";
my $OUFH;
if ($out_file) {
    open($OUFH, '>', $out_file) or die "Cannot redirect stdout to $out_file, $!";
}
else {
    open($OUFH, '>>', \*STDERR) or die "Cannot write output to stdout, $!";
}

LINE: while(my $line = <$INFH>) {
    my $changed;
    if ($line =~ /^\s*[#\s]/) { #skip commented lines and lines beginning with a space
	print $OUFH $line;
	next LINE;
    }
    chomp $line;
    my ($key, $value)         = split('=', $line);
    my @values                = split(/,\s*/, $value);
    my ($class_name, $keydef) = $key =~ /(^[^\.]*)(.*)/;

    my $class = check_class_name($class_name);
    unless ($class) {
	my $new_class_name = update_path($class_name, $line);
	if ($new_class_name 
	    and $new_class_name ne $class_name
	    and $class = check_class_name($new_class_name) ) {
	    $changed++;
	    $class_name = $new_class_name;
	}
	else {
	    $log->warning($DEL, $line,"Cannot find class $class_name");
	    next LINE;
	}
    }
    my @new_values;
    foreach my $field_name (@values) {
	$field_name =~ s/^\s*//;
	chomp $field_name;
	if (not $class->get_field_by_name($field_name)) {
	    if (my $new_path = update_path($class_name . '.' . $field_name, $line)) {
		($field_name) = $new_path =~ /([^\.]*$)/;
		push @new_values, $field_name;
	    }
	    else {
		$log->warning($DEL, $field_name, 'from', $line);
	    }
	    $changed++;
	}
	else {
	    push @new_values, $field_name;
	}
    }
    my $new_line = $class_name . $keydef . '=' . join(', ', @new_values);
    if ($changed) {
	$log->info($CHANGE, $line, '=>', $new_line);
    }
    print $OUFH $new_line, "\n";

}

__DATA__

update_keydefs.pl: Update InterMine configuration files to reflect changes in the data model

  Synopsis:
update_keydefs.pl -i [file],[file] (-o [file],[file]) -m [file] -c [file.json] (-s [dir]) (-l [file])

This updater will read through an input file, checking the validity of 
any InterMine paths (classes or fields specified in the keyDefs.properties file), 
writing out the new updated version to a specified file, or STDOUT. 
It requires an example of the new data model, as well as a list of the changes 
between the old model and the current one.

The updater will attempt to transform paths wherever possible. If this is
not possible, then classes or fields will be deleted. All changes and 
deletions will be logged.

  Options:

--help|usage   | -h|u : This help text

--inputfile    | -i   : The file to be processed

--outputfile   | -o   : The file to write the new output to
   (optional)           if not supplied, the inputfile name 
                        will be used, suffixed with '.new'

--modelfile    | -m   : The new model to validate paths against

--changesfile  | -c   : The file specifying model changes (deletions
                        and name changes)
--svndirectory | -s   : The location of the InterMine svn directory,
    (optional)          by default this is assumed to be "~/svn/dev"

--logfile      | -l   : File to save the log to. If there is no file, 
    (optional)          all logging output will go to STDOUT

  Example:

perl svn/dev/intermine/scripts/update_keydefs.pl -m svn/model_update/flymine/dbmodel/build/model/genomic_model.xml -c svn/dev/intermine/scripts/resources/model_changes0.94.json -i svn/dev/flymine/dbmodel/build/model/genomic_keyDefs.properties -l log/keydefs.log
