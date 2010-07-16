#!/usr/bin/perl

use strict;
use warnings;
use Carp;
use Getopt::Long;

## Modules to be installed
use Log::Handler;

my $DEL    = '[DELETION]';
my $CHANGE = '[CHANGE  ]';

my($log_file, @out_files, @in_files, $help, 
   $new_model_file, $changes_file);
my $lib = $ENV{HOME}.'/svn/dev';
my $result = GetOptions("logfile=s"       => \$log_file,
			"outputfile=s"    => \@out_files,
			"inputfile=s"     => \@in_files,
			"modelfile=s"     => \$new_model_file,
			"changesfile=s"   => \$changes_file,
			"help"            => \$help,
			"usage"           => \$help,
			"svndirectory=s"  => \$lib,
    );

@in_files = split(/,/, join (',', @in_files));
@out_files   = split(/,/, join (',', @out_files));

my $output_to_stdout;

if (@out_files == 1 and $out_files[0] eq '-') { # ie. the user wants redirection to stdout
    undef @out_files;
    push @out_files, '-' for @in_files;
    $output_to_stdout = 1;
}
		    
if (@out_files and @out_files != @in_files) {
    croak "The number of output files is not the same as the number of input files\n";
}			

sub usage {
    print for (<DATA>); 
}

if (not (@in_files and $new_model_file and $changes_file) 
    or  ($help)) {
    usage();
    exit;
}

die "Cannot direct output and logging to stdout at the same time,\n".
    "Please specify a destination file with --outputfile=[file],\n".
    "or a log file with --logfile=[file]\n" if ($output_to_stdout and not $log_file);

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
for my $in_file (@in_files) {
    my $out_file = (shift @out_files) || $in_file . '.new';
    open(my $INFH, '<', $in_file) or die "Cannot open $in_file, $!";
    my $OUT;
    if ($out_file eq '-') {
	$OUT = *STDOUT;
    }
    else {
	open($OUT,  '>', $out_file) or die "Cannot write to $out_file, $!";
    }
    print $OUT '-' x 70, "\n", $in_file, ":\n\n",  if $output_to_stdout;
    LINE: while(my $line = <$INFH>) {
	my ($changed, %type_of, $new_line,$path);
	if ($line =~ /^\s*[#\s]/ or $line =~ /max\.field\.values/) { #skip commented lines and lines beginning with a space
	    print $OUT $line;
	    next LINE;
	}
	chomp $line;
	my ($key, $value)   = split(/\s?=\s?/, $line, 2);

	if ($key =~ /precompute\.constructquery\.\d+/) {
	    my ($num)  = $key =~ /(\d+)/;
	    my @bits = split(/\s/, $value);
	    croak "Got an even number of bits - that ain't good" if (@bits % 2 == 0);
	    unshift(@bits, $bits[0]); # double up the first element, as the first bit is always its own class
	    my @old_bits = @bits;
	    map {s/\+//} @bits;
	    # produce a dotted path from the even indexed elements, and a type hash from the odd indexed ones
	    while (@bits) {
		$path          .= shift  @bits;
		$type_of{$path} = shift  @bits;
		$path          .= '.' if @bits; 
	    }

	    my @new_pathbits;
	    for my $old_path (%type_of) {
		my $new_path = update_path($old_path, $line);
		unless ($new_path) {
		    $log->info($DEL, $in_file, 'll.', $., qq{"$line":}, "could not find $old_path");
		    next LINE;
		}
	        push @new_pathbits, $new_path;
		unless ($new_path eq $old_path) {
		    $log->info($CHANGE, $in_file, 'll.', $., "Precompute construct query $num:", $old_path, '=>', $new_path);
		    $changed++;
		}
	    }
	    my %new_hash = @new_pathbits;
	    my (@new_bits, $c);
	    for (map {s/.*\.//; $_} 
		     map {($_, $new_hash{$_})} 
		         sort {length($a) <=> length($b)} 
		              keys %new_hash) {
		$_ = '+' . $_ if ($old_bits[$c++] =~ /\+/);
		push @new_bits, $_;
	    }
	    shift @new_bits;
	    $new_line = join(' = ', $key, join(' ', @new_bits));
	}
	elsif ($key =~ /precompute\.query/) {
	    my ($num) = $key =~ /(\d+)/;
	    my $old_value = $value;
	    my $prefix = 'org.intermine.model.bio';
	    my @definitions = $value =~ /($prefix[^_\s]+ AS \w+)/g;
	    for my $def (@definitions) {
		my ($full, $abbr) = split(/ AS /, $def);
		$value =~ s/$abbr/$full/g; # expand the abbreviations so we can assess fields such as a1_.type
	    }

	    my @old_paths = $value =~ /($prefix[A-Za-z\.]+)/g;
	    
	    my %updated_version_of;
	    for my $old_path (@old_paths) {
		my $new_path = update_path($old_path, "Precomputed query $num");
		unless ($new_path) {
		    $log->info($DEL, $in_file,  'll.', $., qq{"$line":}, "could not find $old_path");
		    next LINE;
		}
	        $updated_version_of{$old_path} = $new_path;
		unless ($new_path eq $old_path) {
		    $log->info($CHANGE, $in_file,  'll.', $., "Precomputed query $num:", $old_path, '=>', $new_path);
		    $changed++;
		}
	    }
	    
	    if ($changed) {
		for my $old_path (sort {length($b) <=> length($a)} @old_paths) {
		    $value =~ s/$old_path/$updated_version_of{$old_path}/;
		}
		for my $def (@definitions) {
		    my ($full, $abbr) = split(/ AS /, $def);
		    my $new_full = $updated_version_of{$full};
		    $value =~ s/$new_full(?! AS)/$abbr/g;
		}
	    }
	    else {
		$value = $old_value;
	    }
	    
	    $new_line = join(' = ', $key, $value);
	}
	else {
	    my @values     = split(/,?\s/, $value);
	    chomp @values;
	    my $class_name = my $guff = '';
	    if ($key =~ /\./) {
		($class_name, $guff) = $key =~ /(^.*)(\.\S*)/; # split into 'class.name' and '.fields'
	    }
	    else {
		$class_name = $key;
	    }
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
		    $log->info($DEL, $in_file, 'll.', $., qq{"$line":},"Cannot find class $class_name");
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
			$log->info($DEL, $in_file,'ll.',$.,$field_name, 'from',  qq{"$line":});
		    }
		    $changed++;
		}
		else {
		    push @new_values, $field_name;
		}
	    }
	    $new_line = $class_name . $guff .  ' = ' . join( (($value =~ /,/)?', ':' '), @new_values);
	    if ($changed) {
		$log->info($CHANGE, $in_file, 'll.', $., qq{"$line"}, '=>', qq{"$new_line"});
	    }
	}
	print $OUT $new_line, "\n";
    }
}

__DATA__

update_key_value_list.pl: Update InterMine configuration files to reflect changes in the data model

  Synopsis:
update_key_value_list.pl -i [file],[file] (-o [file],[file]) -m [file] -c [file.json] (-s [dir]) (-l [file])

This updater will read through an input file, checking the validity of 
any InterMine paths (classes or fields specified in the properties file), 
writing out the new updated version to a specified file, or STDOUT. 
It requires an example of the new data model, as well as a list of the changes 
between the old model and the current one.

The updater will attempt to transform paths wherever possible. If this is
not possible, then the line will be deleted. All changes and 
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

perl svn/dev/intermine/scripts/update_key_value_list.pl -m svn/model_update/flymine/dbmodel/build/model/genomic_model.xml -c svn/dev/intermine/scripts/resources/model_changes0.94.json -i svn/dev/flymine/dbmodel/resources/genomic_keyDefs.properties,svn/dev/flymine/dbmodel/resources/objectstoresummary.config.properties,svn/dev/flymine/dbmodel/resources/class_keys.properties,svn/dev/flymine/dbmodel/resources/genomic_precompute.properties -l /tmp/out.file -o -
