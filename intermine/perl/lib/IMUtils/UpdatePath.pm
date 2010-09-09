package IMUtils::UpdatePath; {

    use strict;
    use warnings;
    use Carp;
    use Exporter 'import';

    use JSON;

    our @EXPORT= qw/changed dead update_path update_query check_class_name/;

    my ($model, $changes, $log);
    our $prefix = 'org.intermine.model.bio.';

    sub set_up {
	# args: model, changes,
	my $class = shift;
	my %args  = @_;
	$model    = $args{model};
	$log      = $args{log};

    # Read the details of the model changes from the .json config file
	die 'No model change details supplied - please list a file with the --changesfile flag '
	    unless $args{changes};
	open my $changesFH, '<', $args{changes} or die "Could not open $args{changes}, $!";
	my $content = join('', <$changesFH>);
	close $changesFH or die "could not close $args{changes}, $!";

    # Decode it into a hash reference
	my $json  = new JSON;
        $changes = $json->decode($content);
    }

    sub changed {
	my $key  = shift;
	return $changes->{translation_for}{$key};
    }

    sub dead {
	my $key = shift;
	$key =~ s/$prefix//;
	return $changes->{ok_to_delete}{$key};
    }

# utility for zipping two arrays together
# only works if both arrays are the same size!
    sub zip {
	my @dbl_array = @_;

	# otherwise you get @dbl_array[0,0], ie. doubling.
	return @dbl_array if (@dbl_array == 1);

	# find the length of the first array (ie. half the total)
	my $midpoint = @dbl_array / 2;

	# pair up the elements
	return @dbl_array[ map { $_, $_ + $midpoint } 0 .. $midpoint - 1 ];
    }


    sub check_class_name {
	my $class_name = shift;
	$class_name =~ s/$prefix// if $class_name;
	my $class = eval {$model->get_classdescriptor_by_name($class_name)};
	return $class;
    }

    my %processed;

    sub update_path {
	my $path = shift;

	if (exists $processed{$path}) {
	    return $processed{$path};
	}

	my $prefixed;
	if ($path =~ /$prefix/) {
	    $path =~ s/$prefix//; # cut off the prefix
	    $prefixed++;          # but remember that we did so
	}

	my $query = shift;

	my $query_name = (ref $query) ? $query->get_name : $query;


	my @new_bits;

	my @bits       = split /[\.:]/, $path;
	my @separators = split /\w+/,   $path;

	my $class_name = shift @bits;

	my $class = check_class_name($class_name);

	if (defined $class) {
	    push @new_bits, $class_name;
	}
	else {
	    if ($class = check_class_name(changed($class_name)) ) {
		push @new_bits, changed($class_name);
	    }
	    else {
		$log->warning($query_name, qq{Unexpected deletion of class "$class_name"})
		    unless dead($class_name);
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
		  my @ancestors = map {$_->name} $current_class->get_ancestors;

		  foreach my $ancestor (@ancestors) {
		      my $key = "$ancestor.$bit";
		      if (my $translation = changed($key)) {
			  if ($current_field = $current_class->get_field_by_name($translation)){

			      push @new_bits, $translation;
			      push @path_so_far, $bit;

			      $current_class = next_class($current_field);
			      next FIELD;
			  }
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
	my $new_path = join('', zip(@separators, @new_bits));

	if ($prefixed) { # put it back on then
	    $path     = $prefix . $path;
	    $new_path = $prefix . $new_path;
	}
	$processed{$path} = $new_path;
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
	my $query  = shift;
	my $origin = shift;

	my ($is_broken, $is_changed);

	my $deletion = q!$is_changed++;"[DELETION][". $origin . $query->get_name . qq{][$place] "$path"}!;
	my $change   = q!$is_changed++;"[CHANGE][" . $origin . $query->get_name . qq{][$place] "$path" => "$translation"}!;
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
	    $log->warning($origin, $query->{type}, '"'.$query->get_name.'"', '"is broken');
	}
	elsif ($is_changed) {
	    $log->info($origin, $query->{type}, '"'.$query->get_name.'"', 'has been updated');
	    $is_broken = 0;
	}
	else {
	    $log->info($origin, $query->{type}, '"'.$query->get_name.'"', 'is unchanged');
	    $is_broken = undef;
	}
	return $query, $is_broken;
    }

}
1;
