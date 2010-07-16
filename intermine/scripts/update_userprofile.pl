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
use JSON;

## Optional Module: Number:Format
BEGIN {
    # If Number::Format is not installed, don't format numbers
    eval "use Number::Format qw(format_number)";
    if ($@) {
	sub format_number {return @_};
    }
}
my($logfile, $outfile, $inputfile, $help, 
   $new_model_file, $changes_file);
my $lib = $ENV{HOME}.'/svn/dev';
my $result = GetOptions("logfile=s"       => \$logfile,
			"outputfile=s"    => \$outfile,
			"inputfile=s"     => \$inputfile,
			"modelfile=s"     => \$new_model_file,
			"changesfile=s"   => \$changes_file,
			"help"            => \$help,
			"usage"           => \$help,
			"svndirectory=s"  => \$lib,
    );

sub usage {
    my $str = qq{
$0: Update InterMine userprofile.xml files to reflect changes in the data model

  Synopsis:
$0 -i [file] -o [file] -m [file] -c [file.json] (-s [dir]) (-l [file])

This updater will read through an input file, checking the validity of 
any InterMine paths (in queries or items), writing out the new updated
version to a specified file. It requires an example of the new data 
model, as well as a list of the changes between the old model and the 
current one.

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

--inputfile    | -i   : The userprofile to be processed

--outputfile   | -o   : The file to write the new userprofile to

--modelfile    | -m   : The new model to validate paths against

--changesfile  | -c   : The file specifying model changes (deletions
                        and name changes)
--svndirectory | -s   : The location of the InterMine svn directory,
                        by default this is assumed to be "~/svn/dev"

--logfile      | -l   : File to save the log to. If there is no file, 
                        all logging output will go to STDOUT

  Example:

perl svn/dev/intermine/scripts/update_userprofile.pl -i Projects/userprofile.xml -o Projects/userprofile.xml.new -c Projects/model_changes.json -m svn/model_update/flymine/dbmodel/build/model/genomic_model.xml -l log/userprofiles.log -s svn/dev

};
    print $str;
}

if (not ($inputfile and $outfile and $new_model_file and $changes_file) 
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

# Read the details of the model changes from the .json config file
die 'No model change details supplied - please list a file with the --changesfile flag ' unless $changes_file;
open my $changesFH, '<', $changes_file or die "Could not open $changes_file, $!";
my $content;
$content .= $_ for <$changesFH>;
close $changesFH or die "could not close $changes_file, $!";

# Decode it into a hash reference
my $json  = new JSON;
my $changes_href = $json->decode($content);

my %processed;

sub zip {
    my @dbl_array = @_;

    # otherwise you get @dbl_array[0,0], ie. doubling.
    return @dbl_array if (@dbl_array == 1); 

    # find the length of the first array (ie. half the total)
    my $midpoint = @dbl_array / 2;     

    # pair up the elements
    return @dbl_array[ map { $_, $_ + $midpoint } 0 .. $midpoint - 1 ]; 
}

sub changed {
    my $key = shift;
    return $changes_href->{translation_for}{$key};
}

sub dead {
    my $key = shift;
    $changes_href->{ok_to_delete}{$key};
}


sub update_path {
    my $path = shift;
    my $query = shift;
    
    my $query_name = (ref $query) ? $query->get_name : $query;

    if (exists $processed{$path}) {
	return $processed{$path};
    }

    my @new_bits;

    my @bits       = split /[\.:]/, $path;
    my @separators = split /\w+/,   $path;

    my $class_name = shift @bits;
    
    my $class = $model->get_classdescriptor_by_name($class_name);

    if (defined $class) {
	push @new_bits, $class_name;
    }
    else {
	if ($class = 
	    eval {$model->get_classdescriptor_by_name(changed($class_name))}
	    ) {
	    push @new_bits, changed($class_name);
	}
	else {
	    $log->warning($query_name, qq{Unexpected deletion of class "$class_name"}) unless dead($class_name);
	    return;
	}
    }
    
    my $current_class = $class;
    my $current_field = undef;
    
    my @path_so_far = ($class_name,);
  FIELD: for my $bit (@bits) {

      if ($bit eq 'id' and $bit eq $bits[-1]) { # id is an internal attribute for all tables
	  push @new_bits, $bit;
	  $current_class = undef; # id must be the final attribute 
	  # - this will catch it if it isn't
      }
      else {
	  my $old_path = join '', zip(@separators[0 .. $#path_so_far], @path_so_far);

	  my $type;
	  if (ref $query and $type = $query->type_of($old_path)) {
	      if ($type eq 'Relation') {
		  $current_class =  $current_field->rev_referenced_classdescriptor();
	      }
	      elsif (my $typeclass = eval {$model->get_classdescriptor_by_name($type)}) {
		  $current_class = $typeclass;
	      }
	  }
	  if (not UNIVERSAL::can($current_class, 'isa') 
	      or not $current_class->isa('InterMine::Model::ClassDescriptor')) {
	      croak "Could not find class of $new_bits[-1] when searching for $bit in $path";
	  }
	  else {
	      $current_field = $current_class->get_field_by_name($bit);
	  }
	  if (!defined $current_field) {
	      
	      # Maybe this field is declared in a parent class?
	      my @parents = map {$_->name} $current_class->get_parents;

	      foreach my $parent (@parents) {
		  my $key = "$parent.$bit";
		  if (my $translation = changed($key)) {
		      push @new_bits, $translation;
		      push @path_so_far, $bit;

		      $current_field = $current_class->get_field_by_name($translation);
		      $current_class = next_class($current_field);
		      next FIELD;
		  }
		  
	      }
	      if (!defined $current_field) { # still!
		  unless (dead($bit)) {
		      $log->warn("$query_name: Unexpected deletion of $bit from $path");
		  }
		  $processed{$path} = undef;
		  return;
	      }
	  }
	  push @new_bits, $bit;
	  push @path_so_far, $bit;
	  $current_class = next_class($current_field);
      }
  }
    $processed{$path} = join('', zip(@separators, @new_bits));
    return $processed{$path};
    
}

sub next_class {
    my $field = shift;
    if ($field->field_type() eq 'attribute') {
	# if this is not the last bit, it will caught next time around the loop
	return undef;
    } else {
	if (   ($field->can('is_many_to_0') ) # for this kind of reference
	       &&                             # we prefer reverse refs to refs
	       ($field->is_many_to_0)                      ) {
	    return $field->rev_referenced_classdescriptor() ||
		$field->referenced_classdescriptor();
	}
	else {
	    return $field->referenced_classdescriptor();
	}
    }
    
}

sub update_query {
    my $query = shift;
    
    my ($is_broken, $is_changed);

    my $deletion = q!$is_changed++;"[DELETION][" . $query->get_name . qq{][$place] "$path"}!;
    my $change   = q!$is_changed++;"[CHANGE][" . $query->get_name . qq{][$place] "$path" => "$translation"}!;
    confess "$query is not a reference" unless (ref $query);
    $log->info('Processing', $query->{type}, '"'.$query->get_name. '"');
    
    if ($query->type_hash) {
	while (my ($key, $path) = each %{$query->type_hash}) {
	    my $place = 'types';
	    my $translation = changed($path);
	    unless (defined $translation) {
		if (dead($path)) {
		   # maybe we can do better by looking at the path itself
		    if (my $new_key = update_path($key, $query)) {
			my @args = ($model, $new_key);
			$translation = eval {InterMine::Path->new(@args)->end_type};
		    }
		}
		else {
		    $translation = $path;
		}
	    }
	    # to prevent undefined in string errors
	    $translation = '' unless $translation; 

	    $log->info(eval $change) unless ($path eq $translation);
	    $query->type_of($key => $translation);
	}
   }

    my @views = $query->get_views;
    my @new_views;
    for my $path (@views) {
	my $place = 'view';
	if (my $translation = update_path($path, $query)) {
	    $log->info(eval $change) unless ($path eq $translation);;
	    push @new_views, $translation;
	}
	else {
	    $log->info(eval $deletion);
	}
    }
    $query->{view} = \@new_views;
    
    if ($query->sort_order) {
	my ($sort_order, $direction) = split(/\s/, $query->sort_order);
	if ($sort_order) {
	    for my $path ($sort_order) {
		my $place = 'sort order';
		if (my $translation = update_path($path, $query)) {
		    $log->info(eval $change) unless ($path eq $translation);
		    $sort_order = $translation;
		}
		else {
		    $log->warning(eval $deletion);
		    $is_broken++;
		}
	    }
	    $query->{sort_order} = $sort_order . 
		(($direction) ? ' ' . $direction : '');
	}
    }

    my @path_strings = $query->get_described_paths();
    my %new_pathDescriptions;
    for my $path (@path_strings) {
	my $place = 'pathDescription';
	if (my $translation = update_path($path, $query)) {
	    $log->info(eval $change) unless ($path eq $translation);
	    $new_pathDescriptions{$translation} = $query->{pathDescriptions}{$path};
	}
	else {
	    $log->info(eval $deletion);
	}
    }
    $query->{pathDescriptions} = \%new_pathDescriptions;

    my @nodes = $query->get_node_paths();
    my %new_constraints;
    for my $path (@nodes) {
	my $place = 'nodes';
	if (my $translation = update_path($path, $query)) {
	    $log->info(eval $change) unless ($path eq $translation);
	    $new_constraints{$translation} = $query->{constraints}{$path};
	    for my $con (@{$new_constraints{$translation}}) {
		$con->_set_path($translation);
	    }
	}
	else {
	    $log->info(eval $deletion, ' (with its '. 
		       scalar(@{$query->{constraints}{$path}}).
		       ' constraints)');
	    $is_broken++ if (@{$query->{constraints}{$path}});
	}
    }
    $query->{constraints} = \%new_constraints;

    if ($query->type_hash) {
	my %new_typehash;
	for my $path (keys %{$query->type_hash}) {
	    my $place = 'types';
	    if (my $translation = update_path($path, $query)) {
		$log->info(eval $change) unless ($path eq $translation);
		$new_typehash{$translation} = $query->type_of($path);
	    }
	    else {
		$log->info(eval $deletion);
	    }
	}
	$query->type_hash(\%new_typehash);
    }
    
    if ($is_broken) {	
	$log->warning($query->{type}, '"'.$query->get_name.'"', '"is broken');
    }
    elsif ($is_changed) {
	$log->info($query->{type}, '"'.$query->get_name.'"', 'has been updated');
	$is_broken = 0;
    }
    else {
	$log->info($query->{type}, '"'.$query->get_name.'"', 'is unchanged');
	$is_broken = undef;
    }
    return $query, $is_broken;
}

sub update {
    my ($buffer, $type) = @_;
    if ($type eq 'item') {
	return translate_item($type, $buffer);
    }
    else {
	return translate_query($type, $buffer);
    }
}

my $parser = XML::Rules->new(rules => [_default => 'no content array']);

sub undress { # takes xml and extracts the content
    my $xml = shift;
    return $parser->parse($xml);
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
    if ($is_broken) {
	return $new_q->get_source_string, $is_broken;
    }
    else {
	return $new_q->to_xml_string, $is_broken;
    }
}

sub translate_item { # takes in xml, returns xml
    my ($type, $xml) = @_;
    my $naked    = undress($xml);
    my $hash_ref = $naked->{$type}[0];
    my $nothing  = '';
    my $broken   = 1;
    delete($hash_ref->{_content});
    my ($class, $changed); 
    unless ($class = eval {
	$model->get_classdescriptor_by_name($hash_ref->{implements})
	    }) {
	my $translation = update_path($hash_ref->{implements}, 
						$hash_ref->{name});

	if ($class = eval {$model->get_classdescriptor_by_name($translation)}) {
	    $log->info('[CHANGE] Item', $hash_ref->{id}, ':', $hash_ref->{implements}, " => $translation");
	    $hash_ref->{implements} = $translation;
	    $changed++;
	}
	else {
	    $log->warning('[DELETION] Item', $hash_ref->{id}, ': could not find', $hash_ref->{implements});
	    return $nothing, $broken;
	}
    }
    
    foreach my $field (@{$hash_ref->{attribute}}, @{$hash_ref->{reference}}) {
	if (not $class->get_field_by_name($field->{name})) {
	    my $path = join '.', $hash_ref->{implements}, $field->{name};
	    my $translation = update_path($path, $hash_ref->{id});
	    if (defined $translation) {
		$translation =~ s/^.*\.//;
		$field->{name} = $translation;
		$log->info('[CHANGE] Item', 
			  $hash_ref->{id}, ':', 
			  "$path => $translation");
		$changed++;
	    }
	    else {
		$log->warning('[DELETION] Item',  
			     $hash_ref->{id}, 
			     ": Could not find $path");
		return $nothing, $broken;
	    }
	}
    }
    return dress($type,$hash_ref), ($changed)? 0 : undef;
}	

sub main {
    open (my $INFH, '<', $inputfile) or die "cannot open $inputfile, $!";
    open (my $OUTFH, '>', $outfile)  or die "cannot open $outfile, $!";
    my ($buffer, $is_buffering);
    
    my %counter;
    my @elems_to_process = qw(item template saved-query);

    while (my $line = <$INFH>) {
	$line =~ s/></>><</g;
	my @elems;
	@elems = split('><', $line);
	while (my $elem = shift @elems) {
	    my ($end, $type) = $elem =~ m!^\s*<(/?)([a-z-]+)[\s>]!i;
	    if ($type) {
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
    $log->info("finished processing");
    $log->info("Processed $counter{total} elements");
    for (@elems_to_process) {
	my $tag = $_;
	$tag =~ s/y/ie/;
	my $msg = sprintf(
	    "Processed %8s %-12s: %d unchanged, %d broken, %d changed",
	    format_number($counter{$_} || 0),
	    $tag.'s',
	    format_number($counter{unchanged}{$_} || 0),
	    format_number($counter{broken}{$_}    || 0),
	    format_number($counter{changed}{$_}   || 0),
	    );
	$log->info($msg);
    }
    close $INFH or die "cannot close $inputfile, $!";
    close $OUTFH or die "cannot close $outfile, $!";
    
    exit;
}

main() if (__PACKAGE__ eq 'main');

