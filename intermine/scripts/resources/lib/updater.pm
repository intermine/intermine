package Updater;

use Moose;
use InterMine::Model::Attribute;
use Webservice::InterMine::Path qw(type_of class_of next_class);
use InterMine::Model::Types (qw/Model/);
use Webservice::InterMine::Types (qw/File/);
use MooseX::Types::Moose   (qw/HashRef Str/);
use YAML;
use Scalar::Util qw(blessed);

has model => (
    is  => 'ro',
    isa => Model,
    handles => {
        class => 'get_classdescriptor_by_name',
    },
);

has logger => (
    is      => 'ro',
    isa     => 'Log::Handler',
    handles => [ 'warn', 'warning', 'info', 'error', 'debug', ],
);

my $parse_changes = sub {
    my $self = shift;
    my $file = shift;
    my ( $delete_these, $translate_these ) = YAML::LoadFile($file);
    $delete_these = { map { $_ => 1 } @$delete_these };
    $self->set_deletions($delete_these);
    $self->set_translations($translate_these);
};

has changes => (
    is      => 'ro',
    isa     => File,
    trigger => $parse_changes,
);

has deletions => (
    isa     => HashRef,
    writer  => 'set_deletions',
    traits  => ['Hash'],
    default => sub { {} },
    handles => { has_deletion => 'exists', },
);

has translations => (
    isa     => HashRef,
    writer  => 'set_translations',
    traits  => ['Hash'],
    default => sub { {} },
    handles => { get_translation_for => 'get', },
);

sub knows_about_deletion_of {
    my $self = shift;
    my $key  = shift;
    my $prefix = $self->prefix;
    $key =~ s/$prefix//;
    return $self->has_deletion($key);
}

has processed_paths => (
    isa     => HashRef,
    traits  => ['Hash'],
    default => sub { {} },
    handles => {
        has_processed        => 'exists',
        processed_version_of => 'accessor',
        delete_processed     => 'delete',
    },
);

has prefix => (
    isa     => Str,
    is      => 'ro',
    default => 'org.intermine.model.bio.',
);

sub get_class {
    my $self = shift;
    my $class_name = shift;
    return unless $class_name;
    my $prefix = $self->prefix;
    $class_name =~ s/$prefix//;
    my $class = eval { $self->class($class_name) };
    return $class;
}

####### ROUTINE TO TRANSLATE AN OLD DOTTED PATH INTO ITS NEW FORM
### TAKES TWO ARGS: update_path($path, [$type_dict])

sub update_path {
    my $self = shift;
    my $path = shift;
    my $type_dict = shift || {};

    ### Don't do work if you've already done it
    if ( $self->has_processed($path) ) {
        return $self->processed_version_of($path);
    }

    my $prefixed;
    my $prefix = $self->prefix;
    if ( $path =~ /$prefix/ ) {
        $path =~ s/$prefix//;    # cut off the prefix
        $prefixed++;             # but remember that we did so
    }
    #######

    my @new_bits;
    my @bits       = split /\./, $path;
    my $class_name = shift @bits;
    my $class      = $self->get_class($class_name);

    if ( defined $class ) {
        push @new_bits, $class;
    } else {

        # Maybe this class has a new name
        if ( $class = $self->get_class( $self->get_translation_for($class_name) ) ) {
            push @new_bits, $class;
        } else {
            $self->warning(
                qq{Unexpected deletion of class "$class_name" from "$path"})
              unless $self->knows_about_deletion_of($class_name);
            return;
        }
    }

    my $current_class = $class;
    my $current_field = undef;

    my @path_so_far = ($class_name);

    my $skip_continue;
  FIELD: while ( my $bit = shift @bits ) {
      $skip_continue = 0;

        if ( $bit eq 'id' and not @bits ) {

            # id is an internal attribute for all tables
            $current_field = InterMine::Model::Attribute->new(
                name  => 'id',
                type  => 'Integer',
                model => $self->model,
            );
            next FIELD;

        } else {
            my $old_path = join( '.', @path_so_far );

            $current_field = $current_class->get_field_by_name($bit);

            next FIELD if ( defined $current_field );

            # Maybe this field has a new name, either here or in a parent class?
            my @ancestors = map { $_->name } $current_class->get_ancestors;

            foreach my $ancestor (@ancestors) {
                my $key = "$ancestor.$bit";
                if ( my $new = $self->get_translation_for($key) ) {
                    if ( $new =~ /\w+\.\w+/ )
                    {    # Translation is not one, but two steps or more
                        unshift @bits, split( /\./, $new );
                        $skip_continue = 1;
                        next FIELD;
                    } elsif ( $current_field =
                        $current_class->get_field_by_name($new) )
                    {
                        next FIELD;
                    }
                }
            }

            # We've got a dead one here
            unless ( $self->knows_about_deletion_of($bit) ) {
                my $so_far = join( '.', @new_bits );
                $self->warning(
qq{Unexpected deletion of field "$bit" from "$current_class" in "$so_far", while processing "$path"}
                );
            }
            $self->delete_processed($path);
            return;
        }
    } continue {
        unless ($skip_continue) {
            push @new_bits,    $current_field;
            push @path_so_far, $bit;
            confess "Type dictionary is not a hash ref"
            if ( ($type_dict) and ( not ref $type_dict eq 'HASH' ) );
            my $type = $type_dict->{ join( '.', @path_so_far ) };
            $current_class = next_class( $current_field, $self->model, $type );
        }
    }
    my $new_path = join( '.', @new_bits );

    if ($prefixed) {    # put it back on then
        $path     = $prefix . $path;
        $new_path = $prefix . $new_path;
    }
    $self->processed_version_of($path => $new_path);
    return $new_path;
}

sub update_type_classes {
    my $self = shift;
    my $query = shift;
    my $is_changed = 0;
    # Needs changing first, 
    # so that the typedict contains useful information
    my @java_types = (qw/
        String Boolean Double Float
    /);
    for my $scc ( $query->sub_class_constraints ) {
        my $path  = $scc->type;
        if (grep {$_ eq $path} @java_types) {
            $query->remove($scc);
        } else {
            my $place = 'sub-class-constraints';
            if ( my $translation = $self->update_path($path) ) {
                unless ($path eq $translation) {
                    $self->log_change(
                        in   => $place,
                        from => $query->{':origin'},
                        re   => $query->name,
                        of   => $path,
                        to   => $translation,
                    );
                    $is_changed++;
                }
                $scc->set_type($translation);
            } else {
                $self->log_deletion(
                    in   => $place,
                    from => $query->{':origin'},
                    re   => $query->name,
                    of   => $path,
                );
                $is_changed++;
                $query->remove($scc);
            }
        }
    }
    return $is_changed;
}

sub update_view {
    my $self = shift;
    my $query = shift;
    my $is_changed = 0;
    my @views = $query->views;
    my @new_views;
    for my $path (@views) {
        my $place = 'view';
        if ( my $translation = $self->update_path( $path, $query->type_dict ) ) {
            unless ($path eq $translation) {
                $self->log_change(
                    in   => $place,
                    from => $query->{':origin'},
                    re   => $query->name,
                    of   => $path,
                    to   => $translation,
                );
                $is_changed++;
            }
            push @new_views, $translation;
        } else {
            $self->log_deletion(
                in   => $place,
                from => $query->{':origin'},
                re   => $query->name,
                of   => $path,
            );
            $is_changed++;
        }
    }
    $query->clear_view;
    
    $query->add_view(@new_views) if (@new_views); 
    return $is_changed;
}
sub log_change {
    my $self = shift;
    confess "Bad arguments to log_change: ", @_
        unless (@_ == 10);
    my %change = @_;
    my $message = sprintf(qq{[CHANGE  ] [%s %s] [%s] "%s" => "%s"},
                            @change{qw/from re in of to/});
    $self->info($message);
}
sub log_deletion {
    my $self = shift;
    confess "Bad arguments to log_deletion: ", @_
        unless (@_ == 8);
    my %change = @_;
    my $message = sprintf(qq{[DELETION] [%s %s] [%s] "%s"},
                            @change{qw/from re in of/});
    $self->warning($message);
}

sub update_sort_order {
    my $self = shift;
    my $query = shift;
    my $is_changed = 0;
    my $place = 'sort order';
    my @orders = $query->sort_orders;
    $query->clear_sort_order;
    for my $so (@orders) {
        my $path = $so->path;
        if ( my $translation = $self->update_path( $path, $query->type_dict ) ) {
            unless ($path eq $translation) {
                $self->log_change(
                    in   => $place,
                    from => $query->{':origin'},
                    re   => $query->name,
                    of   => $path,
                    to   => $translation,
                );
                $is_changed++;
            }
            $query->add_sort_order( $translation, $so->direction );
        } else {
            $self->log_deletion(
                in   => $place,
                from => $query->{':origin'},
                re   => $query->name,
                of   => $path,
            );
            $is_changed++;
        }
    }
    return $is_changed;
}

sub update_child_elements {
    my $self = shift;
    my $query = shift;
    my $is_changed = 0;
    for my $path_feature ( $query->all_children ) {
        my $path  = $path_feature->path;
        my $place = $path_feature->element_name;
        if ( my $translation = $self->update_path( $path, $query->type_dict ) ) {
            unless ($path eq $translation) {
                $self->log_change(
                    in   => $place,
                    from => $query->{':origin'},
                    re   => $query->name,
                    of   => $path,
                    to   => $translation,
                );
                $is_changed++;
            }
            $path_feature->set_path($translation);
        } else {
            $self->log_deletion(
                in   => $place,
                from => $query->{':origin'},
                re   => $query->name,
                of   => $path,
            );
            $is_changed++;
            $query->remove($path_feature);
        }
    }
    return $is_changed;
}

sub update_query {
    my $self = shift;
    my $query  = shift;
    my $origin = shift;

    # Those of squeemish dispositions look away now...
    unless (ref $query and blessed $query and $query->isa('HASH')) {
        confess "Can't use this query - it must be blessed hash ref";
    }
    $query->{':origin'} = $origin; 

    my ( $is_broken, $is_changed );

    $self->info(
        sprintf( qq{Processing %s "%s"}, $query->type, $query->name ) );
    
    $is_changed += $self->update_type_classes($query);
    $is_changed += $self->update_view($query);
    $is_broken = "doesn't have any valid views" unless $query->views;
    $self->warn($query->name, $is_broken) if $is_broken;

    if ( not $is_broken and $query->has_sort_order ) {
        $is_changed += $self->update_sort_order($query);
        $is_broken = "has had its sort-order deleted" unless $query->has_sort_order;

    }

    unless ($is_broken) {
        my $no_of_children = $query->all_children;
        $is_changed += $self->update_child_elements($query);
        $is_broken = "Some or all of its child nodes were deleted" 
            unless ($query->all_children == $no_of_children);
    }

    $query->clean_out_SCCs; 

    my $this_query =
    sprintf( qq{[%s %s] "%s"}, $origin, $query->type, $query->name );
    if ($is_broken) {       # Three code system: pos value for broken
        $self->warning( $this_query, 'is broken:', $is_broken );
    } elsif ($is_changed) {
        $self->info( $this_query, 'has been updated' );
        $is_broken = 0;     # Three code system: 0 for ok, but changed
    } else {
        $self->info( $this_query, 'is unchanged' );
        $is_broken = undef; # Three code system: undef for unchanged
    }

    return $query, $is_broken;
}

1;
