#!/usr/bin/perl


=head1 NAME

update_templates.pl

=head1 SYNOPSIS

update_templates.pl [--logfile file] [--outputfile file] [--inputfile file] [--modelfile]

Takes a file of templates and transforms them to match the new model.

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc update_templates.pl

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2010 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=cut


use strict;
use warnings;
use Carp;

use IO qw(Handle);

use Getopt::Long;

my($logfile, $outfile, $inputfile, $help, $new_model_file);
my $result = GetOptions("logfile=s"      => \$logfile,
			"outputfile=s"   => \$outfile,
			"inputfile=s"    => \$inputfile,
			"modelfile=s"     => \$new_model_file,
			"help"           => \$help,
			"usage"          => \$help,
    );

if ($help) {
    print qq{
$0: [--logfile file] [--outputfile file] [--inputfile file] [--modelfile]

Takes a file of templates and transforms them to match the new model.

WARNING: A template will only be considered broken if the sort order is deleted, 
or any of the constraints are deleted. Views may be deleted - check your log."
};

    exit;
}

use lib $ENV{HOME}.'/svn/dev/intermine/perl/lib';

my %ok_to_delete = (
    "analysis" => 1,
    "Analysis" => 1,
    "AnalysisResult" => 1,
    "Annotation" => 1,
    "ComputationalAnalysis" => 1,
    "ComputationalResult" => 1,
    "InfoSource" => 1,
    "Relation" => 1,
    "curated" => 1,
    "evidence" => 1,
    "Deletion" => 1,
    "relations" => 1,
    "BioProperty" => 1,
    "Evidence" => 1,
    "Experiment" => 1,
    "ExperimentalResult" => 1,
    "source" => 1,
    "phase" => 1,
    "endPhase" => 1,
    "endIsPartial" => 1,
    "startIsPartial" => 1,
    "abbreviation" => 1,
    "OverlapRelation" => 1,
    "PartialLocation" => 1,
    "RankedRelation" => 1,
    "Relation" => 1,
    "SimpleRelation" => 1,
    "SymmetricalRelation" => 1,
    "GenomeRegion" => 1,
    "exonCount" => 1,
    "genes" => 1,
    "exons" => 1,
    "features" => 1,
    );

my %translation_for = (
    'BioEntity.evidence'     =>   'dataSets',
    'DataSet.title'          =>   'name',
    'LocatedSequenceFeature' =>	  'SequenceFeature',
    'Location.object'        =>	  'locatedOn',
    'Location.subject'       =>	  'feature',
    'BlastMatch.object'      =>	  'parent',
    'BlastMatch.subject'     =>   'child',
    'ESTCluster'             =>   'OverlappingESTSet',
    'EST.ESTClusters'        =>   'overlappingESTSets',
    'InteractionExperiment.interactionDetectionMethod'	 
                             =>   'interactionDetectionMethods',
    'BioEntity.subjects'     =>   'locatedFeatures',
    'BioEntity.objects'      =>   'locations',
    'BioEntity.annotations'  =>   'ontologyAnnotations',
    );



use InterMine::TemplateFactory;
use InterMine::Model;

unless ($new_model_file) {
    $new_model_file 
	= $ENV{HOME}.'/svn/model_update/flymine/dbmodel/build/model/genomic_model.xml';
}
my $new_model = InterMine::Model->new(file => $new_model_file);    


{
    my %processed;
        
    sub update_path {
	my $path = shift;
	my $template = shift;
	if (exists $processed{$path}) {
	    return $processed{$path};
	}

	my @new_bits;

	my @bits       = split /[\.:]/, $path;
	my @separators = split /\w+/,   $path;

	my $class_name = shift @bits;
	
	my $class = $new_model->get_classdescriptor_by_name($class_name);


	if (defined $class) {
	    push @new_bits, $class_name;
	}
	else {
	    if ($class = eval {$new_model->get_classdescriptor_by_name($translation_for{$class_name})} ) {
		push @new_bits, $translation_for{$class_name};
	    }
	    else {
		announce("[WARNING ][", $template->get_name, qq{]Unexpected deletion of class "$class_name"}) unless $ok_to_delete{$class_name};
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
		my $index = @path_so_far - 1;
		my $old_path   = join '', zip(@separators[0 .. $index], @path_so_far);

		my $type;
		if ($type = $template->{pq}{type_of}{$old_path}) {
		    if ($type eq 'Relation') {
			$current_class =  $current_field->rev_referenced_classdescriptor();
		    }
		    elsif (my $typeclass = $new_model->get_classdescriptor_by_name($type)) {
			$current_class = $typeclass;
		    }
		}
		if (ref $current_class ne 'InterMine::Model::ClassDescriptor') {
		    croak "Could not find class of $new_bits[-1] when searching for $bit in $path";
		}
		else {
		    $current_field = $current_class->get_field_by_name($bit);
		}
		if (!defined $current_field) {
		    
		    # Maybe this field is declared in a parent class?
		    my @parents = get_parents($current_class);
		    my $parents = join('->', @parents);
		    foreach my $parent (@parents) {
			my $key = "$parent.$bit";
			if (my $translation = $translation_for{$key}) {
			    push @new_bits, $translation;
			    push @path_so_far, $bit;

			    $current_field = $current_class->get_field_by_name($translation);
			    $current_class = next_class($current_field);
			    next FIELD;
			}
			
		    }
		    if (!defined $current_field) { # still!
			unless ($ok_to_delete{$bit}) {
			    announce("[WARNING ][", 
				     $template->get_name, 
				     qq{]"Unexpected deletion of $bit from $path});
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
    sub get_parents {
	my $cd = shift;
	my @inheritance_path = ($cd->name(),);
	my @classes = $cd->extends_class_descriptors();
	for my $class (@classes) {
	    push @inheritance_path, get_parents($class);
	}
	return @inheritance_path;
    }
        
}

sub time_as_str {
    my @times = localtime(time);
    return join('-', ($times[5] + 1900), ($times[4]+1), $times[3])." $times[2]:$times[1]";
}

sub process_template {
    my $template = shift;
    
    my $is_broken;

    my $deletion = q!"[DELETION][" . $template->get_name . qq{][$place] "$path"}!;
    my $change   = q!"[CHANGE  ][" . $template->get_name . qq{][$place] "$path" => "$translation"}!;

    announce('*' x 10, '[Template] ', sprintf("%-50s", $template->get_name()), '[ at ', time_as_str(), ']');

    while (my ($key, $path) = each %{$template->{pq}{type_of}}) {
	my $place = 'types';
	my $translation = $translation_for{$path} || $path;
	announce(eval $change) unless ($path eq $translation);
	$template->{pq}{type_of}{$key} = $translation;
    }

    my @views = $template->get_views;
    my @new_views;
    for my $path (@views) {
	my $place = 'view';
	if (my $translation = update_path($path, $template)) {
	    announce(eval $change) unless ($path eq $translation);;
	    push @new_views, $translation;
	}
	else {
	    announce(eval $deletion);
	}
    }
    $template->{pq}{view} = \@new_views;

    my ($sort_order, $direction) = split(/\s/, $template->{pq}->sort_order);
    if ($sort_order) {
	for my $path ($sort_order) {
	    my $place = 'sort order';
	    if (my $translation = update_path($path, $template)) {
		announce(eval $change) unless ($path eq $translation);
		$sort_order = $translation;
	    }
	    else {
		announce(eval $deletion);
		$is_broken++;
	    }
	}
	$template->{pq}{sort_order} = $sort_order . 
	    (($direction) ? ' ' . $direction : '');
    }

    my @path_strings = keys %{$template->{pq}{pathDescriptions}};
    my %new_pathDescriptions;
    for my $path (@path_strings) {
	my $place = 'pathDescription';
	if (my $translation = update_path($path, $template)) {
	    announce(eval $change) unless ($path eq $translation);
	    $new_pathDescriptions{$translation} = $template->{pq}{pathDescriptions}{$path};
	}
	else {
	    announce(eval $deletion);
	}
    }
    $template->{pq}{pathDescriptions} = \%new_pathDescriptions;

    my @nodes = keys %{$template->{pq}{constraints}};
    my %new_constraints;
    for my $path (@nodes) {
	my $place = 'nodes';
	if (my $translation = update_path($path, $template)) {
	    announce(eval $change) unless ($path eq $translation);
	    $new_constraints{$translation} = $template->{pq}{constraints}{$path};
	    for my $con (@{$new_constraints{$translation}}) {
		$con->_set_path($translation);
	    }
	}
	else {
	    announce(eval $deletion, ' (with its '. 
		     scalar(@{$template->{pq}{constraints}{$path}}).
		     ' constraints)');
	    $is_broken++ if (@{$template->{pq}{constraints}{$path}});
	}
    }
    $template->{pq}{constraints} = \%new_constraints;
    
    my %new_typehash;
    for my $path (keys %{$template->{pq}{type_of}}) {
	my $place = 'types';
	if (my $translation = update_path($path, $template)) {
	    announce(eval $change) unless ($path eq $translation);
	    $new_typehash{$translation} = $template->{pq}{type_of}{$path};
	}
	else {
	    announce(eval $deletion);
	}
    }
    $template->{pq}{type_of} = \%new_typehash;
    
    return $is_broken;
}

sub main {

    announce("\n[Updating Templates] ", ' ' x 50, '[ at ', time_as_str(), ']');

    my $template_xml = read_input();

    my $tf = InterMine::TemplateFactory->new($template_xml, $new_model);
    output('<template-queries>');
    my $total = my $goodcount = my $errorcount = my $brokencount = 0;
    for my $template ($tf->get_templates) {
	my $broken = eval {process_template($template)};
	if ($@) {
	    carp "$@";
	    announce('[WARNING ][Processing] Unexpected error processing ', $template->get_name);
	    $errorcount++;
	}
	else {
	    my $output;
	    if ($broken) {
		announce('[WARNING ][Broken Template] ', $template->get_name);
		$output = $template->get_source_string();
		$brokencount++;
	    }
	    else {
		$output = eval {$template->to_string};
	    }
	    if ($output) {
		output($output);
	    }
	    else {
		carp "$@";
		$errorcount++;
		announce('[WARNING ][Output] Failed to write ', $template->get_name);
	    }
	}
	$total++;
    }
    output('</template-queries>');
    $goodcount = $total - ($errorcount + $brokencount);

    print "Processed $total templates, ". 
	( (($goodcount && $goodcount == $total)? 'all' : $goodcount) || 'none' ) .
	" of which were good, and ".
	( (($brokencount && $brokencount == $total)? 'all' : $brokencount) || 'none') . 
	" of which were broken, and ".
	( (($errorcount && $errorcount == $total)? 'all' : $errorcount) || 'none') . 
	" of which had errors \n";
    exit;
}


# utility for zipping two arrays together
# only works if both arrays are the same size!
sub zip {
    return @_ if (@_ == 1); # otherwise you get @_[0,0], ie. doubling.
    my $p = @_ / 2;         # find the length of the first array (ie. half the total)
    return @_[ map { $_, $_ + $p } 0 .. $p - 1 ]; # pair up the elements
}

# File handling routines
{
 #   my  $inputfile = '/home/alex/Projects/testtemplates.xml';
    my ($loghandle, $outhandle, $inputhandle);
    my ($initialised, %overwrite);

    sub initialise_filehandles {
	
	return if $initialised;
	
	if ($inputfile) {
	    open $inputhandle, '<', $inputfile 
		or croak "Can't open $inputfile for reading from, $!";
	}
	else {
	    $inputhandle = new IO::Handle;
	    $inputhandle->fdopen( \*STDIN, '<') 
		or croak "Can't read from STDIN, $!";
	}   

	if ($logfile) {
	    confirm_overwrite($logfile);
	    open $loghandle, '>', $logfile 
		or croak "Can't open $logfile for logging to, $!";
	}
	else {
	    $loghandle = new IO::Handle;
	    $loghandle->fdopen( \*STDERR, '>>') 
		or croak "Can't redirect the logging output to STDERR, $!";
	}

	if ($outfile) {
	    confirm_overwrite($outfile);
	    open $outhandle, '>', $outfile 
		or croak "Can't open $outfile for writing to, $!";
	}
	else {
	    $outhandle = new IO::Handle;
	    $outhandle->fdopen( \*STDOUT, '>') 
	    or croak "Can't redirect the output to STDERR, $!";
	}
	
	$initialised = 1;
	return;
    }

    sub confirm_overwrite {
	my $file = shift;
        return $overwrite{$file} if $overwrite{$file};
	if (-f $file) {
	    print "[QUESTION] $file already exists. Overwrite? (N|y): ";
	    chomp (my $answer = <STDIN>);
	    if ($answer =~ /y(es|[ie]p|eah)?/i) {
		return ($overwrite{$file} = 1);
	    }
	    else {
		exit;
	    }
	}
	else {
	    return ($overwrite{$file} = 1);
	}
    }
    
    sub announce {
	initialise_filehandles();
	print $loghandle @_, "\n" or croak "$!";
    }
    
    sub output {
	initialise_filehandles();
	print $outhandle @_, "\n" or croak "$!";
    }

    sub read_input {
	initialise_filehandles();
	my $content;
	$content .= $_ for <$inputhandle>;
	close $inputhandle or croak "Cannot close input file handle! $!";
	return $content;
    }

}

main() if (__PACKAGE__ eq 'main');


