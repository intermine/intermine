#!/usr/bin/perl
use strict;
use warnings;
use Carp qw(carp croak confess);

use encoding 'latin1';

# We want all warnings to be very very fatal
$SIG{__WARN__} = sub {
    confess @_;
};
$SIG{__DIE__} = sub {
    confess @_;
};
## Modules to be installed
use XML::Rules;
use XML::DOM;
use XML::Writer;
use Log::Handler;
use AppConfig qw(:expand :argcount);

## These should be installed or on the path before use
use InterMine::Model 0.9401;
use Webservice::InterMine::Query::Template;
use Webservice::InterMine::Query::Saved;

## This is required from its relative location
require 'resources/lib/updater.pm';

## Optional Module: Number:Format
BEGIN {
    # If Number::Format is not installed, don't format numbers
    eval "use Number::Format qw(format_number)";
    if ($@) {
        sub format_number {return @_};
    }
}

my $DEL     = '[DELETION]';
my $CHANGE  = '[CHANGE  ]';
my $nothing = '';
my $broken  = 1;
my $NEWLINE = "\n";
my $separator = '-' x 70 . $NEWLINE;

# Set up configured options 
my $config = AppConfig->new({
        CREATE => 1,
        GLOBAL => 
            {EXPAND   => EXPAND_ALL,
            ARGCOUNT => ARGCOUNT_ONE
        }
    });
$config->define('oldmodel', 'newmodel', 'changesfile', 'logfile', 'mine', 'svndirectory', 'ext');
$config->define('help|usage!');
$config->define('inputfile|infile=s@');

my $configfile = ($ARGV[0] || 'resources/updater.config'); 
$config->file($configfile) if (-f $configfile);
$config->getopt();

my $log_file       = $config->logfile();
my @in_files       = @{$config->inputfile()};
my $help           = $config->help();
my $new_model_file = $config->newmodel();
my $changes_file   = $config->changesfile();
my $ext            = $config->ext();

@in_files = split(/,/, join (',', @in_files));

sub usage {
    print for (<DATA>); 
}

if ($help or not ($new_model_file and $changes_file)) {
    usage();
    exit;
}

my $newmodel = InterMine::Model->new(file => $new_model_file);
# dummy service needed for instantiation of Templates/Saved-Queries
my $service = bless {}, 'Webservice::InterMine::Service'; 

# Set up logging, to screen if there is no file specified
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
open(my $items_by_owner, '>', $log_file . '.users') or die "$!";
        
my $updater = Updater->new(
    model => $newmodel,
    logger => $log,
    changes => $changes_file,
);


sub update_buffer {
    my ($buffer, $type, $origin, $owner) = @_;
    if ($type eq 'item' or $type eq 'class') {
        return translate_item($type, $buffer, $origin, $owner);
    }
    elsif ($type eq 'graphdisplayer') {
        return translate_graphdisplayer($type, $buffer, $origin, $owner);
    }
    elsif ($type eq 'template' or $type eq 'saved-query') {
        return translate_query($type, $buffer, $origin, $owner);
    }
    elsif ($type eq 'bag') {
        return check_bag_type($type, $buffer, $origin, $owner);
    }
    else {
        croak "Unknown item to update: '$type'";
    }
}

sub translate_query { # takes in xml, returns xml
    my ($name, $xml, $origin, $owner) = @_;
    my ($q, $ret_xml, $new_q, $is_broken);
    my %args = (source_string => $xml, model => $newmodel, service => $service, is_dubious => 1);
    if ($name eq 'template') {
        $q = eval{ Webservice::InterMine::Query::Template->new(%args)};
    }
    elsif ($name eq 'saved-query') {
        $q = eval{ Webservice::InterMine::Query::Saved->new(%args)};
    }
    my $stack_trace = $@;
    if ($q) {
        $q->suspend_validation;
        ($new_q, $is_broken) = $updater->update_query($q, $origin);
#        unless ($is_broken) {
            $ret_xml  = eval {$new_q->to_xml}; # it still might break
#        }
        if ($@) {
            $is_broken = $@;
            $log->warning('broken query:', $is_broken, $origin);
        }
        $ret_xml = $xml unless $ret_xml;
    } else {
        $log->warning("Something is wrong with this query: please see stack trace below - $origin\n$xml\n$stack_trace");
        $ret_xml = $xml;
        $is_broken = $stack_trace;
    }
    if ($owner and defined $is_broken) {
        my $query_name = eval {$q->name} || 'UNKNOWN';
        if ($is_broken) {
            print $items_by_owner join($NEWLINE, $owner, $name, $query_name, $is_broken, $separator);
        } else {
            print $items_by_owner join($NEWLINE, $owner, $name, $query_name, 'IS_UPDATED', $separator);
        }
    }
    return $ret_xml . "\n", $is_broken;
}

sub translate_graphdisplayer {
    my ($type, $xml, $origin) = @_;
    my $naked = undress($type, $xml);
    my $id = $naked->{id};
    my (@new_class_names, $changed);
    my @class_names = split /,/, $naked->{typeClass};
    for my $old_class_name (@class_names) {
        my $new_class_name = $updater->update_path($old_class_name);
        if ($new_class_name) {
            unless ($new_class_name eq $old_class_name) {
                $log->info($CHANGE, $origin, $type, ':',
                    $old_class_name, '=>', $new_class_name);
                $changed++;
            }
            push @new_class_names, $new_class_name;
        }
        else {
            if ($updater->knows_about_deletion_of($old_class_name)) {
                $log->info($DEL, $origin, 'anticipated deletion in', $type, $id, 
                    ': could not find', $old_class_name);
            }
            else {
                $log->warning($DEL, $origin, 'unexpected deletion in', $type, $id, 
                    ': could not find', $old_class_name);
            }
            $changed++;
        }
    }
    unless (@new_class_names) {
        $log->warning($DEL, $origin, $type, $id, 
            'is broken - all typeclasses have been deleted');
        return $nothing, $broken;
    }
    $naked->{typeClass} = join ',', @new_class_names;
    return  dress($type,$naked), ($changed)? 0 : undef;
}

sub check_bag_type {
    my ($type, $xml, $origin, $owner) = @_;
    my $naked = undress($type, $xml);
    my $bag_name = $naked->{name};
    my $bag_type = $naked->{type};
    my $changed;
    my $translation = $updater->update_path($bag_type);
    if ($translation) {
        unless ($bag_type eq $translation) {
            $naked->{type} = $translation;
            $log->info($CHANGE, $origin, $type, ':', $bag_type, '=>', $translation);
            $changed++;
        }
    } else {
        if ($updater->knows_about_deletion_of($bag_type)) {
            $log->info($CHANGE, $origin, 'anticipated deletion of', $type, $bag_name, 
                ': This type has been deleted in the model -', $bag_type);
        }
        else {
            $log->warning($DEL, $origin, 'unexpected deletion of', $type, $bag_name,
                ': could not find in the model', $bag_type);
        }
        return $nothing, $broken;
        if ($owner) {
            my $message = "Your list has been deleted because this data type has been removed from our data model";
            print $items_by_owner join($NEWLINE, $owner, 'list', $naked->{name}, $message, $separator);
        }
    }
    return dress($type, $naked), ($changed) ? 0 : undef;
}

my $dom_parser = XML::DOM::Parser->new();

sub translate_item { # takes in xml, returns xml
    my ($type, $xml, $origin) = @_;
    my $doc = $dom_parser->parsestring($xml);
    my $root = $doc->getDocumentElement();
    my ($class_name, $class_key, $id, $id_key, $class, $changed); 

    if ($type eq 'item') {
	$class_key = 'implements';
	$id_key = 'id';
    }
    elsif ($type eq 'class') {
        $class_key = "className";
        $id_key         = "className";
    }
    else {
        croak "This sub doesn't only does classes and items: I got $type";
    }
    $class_name = $root->getAttribute($class_key);
    $id         = $root->getAttribute($id_key);

    unless ($class = $updater->get_class($class_name)) {
        my $translation = $updater->update_path($class_name);

        if ($translation and $class = $updater->get_class($translation)) {
            $log->info($CHANGE, $origin, $type, $id, ':', $class_name, '=>', $translation);
	    $root->setAttribute($class_key, $translation);
            $changed++;
        }
        else {
            if ($updater->knows_about_deletion_of($class_name)) {
                $log->info($DEL, $origin, 'anticipated deletion of', $type, $id, 
                    ': could not find', $class_name);
            }
            else {
                $log->warning($DEL, $origin, 'unexpected deletion of', $type, $id, 
                    ': could not find', $class_name);
            }
	    $doc->dispose();
            return $nothing, $broken;
        }
    }

    foreach my $field ($root->getChildNodes()) {
	next unless ($field->getNodeType() == ELEMENT_NODE);
	my $field_name = $field->getAttribute("name");
#        my $field_ref = ($field->{name}) ? \$field->{name} : \$field->{fieldExpr};
        my $path = join '.', $class->name, $field_name;
        my $translation = $updater->update_path($path);
        if (defined $translation) {
            my $cn = $class->name;
            $translation =~ s/$cn\.//; # strip off the classname
            unless ($translation eq $field_name) {
                $log->info($CHANGE, $origin, $type, $id, ':', 
                    "$path => $translation");
                $changed++;
            }
            $field->setAttribute("name", $translation);
        }
        else {
            $log->warning($DEL, $origin, $type, $id, 
                ": Broken by deletion of $path");
            return $nothing, $broken;
        }
    }
    my $string = $doc->toString();
    $doc->dispose();
    return $string, ($changed)? 0 : undef;
}	

my $parser = XML::Rules->new(rules => [_default => 'no content array']);

sub undress { # takes xml and extracts the content
    my ($type, $xml) = @_;
    return $parser->parse($xml)->{$type}[0];
}

sub dress { # takes content and straps on the xml
    my ($name, $hash_ref, $writer) = @_;
    my %attr;
    my @subtags;
    my $processed_element;
    $writer = XML::Writer->new(
        OUTPUT     => \$processed_element,
        DATA_MODE  => 1,
        DATA_INDENT => 3,
    ) unless $writer;
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
        dress(each %$subtag, $writer);
    }
    $writer->endTag($name);
    return $processed_element;
}

sub process_precomputequery {
    my $changed;
    chomp (my $value = shift);
    my $err          = [];
    my $old_value    = $value;
    my $prefix       = 'org.intermine.model.bio';
    my @definitions  = $value =~ /($prefix[^_\s]+ AS \w+)/g;

    for my $def (@definitions) {
        my ($full, $abbr) = split(/ AS /, $def);
        # expand the abbreviations so we can assess fields such as a1_.type
        $value =~ s/$abbr/$full/g; 
    }

    my @old_paths = $value =~ /($prefix[A-Za-z\.]+)/g;

    my %updated_version_of;
    for my $old_path (@old_paths) {
        my $new_path = $updater->update_path($old_path, '');
        unless ($new_path) {
            return (undef, "could not find $old_path");
        }
        $updated_version_of{$old_path} = $new_path;
        unless ($new_path eq $old_path) {
            push @$err, "$old_path => $new_path";
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

    return ($value, $err);
}
sub process_constructquery {
    my $err   = [];
    my $changed;
    my $value = shift;
    my @bits  = split(/\s/, $value);
    croak "Got an even number of bits - that ain't good" if (@bits % 2 == 0);

    # double up the first bit, which is its own class
    unshift(@bits, $bits[0]);
    my @old_bits = @bits;
    map {s/\+//} @bits;

    # produce a dotted path from the even indexed elements, 
    # and a type hash from the odd indexed ones
    my ($path, %type_of);
    while (@bits) {
        $path          .= shift  @bits;
        $type_of{$path} = shift  @bits;
        $path          .= '.' if @bits; 
    }

    my @new_pathbits;
    for my $old_path (%type_of) {
        my $new_path = $updater->update_path($old_path, '');
        unless ($new_path) {
            return (undef, "could not find $old_path");
        }
        push @new_pathbits, $new_path;
        unless ($new_path eq $old_path) {
            push @$err, "$old_path => $new_path";
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
    return (join(' ', @new_bits), $err);
}

sub process_field_list {
    my $changed;
    my ($key, $value) = @_;
    my $err           = [];
    my @values        = split(/,?\s/, $value);
    chomp @values;
    my $class_name = my $guff = '';
    if ($key =~ /\./) {
        # split into 'class.name' and '.fields'
        ($class_name, $guff) = $key =~ /(^.*)(\.\S*)/;
    }
    else {
        $class_name = $key;
    }
    my $class = $updater->get_class($class_name);
    unless ($class) {
        my $new_class_name = $updater->update_path($class_name, '');
        if ($new_class_name 
                and $new_class_name ne $class_name
                and $class = $updater->get_class($new_class_name) ) {
            $changed++;
            push @$err, "$class_name => $new_class_name";
            $class_name = $new_class_name;
        }
        else {
            return (undef, undef, "class $class_name not in the model");
        }
    }
    my @new_values;
    foreach my $field_name (@values) {
        $field_name =~ s/^\s*//;
        chomp $field_name;
        if (not $class->get_field_by_name($field_name)) {
            if (my $new_path = $updater->update_path($class_name . '.' . $field_name, '')) {
                ($field_name) = $new_path =~ /([^\.]*$)/;
                push @new_values, $field_name;
            }
            else {
                push @$err, "$field_name deleted";
            }
            $changed++;
        }
        else {
            push @new_values, $field_name;
        }
    }
    my $new_key   = $class_name . $guff;
    my $new_value = join( (($value =~ /,/)?', ':' '), @new_values);
    return ($new_key, $new_value, $err);
}

sub process_key_value_line {
    my $line = shift;
    my $file = shift;
    #skip commented lines and lines beginning with a space
    if ($line =~ /^\s*[#\s]/ or $line =~ /max\.field\.values/) { 
        return $line;
    }

    chomp $line;
    my ($key, $value)   = split(/\s?=\s?/, $line, 2);
    my $err;
    if ($key =~ /precompute\.constructquery\.(\d+)/) {
        ($value, $err) = process_constructquery($value);
    }
    elsif ($key =~ /precompute\.query\.(\d+)/) {
        ($value, $err) = process_precomputequery($value);
    }
    else {
        ($key, $value, $err) = process_field_list($key, $value);
    }

    if (not defined $value) {
        my @complaint = ($DEL, 'from', $file, 'l.', $., qq{"$line":}, $err);
        $log->info(@complaint);
        return '';
    }
    else {
        if ($err) {
            $log->info($CHANGE, 'in', $file, 'l.', $., ':', $_) for (@$err);
        }
    }
    return join(' = ', $key, $value) . "\n";
}


my ($buffer, $is_buffering, @open_tags, %counter);
my %needs_processing = map {($_ => 1)} qw(
    item template saved-query
    graphdisplayer class
    bag
);

my $owner = '';
sub process_xml_line {
    my ($line, $file) = @_;
    my $new_line = '';
    $line   =~ s/></>><</g;
    my @elems = split('><', $line);
    while (my $elem = shift @elems) {	    
        my ($end, $type) = $elem =~ m!^\s*<(/?)([a-z-]+)[\s>]!i;
        if ($type) {
            if ($type eq 'userprofile') {
              ($owner) = $elem =~ /username="([^"]*)"/;
            }
            push @open_tags, $type;
            $counter{$type}++ unless $end;  
            $counter{total}++;
            printf "\rProcessing element %10s", format_number($counter{total});
            $is_buffering = 1 if ($needs_processing{$type});
        }
        if ($is_buffering) {
            $buffer .= $elem;
        }
        else {
            $new_line .= $elem;
        }
        if ($elem =~ m{/>\s*$} or $end) { # a closed contentless tag
            $end  = pop @open_tags;
            $type = $end unless $type;
        }
        if ($type and $needs_processing{$type} and $end) {
            undef $is_buffering;
            my $origin = qq{$file ll. $.};
            $origin .= qq{ in profile of "$owner"} if $owner;
            my ($updated_buffer, $changes) = update_buffer($buffer, $type, $origin, $owner);
            $new_line .= $updated_buffer;
            undef $buffer;
            if (defined $changes) {
                if ($changes) {
                    $counter{broken}{$type}++;
                }
                else {
                    $counter{changed}{$type}++;
                }
            } 
            else {
                $counter{unchanged}{$type}++;
            }
        }
    }
    return $new_line;
}

### MAIN ROUTINES

### PROCESS THE INPUT FILES 
die "No input files specified! Please supply some using --inputfile or -i\n"
unless @in_files;
for my $file (@in_files) {
    my $backup = $file . $ext;
    if (-f $backup) {
        die "Back up file ($backup) exists - we do not want to overwrite it";
    }
    open(my $INFH, '<', $file) 
        or (warn "\rCannot open $file, $! - skipping\n" and next);
    $log->info("Processing file $file");
    rename($file, $backup);
    print STDERR "Backing up $file to $backup\n";
    open(my $OUT,  '>', $file) or die "Cannot write to $file, $!";
    
    while(<$INFH>) {
        my $new_line;
        if ($file =~ /\.xml$/) {
            $new_line = process_xml_line($_, $file);
        }
        else {
            $new_line = process_key_value_line($_, $file);
        }

        print $OUT $new_line;
    }
    close $INFH or die "Cannot close in file $!";
    close $OUT or die "Cannot close out file $!";
}

### REPORT THE RESULTS

if (%counter) {
    $log->info('Processed', format_number($counter{total}), 'elements');
    for (sort keys %needs_processing) {
        my $tag = $_;
        $tag =~ s/y$/ie/;
        $tag =~ s/s$/se/;
        my $msg = sprintf(
            "Processed %8s %-12s: %s unchanged, %s broken, %s changed",
            format_number($counter{$_} || 0),
            $tag.'s',
            format_number($counter{unchanged}{$_} || 0),
            format_number($counter{broken}{$_}    || 0),
            format_number($counter{changed}{$_}   || 0),
        );
        $log->info($msg);
    }
    print "\n";
}

exit()

__DATA__

update_key_value_list.pl: Update InterMine configuration files to reflect changes in the data model

  Synopsis:

  intermine_updater.pl ([config_file])

OR

  intermine_updater.pl -i [file],[file] (-o [file],[file]) -m [file] -c [file.json] (-s [dir]) (-l [file])

This updater will read through an input file, checking the validity of 
any InterMine paths (classes or fields specified in the properties file), 
writing out the new updated version to a specified file, or a new file
composed of the old filename plus a prefix (the default is ".new") or STDOUT. 
It requires an example of the new data model, as well as a list of the changes 
between the old model and the current one.

The updater will attempt to transform paths wherever possible. If this is
not possible, then the line will be deleted. All changes and 
deletions will be logged.

  Options:

--help|usage   | -h|u : This help text

--inputfile    | -i   : The file(s) to be processed

--outputfile   | -o   : The file(s) to write the new output to
   (optional)           if not supplied, the inputfile name 
                        will be used, suffixed with '.new'

--modelfile    | -m   : The new model to validate paths against

--changesfile  | -c   : The file specifying model changes (deletions
                        and name changes)
--svndirectory | -s   : The location of the InterMine svn directory,
    (optional)          by default this is assumed to be "~/svn/dev"

--logfile      | -l   : File to save the log to. If there is no file, 
    (optional)          all logging output will go to STDOUT

for options that accept multiple values (-i/-o) the values can either be 
supplied by multiple recurrances of the switch, or as comma separated lists,
or as a combination of the two. For example, the following are all equivalent:

  --inputfile file1,file2,file3,file4

  -i file1 -i file2 -i file3 -i file4

  -i file1,file2 --inputfile file3,file4

Make sure, if you specify output files, that you have the same number of files
in both the input and output lists, and that they are in the same sequence. The
updater will throw an error if the lists are of different lengths, but getting
the order wrong will just give you headaches down the line. Checking the output
visually is always recommended.

Writing to standard output is supported (simply supply "-" as the output file for that file, or just a single "-" if you want all output to standard output) but modification in place is not. 

Trying to write output and the log to standard output at the same time will 
throw an error.

  Configuration:

The updater can either be run using commandline switches or a configuration file, or a combination of the two.

The configuration file options are the same as the long forms of the command-
line flags. For example:

  model = path/to/modelfile
  changes = path/to/changesfile
  inputfile = list,of,input,files
  inputfile = another input file

An example of a configuration file is provided in: 
  
  intermine/resources/updater.config

  Example:

if you are running the updater from the intermine/scripts directory, then you can just call it as:

  ./intermine_updater.pl

and, assuming you have configured resources/updater.config, it should just work.

Alternatively, you can call it using command line options, or a combination of the two.

Both abbreviated and full commandline flags are allowed, and they are case-insensitive

perl path/to/intermine_updater.pl -m path/to/model/genomic_model.xml -c path/to/model_changes0.94.json -i path/to/class_keys.properties,path/to/genomic_precompute.properties -l /tmp/out.file -o -

OR 

path/to/intermine_updater.pl --model path/to/model/genomic_model.xml --changes path/to/model_changes0.94.json --inputfile path/to/class_keys.properties --inputfile path/to/genomic_precompute.properties --logfile /tmp/out.file --outputfile -
