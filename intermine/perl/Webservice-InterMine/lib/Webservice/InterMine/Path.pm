package Webservice::InterMine::Path;

=head1 NAME

Webservice::InterMine::Path - functions for finding problems with paths

=head1 SYNOPSIS

For validation using functions/static methods:

    use Webservice::InterMine::Path qw(:validate);

    my @errors;
    push @errors, validate_path($model, $path_string);
    push @errors, end_is_class($model, $path_string);
    push @errors, b_is_subclass_of_a($model, $path_stringA, $path_stringB);
    confess @errors if @errors;

For queries for path based information from query services:

    use Webservice::InterMine;

    my $service = Webservice::InterMine->get_testmine;
    my $path = $service->new_path('Department.employees.name');
    my $has_guessed_correctly = 0;
    for my $name ($path->get_possible_values) {
        last if $has_guessed_correctly;
        print "Is your name $name?: y|N:";
        chomp(my $resp = <STDIN>);
        $has_guessed_correctly++ if ($resp =~ /y(es)?/i);
    }

=cut

use Exporter 'import';

my @validators = qw(validate_path end_is_class b_is_subclass_of_a a_is_subclass_of_b root);
our @EXPORT_OK = ( @validators, 'type_of', 'class_of', 'next_class', 'resolve');
our %EXPORT_TAGS = ( validate => \@validators );

use strict;
use InterMine::Model::Attribute;
use Carp qw/confess croak/;

use overload (
    '""' => 'to_string',
    fallback => 1,
);

=head1 METHODS

=head2 new(Str path, Service service, [HashRef subtypes])

Construct a new path object for use with path based 
webservice. The path is immediately validated before use, 
so any subclass constraints that affect this path need 
to be included in the subtypes hash. 

This constructor is not meant to be used directly; rather, obtain
Webservice::InterMine:Path objects from their respective Service objects
via their C<new_path> methods.

=cut 

sub new {
    my $class = (ref $_[0]) ? ref shift : shift;
    my ($path, $service, $subtypes) = @_;
    $subtypes ||= {};
    my $self = {path => $path, service => $service, subtypes => $subtypes};

    $self->{parts} = [_parse($service->model, $path, $subtypes)];
    return bless $self, $class;
}

sub to_string {
    my $self = shift;
    return $self->{path} if $self->{path};
    my @parts = @{ $self->{parts} };

    join ".", $parts[0]->unqualified_name, map {$_->name} @parts[1 .. @parts - 1];
}

=head2 get_results_iterator([$format])

Return an object for iterating over rows of results. 
The formats that are supported by the possible values service
are jsonobjects (the default), and count formats. However, for accessing
counts, and even values, it is probably easier to use the 
convenience methods listed below.

=cut

sub get_results_iterator {
    my $self = shift;
    my $format = shift || 'jsonobjects';
    my $service = $self->{service};
    my $uri = $service->root . $service->POSSIBLE_VALUES_PATH; 
    require JSON;
    my $json = JSON->new;
    my $params = {
        path => $self->{path}, 
        typeConstraints => $json->encode($self->{subtypes}),
    };

    my $iter = $service->get_results_iterator(
        $uri, $params, [], $format, 'perl', []);
    return $iter;
}

=head2 set_subtype($key => $value)

Paths can be refined by adding subtype constraints after they have been
constructed. EG:

 my $path = $service->new_path("Department.employees.name")
 # now the path represents that names of employees

 $path->set_subtype("Department.employees" => "CEO");
 # And now it represents the names of CEOs

=cut

sub set_subtype {
    my $self = shift;
    my ($k, $v) = @_;
    return $self->{subtypes}{$k} = $v;
}

=head2 get_possible_values()

Returns the values this path may potentially have. Be aware
that in list context it returns, as expected, as list of values, whereas
in scalar context it resturns an Array-Reference to that list of
values. If you just want the number of items in the list, use 
C<get_possible_values_count> instead, which is much more efficient.

=cut

sub get_possible_values {
    my $self = shift;
    my $iter = $self->get_results_iterator;
    my @values = map {$_->{value}} $iter->get_all;
    if (wantarray) {
        return @values;
    } else {
        return [@values];
    }
}

=head2 get_possible_values_count()

Returns the number of different values this path may represent. 
This is the most efficient way to retrieve this information from the server.

=cut

sub get_possible_values_count {
    my $self = shift;
    my $iter = $self->get_results_iterator('count');
    return join('', $iter->get_all);
}

=head2 end_is_attribute()

Return true if this object represents a path that ends in an 
attribute.

=cut

sub end_is_attribute {
    my $self = shift;
    my $end = $self->last_bit;
    if (class_of($end)) {
        return 0;
    } else {
        return 1;
    }
}

=head2 prefix()

Return the path before this one. eg, for "Gene.exons.name", return
a path representing "Gene.exons". The resulting path with have all the same
data as this one, including subclass information.

=cut

sub prefix {
    my $self = shift;
    my %hash = %{ $self };
    delete $hash{path};
    my @parts = @{ $self->{parts} };
    pop @parts;
    die $self->to_string . " has no prefix" unless @parts;
    $hash{parts} = [@parts];
    $hash{subtypes} = { %{ $self->{subtypes} } };
    return bless \%hash, (ref $self);
}

=head2 append(@parts)

Return a path representing a path made from this one with further
parts added on. Eg, for a path representing "Gene.exons", a call to 
C<< $path->append("name") >> should return a path representing 
"Gene.exons.name".

=cut

sub append {
    my ($self, @parts) = @_;
    return Path->new($self->to_string . "." . join('.', @parts), 
        $self->{service}, $self->{subtypes});
}

=head1 FUNCTIONS

=head2 validate_path

 Usage   : validate_path($model, 'Department.name');
 Function: Return errors for this path, nothing if the path is valid
 Args    : $model - the InterMine::Model to use for validating
           $path_string - the path in string format

=cut

sub validate_path {

    # Since errors returned should only relate to the path
    # we will confess arg errors here
    confess "Bad arguments: Too many" if ( @_ > 4 );
    confess "Bad arguments: Too few"  if ( @_ < 2 );
    confess "Bad arguments: No pathstring, or not a string"
      if ( ref $_[1] or not defined $_[1] );
    confess "Bad arguments: Third arg is not a hash"
      if ( defined $_[2] and ref $_[2] ne 'HASH' );
    confess "Bad Arguments: No model as first arg"
      if ( not $_[0]->isa('InterMine::Model') );

    eval { _parse(@_) };
    if ($@) {
        return $@;
    }
    else {
        return undef;
    }
}

=head2 last_bit

 Usage   : last_bit($model, 'Department.name');
 Function: Returns the metaclass for the last part of the path-string
 Args    : $model - the InterMine::Model to use for validating
           $path_string - the path in string format

=cut

sub last_bit {
    # Called as method
    return $_->{parts}[-1] if (@_ == 1);
    # Called as fn.
    my ( $model, $path_string, $types) = @_;
    my @bits = _parse( $model, $path_string, $types);
    return $bits[-1] || $bits[0];
}

sub last_bit_but_one {
    my ( $model, $path_string, $types) = @_;
    my @bits = _parse( $model, $path_string, $types);
    return $bits[-2] || $bits[0];
}
sub last_class_type {
    my ( $model, $path_string ) = @_;
    my $end = last_bit_but_one( $model, $path_string );
    if ( $end->isa('InterMine::Model::Reference') ) {
        return $end->referenced_type_name;
    } else {
        return $end->name();    # because it's clearly a class
    }
}

=head2 resolve

Resolves a path to a class descriptor, or an attribute descriptor.

=cut 

sub resolve {
    my $bit;
    if (@_ == 1) {
        # Calling as method
        $bit = $_[0]->{parts}[-1];
    } else {
        # Calling as fn.
        my ( $model, $string, $types) = @_;
        $bit = last_bit($model, $string, $types);
    }
    return class_of($bit) || $bit;
}

=head2 type_of

 Usage    : type_of($model, 'Department.name');
 Function : returns a string with the type that this string evaluates to
            ie: Department.name => String
                Department.employees => Employee

=cut

sub type_of {
    my ( $model, $path_string ) = @_;
    my $end = last_bit( $model, $path_string );
    if ( $end->isa('InterMine::Model::Reference') ) {
        return $end->referenced_type_name;
    }
    elsif ( $end->isa('InterMine::Model::Attribute') ) {
        return $end->attribute_type;
    }
    else {
        return $end->name();    # because it's clearly a class
    }
}

=head2 end_is_class

 Usage   : end_is_class($model, 'Department.name');
 Function: Returns an error if the last bit does not evaluate to a class (ie. is an attribute)
 Args    : $model - the InterMine::Model to use for validating
           $path_string - the path in string format

=cut

sub end_is_class {
    my $end = (@_ == 1) ? $_[0]->{parts}[-1] : eval { last_bit(@_) };
    if ($end) {
        if ( not class_of($end) ) {
            return sprintf( "%s: %s is a %s, not a class\n",
                $_[1], $end->name, $end->attribute_type, );
        }
        else {
            return undef;
        }
    }
    else {
        return validate_path(@_);
    }
}

=head2 a_is_subclass_of_b($model, $classA, $classB)

Returns undef if $classA represents a subclass of $classB, or
if they do not represent valid paths, otherwise returns a message.

=cut 

sub a_is_subclass_of_b {
    my ( $model, $path_stringA, $path_stringB ) = @_;
    return b_is_subclass_of_a($model, $path_stringB, $path_stringA);
}

=head2 b_is_subclass_of_a($model, $classA, $classB)

Returns undef if $classA represents a subclass of $classB, or
if they do not represent valid paths, otherwise returns a message.

=cut

sub b_is_subclass_of_a {
    my ( $model, $path_stringA, $path_stringB ) = @_;

    my ( $A, $B ) = eval {
        map { class_of( last_bit( $model, $_ ) ) }
          $path_stringA,
          $path_stringB;
    };
    return undef unless ( $A and $B );

    # invalid paths are not MY problem
    # - go see Mr. validate_path
    if ( $B->sub_class_of($A) ) {
        return undef;
    }
    else {
        return
          sprintf(
            "%s (which is a %s) is not a subclass of %s (which is a %s)\n",
            $path_stringB, $B->name, $path_stringA, $A->name, );
    }
}

sub root {
    if (@_ == 1) {
        my $self = shift;
        return $self->{parts}[0];
    }
    my ($root) = _parse(@_);
    return $root;
}

sub _parse {
    my ( $model, $path_string, $type_hashref ) = @_;

    $type_hashref ||= {};

    if ($ENV{DEBUG}) {
        require Data::Dumper;
        warn "SUBTYPES: " . Data::Dumper->Dump([$type_hashref]);
    }

    # split Path.string into 'Path', 'string'
    my @bits = split /\./, $path_string;
    my @parts = ();    # <-- the classdescriptors will go here

    my $top_class_name = shift @bits;
    my @processed_bits = ($top_class_name); # <-- to track what we have looked at
    confess "model is not defined" unless ( defined $model );
    push @parts, $model->get_classdescriptor_by_name($top_class_name);

    my $current_class = $parts[-1];
    my $current_field = undef;

    for my $bit (@bits) {

        if ( $bit eq 'id' and $bit eq $bits[-1] ) {
            my $id = InterMine::Model::Attribute->new(
                name        => $bit,
                type        => 'Integer',
                model       => $model,
                field_class => $current_class,
            );
            push @parts, $id;
        }
        else {
            $current_field = $current_class->get_field_by_name($bit);
            if ( !defined $current_field ) {
                my $subclass_key = join('.', @processed_bits);
                warn "COULDN'T FIND $bit, CHECKING SUBCLASSES FOR $subclass_key" if $ENV{DEBUG};
                if ( my $type = $type_hashref->{ $subclass_key } ) {
                    warn "IT MAY BE IN $type" if $ENV{DEBUG};
                    my $type_class = $model->get_classdescriptor_by_name($type);
                    $current_field = $type_class->get_field_by_name($bit);
                }
                if ( !defined $current_field ) {
                    my $message = sprintf(
                        qq{illegal path (%s): can't find field "%s" in class "%s"},
                        $path_string,
                        $bit,
                        $current_class->name(),
                    );
                    if ($ENV{DEBUG}) {
                        confess $message;
                    } else {
                        croak $message;
                    }
                }
            }
            push @parts, $current_field;
            my $type =  $type_hashref->{join('.', map {$_->name} @parts)};
            $current_class =
              next_class( $current_field, $model, $type );
        }
        push @processed_bits, $bit;
    }
    return @parts;
}

sub next_class {
    my ( $current_field, $model, $type ) = @_;
    return undef
      if $current_field->isa('InterMine::Model::Attribute');

    # if the type was given, respect it
    my $next_class;
    if ( $type ) {
        $next_class = $model->get_classdescriptor_by_name($type);
    }
    else {
        $next_class = class_of($current_field);
    }
    confess "Could not find next class for " . $current_field->name
      unless ($next_class);
    return $next_class;
}

=head2 class_of

 Usage   : class_of($instance);
 Function: Returns the meta-class that an object refers to.
 Args    : an Webservice::InterMine::Field or ClassDescriptor instance

=cut

sub class_of {
    my $thing = shift;
    if ( $thing->isa('InterMine::Model::Reference') ) {
        return $thing->referenced_classdescriptor();
    }
    elsif ( $thing->isa('InterMine::Model::ClassDescriptor') ) {
        return $thing;
    }
    else {
        return;
    }
}
1;

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine::Path

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2009,2010 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.


=cut
