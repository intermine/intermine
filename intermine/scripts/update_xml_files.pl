#!/usr/bin/perl

## Core Modules
use strict;
use warnings;
use Carp;
use Getopt::Long;

## Modules to be installed
use Log::Handler;
use XML::Rules;
use XML::Writer;

## Optional Module: Number:Format
BEGIN {
    # If Number::Format is not installed, don't format numbers
    eval "use Number::Format qw(format_number)";
    if ($@) {
	sub format_number {return @_};
    }
}

my $nothing  = '';
my $broken   = 1;

my($logfile, @outfiles, @inputfiles, $help, 
   $new_model_file, $changes_file);
my $lib = $ENV{HOME}.'/svn/dev';
my $result = GetOptions("logfile=s"       => \$logfile,
			"outputfile=s"    => \@outfiles,
			"inputfile=s"     => \@inputfiles,
			"modelfile=s"     => \$new_model_file,
			"changesfile=s"   => \$changes_file,
			"help"            => \$help,
			"usage"           => \$help,
			"svndirectory=s"  => \$lib,
    );

@inputfiles = split(/,/, join (',', @inputfiles));
@outfiles   = split(/,/, join (',', @outfiles));
		    
if (@outfiles and @outfiles != @inputfiles) {
    croak "The number of output files is not the same as the number of input files\n";
}			

sub usage {
    print for (<DATA>); 
}

if (not (@inputfiles and $new_model_file and $changes_file) 
    or  ($help)) {
    usage();
    exit;
}

# use the InterMine libraries from the use defined (or default) directory
eval qq{
use lib "$lib/intermine/perl/lib";
use InterMine::Template;
use InterMine::SavedQuery;
use InterMine::Model;
use IMUtils::UpdatePath;
};
croak "$@" if ($@);

my $model = InterMine::Model->new(file => $new_model_file);

# Set up logging, to screen if there is no file specified
my $log = Log::Handler->new();
if ($logfile) {
    $log->add(
	file => {
	    filename => $logfile,
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

sub update {
    my ($buffer, $type) = @_;
    if ($type eq 'item' or $type eq 'class') {
	return translate_item($type, $buffer);
    }
    elsif ($type eq 'graphdisplayer') {
	return translate_graphdisplayer($type, $buffer);
    }
    elsif ($type eq 'template' or $type eq 'saved-query') {
	return translate_query($type, $buffer);
    }
    else {
	croak "Unknown item to update: '$type'";
    }
}

sub translate_query { # takes in xml, returns xml
    my ($name, $xml) = @_;
    my $q;
    my %args = (string => $xml, model => $model, no_validation => 1);
    if ($name eq 'template') {
	$q = InterMine::Template->new(%args);
    }
    elsif ($name eq 'saved-query') {
	$q = InterMine::SavedQuery->new(%args);
    }
    my ($new_q, $is_broken) = update_query($q);
    my $ret;
    unless ($is_broken) {
	$ret  = eval {$new_q->to_xml_string}; # it still might break
    }
    if ($@) {
	$log->warning('broken query:', "$@");
	$is_broken++;
    }
    $ret = $new_q->get_source_string unless $ret;
    return $ret, $is_broken;
}

sub translate_graphdisplayer {
    my ($type, $xml) = @_;
    my $naked = undress($type, $xml);
    my $id = $naked->{id};
    my (@new_class_names, $changed);
    my @class_names = split /,/, $naked->{typeClass};
    for my $old_class_name (@class_names) {
	my $new_class_name = update_path($old_class_name);
	if ($new_class_name) {
	    unless ($new_class_name eq $old_class_name) {
		$log->info('[CHANGE]', $type, ':',
			   $old_class_name, '=>', $new_class_name);
		$changed++;
	    }
	    push @new_class_names, $new_class_name;
	}
	else {
	    if (dead($old_class_name)) {
		$log->info('[DELETION] anticipated deletion in', $type, $id, 
			      ': could not find', $old_class_name);
	    }
	    else {
		$log->warning('[DELETION] unexpected deletion in', $type, $id, 
			      ': could not find', $old_class_name);
	    }
	    $changed++;
	}
    }
    unless (@new_class_names) {
	$log->warning('[DELETION]', $type, $id, 
		      'is broken - all typeclasses have been deleted');
	return $nothing, $broken;
    }
    $naked->{typeClass} = join ',', @new_class_names;
    return  dress($type,$naked), ($changed)? 0 : undef;
}

sub translate_item { # takes in xml, returns xml
    my ($type, $xml) = @_;
    my $hash_ref = undress($type, $xml);
    delete($hash_ref->{_content});
    my ($class_name, $id, $class, $changed); 

    if ($type eq 'item') {
	$class_name = \$hash_ref->{implements};
	$id         = $hash_ref->{id};
    }
    elsif ($type eq 'class') {
	$class_name = \$hash_ref->{className};
	$id           = $hash_ref->{className};
    }
    else {
	croak "This sub doesn't only does classes and item: I got $type";
    }

    unless ($class = check_class_name($$class_name)) {
	my $translation = update_path($$class_name);

	if ($translation and $class = check_class_name($translation)) {
	    $log->info('[CHANGE]', $type, $id, ':', $$class_name, '=>', $translation);
	    $$class_name = $translation;
	    $changed++;
	}
	else {
	    if (dead($$class_name)) {
		$log->info('[DELETION] anticipated deletion of', $type, $id, 
			      ': could not find', $$class_name);
	    }
	    else {
		$log->warning('[DELETION] unexpected deletion of', $type, $id, 
			      ': could not find', $$class_name);
	    }
	    return $nothing, $broken;
	}
    }
    
    foreach my $field (@{$hash_ref->{attribute}}, 
		       @{$hash_ref->{reference}},
	               @{$hash_ref->{fields}[0]{fieldconfig}}) {
	my $field_ref = ($field->{name}) ? \$field->{name} : \$field->{fieldExpr};
	my $path = join '.', $class->name, $$field_ref;
	my $translation = update_path($path, $id);
	if (defined $translation) {
	    my $cn = $class->name;
	    $translation =~ s/$cn\.//; # strip off the classname
	    unless ($translation eq $$field_ref) {
		$log->info('[CHANGE]', $type, $id, ':', 
			   "$path => $translation");
		$changed++;
	    }
	    $$field_ref   = $translation;
	}
	else {
	    $log->warning('[DELETION]' , $type, $id, 
			  ": Could not find $path");
	    return $nothing, $broken;
	}
    }
    return dress($type,$hash_ref), ($changed)? 0 : undef;
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

sub main {
    my ($buffer, $is_buffering, @open_tags,  %counter);
    my @elems_to_process = qw(item template saved-query
                              graphdisplayer class);
    for my $in_file (@inputfiles) {
	my $out_file = (shift @outfiles) || $in_file . '.new';
	open (my $INFH, '<', $in_file) or die "cannot open $in_file, $!";
	open (my $OUTFH, '>', $out_file)  or die "cannot open $out_file, $!";
	
	while (my $line = <$INFH>) {
	    $line =~ s/></>><</g;
	    my @elems;
	    @elems = split('><', $line);
	    while (my $elem = shift @elems) {
		
		my ($end, $type) = $elem =~ m!^\s*<(/?)([a-z-]+)[\s>]!i;
		if ($type) {
		    push @open_tags, $type;
		    $counter{$type}++ unless $end;  
		    $counter{total}++;
		    printf "\rProcessing element %8d", $counter{total} 
		    if $logfile;
		    
		    if (grep {/$type/} @elems_to_process) {
		    $is_buffering = 1;
		    }
		}
		
		if ($is_buffering) {
		    $buffer .= $elem;
		}
		else {
		    $elem =~ s/([^\n]$)/$1\n/;
		    print $OUTFH $elem;
		}
		if ($elem =~ m!/>\s*$!) { # a closed contentless tag
		    $type = pop @open_tags;
		    $end  = 1;
		}

		if ($type and grep {$_ eq $type} @elems_to_process and $end) {
		    undef $is_buffering;
		    my ($updated_buffer, $changes) = update($buffer, $type);
		    print $OUTFH $updated_buffer, "\n";
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
	}
	print "\n";
	$log->info("finished processing $in_file");

	close $INFH or die "cannot close $in_file, $!";
	close $OUTFH or die "cannot close $out_file, $!";
    }
    $log->info('Processed', format_number($counter{total}), 'elements');
    for (@elems_to_process) {
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
    exit;
}

main() if (__PACKAGE__ eq 'main');

__DATA__

update_xml_files.pl: Update InterMine configuration files to reflect changes in the data model

  Synopsis:
update_xml_files.pl -i [file],[file] (-o [file],[file]) -m [file] -c [file.json] (-s [dir]) (-l [file])

This updater will read through an input file, checking the validity of 
any InterMine paths (in queries, classes, items or graphdisplayers), 
writing out the new updated version to a specified file. It requires an 
example of the new data model, as well as a list of the changes 
between the old model and the current one.

The updater will attempt to transform paths wherever possible. If this is
not possible, then classes or fields will be deleted. All changes and 
deletions will be logged. If a deletion makes an query or an item invalid,
then in the case of a query it will be declared "broken" and the original
version will be inserted into the new file: please see grep your log
for "broken" to find out which queries are broken so you can edit them by
hand, if you wish. Items will be logged as deleted, and removed from the 
output stream. These are very rare (about 3 per million) but again they can
be grepped for from the log output.

At the end of the run (which takes roughly 1min for every 1 million xml 
elements in the input stream) basic statistics will be logged, listing
the total number of items, templates and saved-queries processed, changed
and broken.

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

options that take multiple values (-i, -o) can either be called as:

  updater -i [file] -i [file]

or

  updater -i [file],[file]

if you do supply outputfiles, make sure you have the same number of them
as inputfiles, otherwise an exception will be thrown.

  Example:

perl svn/dev/intermine/scripts/updater.pl -i Projects/userprofile.xml,Projects/webconfig-model.xml -o Projects/userprofile.xml.new -o Projects/webconfig-model.xml -c Projects/model_changes.json -m svn/model_update/flymine/dbmodel/build/model/genomic_model.xml -l log/userprofiles.log -s svn/dev
