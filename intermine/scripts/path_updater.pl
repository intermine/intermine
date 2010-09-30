#!/usr/bin/perl
use strict;
use warnings;
use Carp qw(carp croak confess);

# We want all warnings to be very very fatal
$SIG{__WARN__} = sub {
    confess @_;
};
## Modules to be installed
use XML::Rules;
use XML::Writer;
use YAML;
use Log::Handler;
use AppConfig qw(:expand :argcount);

## These should be installed or on the path before use
use InterMine::Model 0.9401;
use Webservice::InterMine::Query::Template;
use Webservice::InterMine::Query::Saved;
use Webservice::InterMine::Path qw(type_of class_of next_class);

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
my $prefix = 'org.intermine.model.bio.';

# Set up configured options 
my $config = AppConfig->new({GLOBAL => {EXPAND   => EXPAND_ALL,
            ARGCOUNT => ARGCOUNT_ONE}});
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

my ($delete_these, $translate_these) = YAML::LoadFile($changes_file);
my $newmodel = InterMine::Model->new(file => $new_model_file);
my $service = bless {}, 'Webservice::InterMine::Service'; # dummy service needed for instantiation

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
        
sub changed {
    my $key = shift;
    return $translate_these->{$key};
}

sub dead {
    my $key = shift;
    $key =~ s/$prefix//;
    return grep {$_ eq $key} @$delete_these;
}

sub check_class_name {
    my $class_name = shift;
    $class_name =~ s/$prefix// if $class_name;
    my $class = eval { $newmodel->get_classdescriptor_by_name($class_name) };
    return $class;
}

####### ROUTINE TO TRANSLATE AN OLD DOTTED PATH INTO ITS NEW FORM
### TAKES TWO ARGS: update_path($path, [$type_dict])

my %processed;
sub update_path {
    my $path = shift;
    
    ### Mangle the path in a couple of ways
    if ( exists $processed{$path} ) {
        return $processed{$path};
    }

    my $prefixed;
    if ( $path =~ /$prefix/ ) {
        $path =~ s/$prefix//;    # cut off the prefix
        $prefixed++;             # but remember that we did so
    }
    #######

    my $type_dict = shift || {};
    my @new_bits;
    my @bits       = split /\./, $path;
    my $class_name = shift @bits;
    my $class = check_class_name($class_name);

    if ( defined $class ) {
        push @new_bits, $class;
    } else {
        # Maybe this class has a new name
        if ( $class = check_class_name( changed($class_name) ) ) {
            push @new_bits, $class;
        } else {
            $log->warning(qq{Unexpected deletion of class "$class_name" from "$path"} )
                unless dead($class_name);
            return;
        }
    }

    my $current_class = $class;
    my $current_field = undef;

    my @path_so_far = ( $class_name );
    FIELD: while (my $bit = shift @bits) {

        if ( $bit eq 'id' and not @bits ) {    
            # id is an internal attribute for all tables
            $current_field = InterMine::Model::Attribute->new(
                name => 'id',
                type => 'Integer',
                model => $newmodel,
            );
            next FIELD;

        } else {
            my $old_path = join('.', @path_so_far );

            $current_field = $current_class->get_field_by_name($bit);
            
            next FIELD if (defined $current_field);

            # Maybe this field has a new name, either here or in a parent class?
            my @ancestors = map { $_->name } $current_class->get_ancestors;

            foreach my $ancestor (@ancestors) {
                my $key = "$ancestor.$bit";
                if (my $translation = changed($key)) {
                    if ($translation =~ /\w+\.\w+/) { # Translation is not one, but two steps
                        unshift @bits, split(/\./, $translation);
                        $bit = shift @bits;
                        redo FIELD;
                    } elsif ( $current_field = $current_class->get_field_by_name($translation)) {
                        next FIELD;
                    } 
                }
            }
            # We've got a dead one here
            unless ( dead($bit) ) {
                my $so_far = join('.', @new_bits);
                $log->warn(qq{Unexpected deletion of field "$bit" from "$current_class" in "$so_far", while processing "$path"});
            }
            return $processed{$path} = undef;
        }
    } continue {
        push @new_bits,    $current_field;
        push @path_so_far, $bit;
        confess "Type dictionary is not a hash ref" if (($type_dict) and (not ref $type_dict eq 'HASH'));
        my $type = $type_dict->{join('.', @path_so_far)};
        $current_class = next_class($current_field, $newmodel, $type);
    }
    my $new_path = join( '.', @new_bits );

    if ($prefixed) {    # put it back on then
        $path     = $prefix . $path;
        $new_path = $prefix . $new_path;
    }
    $processed{$path} = $new_path;
    return $processed{$path};
}

my $deletion =
q!$is_changed++;"[DELETION][". $origin . $query->name . qq{][$place] "$path"}!;
my $change =
q!$is_changed++;"[CHANGE][" . $origin . $query->name . qq{][$place] "$path" => "$translation"}!;

sub update_type_classes {
    my ($query, $origin) = @_;
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
            if ( my $translation = update_path($path) ) {
                $log->info( eval $change ) unless ( $path eq $translation );
                $scc->set_type($translation);
            } else {
                $log->info( eval $deletion );
                $query->remove($scc);
            }
        }
    }
    return $is_changed;
}

sub update_view {
    my ($query, $origin) = @_;
    my $is_changed = 0;
    my @views = $query->views;
    my @new_views;
    for my $path (@views) {
        my $place = 'view';
        if ( my $translation = update_path( $path, $query->type_dict ) ) {
            $log->info( eval $change ) unless ( $path eq $translation );
            push @new_views, $translation;
        } else {
            $log->info( eval $deletion );
        }
    }
    $query->clear_view;
    
    $query->add_view(@new_views) if (@new_views); 
    return $is_changed;
}

sub update_sort_order {
    my ($query, $origin) = @_;
    my $place = 'sort order';
    my $is_changed = 0;
    $query->clear_sort_order;
    for my $so ($query->sort_orders) {
        my $path = $so->path;
        if ( my $translation = update_path( $path, $query->type_dict ) ) {
            $log->info( eval $change ) unless ( $path eq $translation );
            $query->add_sort_order( $translation, $so->direction );
        } else {
            $log->info( eval $deletion );
        }
    }
    return $is_changed;
}

sub update_child_elements {
    my ($query, $origin) = @_;
    my $is_changed = 0;
    for my $path_feature ( $query->all_children ) {
        my $path  = $path_feature->path;
        my $place = $path_feature->element_name;
        if ( my $translation = update_path( $path, $query->type_dict ) ) {
            $log->info( eval $change ) unless ( $path eq $translation );
            $path_feature->set_path($translation);
        } else {
            $log->info( eval $deletion );
            $query->remove($path_feature);
        }
    }
    return $is_changed;
}

sub update_query {
    my $query  = shift;
    my $origin = shift;
    my ( $is_broken, $is_changed );

    confess "$query is not a reference" unless ( ref $query );
    $log->info(
        sprintf( qq{Processing %s "%s"}, $query->type, $query->name ) );
    
    $is_changed += update_type_classes($query, $origin);
    $is_changed += update_view($query, $origin);
    $is_broken = "It doesn't have any valid views" unless $query->views;

    if ( not $is_broken and $query->has_sort_order ) {
        $is_changed += update_sort_order($query, $origin);
        $is_broken++ unless $query->has_sort_order;
    }

    unless ($is_broken) {
        my $no_of_children = $query->all_children;
        $is_changed += update_child_elements($query, $origin);
        $is_broken = "Some or all of its child nodes were deleted" 
            unless ($query->all_children == $no_of_children);
    }

    $query->clean_out_SCCs; 

    my $this_query =
    sprintf( qq{%s %s "%s"}, $origin, $query->type, $query->name );
    if ($is_broken) {       # Three code system: pos value for broken
        $log->warning( $this_query, 'is broken' );
    } elsif ($is_changed) {
        $log->info( $this_query, 'has been updated' );
        $is_broken = 0;     # Three code system: 0 for ok, but changed
    } else {
        $log->info( $this_query, 'is unchanged' );
        $is_broken = undef; # Three code system: undef for unchanged
    }

    return $query, $is_broken;
}

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
        ($new_q, $is_broken) = update_query($q, $origin);
        unless ($is_broken) {
            $ret_xml  = eval {$new_q->to_xml}; # it still might break
        }
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
        my $new_class_name = update_path($old_class_name);
        if ($new_class_name) {
            unless ($new_class_name eq $old_class_name) {
                $log->info($CHANGE, $origin, $type, ':',
                    $old_class_name, '=>', $new_class_name);
                $changed++;
            }
            push @new_class_names, $new_class_name;
        }
        else {
            if (dead($old_class_name)) {
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
    my $translation = update_path($bag_type);
    if ($translation) {
        unless ($bag_type eq $translation) {
            $naked->{type} = $translation;
            $log->info($CHANGE, $origin, $type, ':', $bag_type, '=>', $translation);
            $changed++;
        }
    } else {
        if (dead($bag_type)) {
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

sub translate_item { # takes in xml, returns xml
    my ($type, $xml, $origin) = @_;
    my $hash_ref = undress($type, $xml);
    delete($hash_ref->{_content});
    my ($class_name, $id, $class, $changed); 

    if ($type eq 'item') {
        $class_name = \$hash_ref->{implements};
        $id         = $hash_ref->{id};
    }
    elsif ($type eq 'class') {
        $class_name = \$hash_ref->{className};
        $id         = $hash_ref->{className};
    }
    else {
        croak "This sub doesn't only does classes and items: I got $type";
    }

    unless ($class = check_class_name($$class_name)) {
        my $translation = update_path($$class_name);

        if ($translation and $class = check_class_name($translation)) {
            $log->info($CHANGE, $origin, $type, $id, ':', $$class_name, '=>', $translation);
            $$class_name = $translation;
            $changed++;
        }
        else {
            if (dead($$class_name)) {
                $log->info($DEL, $origin, 'anticipated deletion of', $type, $id, 
                    ': could not find', $$class_name);
            }
            else {
                $log->warning($DEL, $origin, 'unexpected deletion of', $type, $id, 
                    ': could not find', $$class_name);
            }
            return $nothing, $broken;
        }
    }

    foreach my $field (@{$hash_ref->{attribute}}, 
        @{$hash_ref->{reference}},
        @{$hash_ref->{fields}[0]{fieldconfig}}) {
        my $field_ref = ($field->{name}) ? \$field->{name} : \$field->{fieldExpr};
        my $path = join '.', $class, $$field_ref;
        my $translation = update_path($path);
        if (defined $translation) {
            my $cn = $class->name;
            $translation =~ s/$cn\.//; # strip off the classname
            unless ($translation eq $$field_ref) {
                $log->info($CHANGE, $origin, $type, $id, ':', 
                    "$path => $translation");
                $changed++;
            }
            $$field_ref   = $translation;
        }
        else {
            $log->warning($DEL, $origin, $type, $id, 
                ": Broken by deletion of $path");
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
        my $new_path = update_path($old_path, '');
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
        my $new_path = update_path($old_path, '');
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
    my $class = check_class_name($class_name);
    unless ($class) {
        my $new_class_name = update_path($class_name, '');
        if ($new_class_name 
                and $new_class_name ne $class_name
                and $class = check_class_name($new_class_name) ) {
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
            if (my $new_path = update_path($class_name . '.' . $field_name, '')) {
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
    rename($file, $backup);
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
