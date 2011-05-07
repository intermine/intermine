package Webservice::InterMine::Path;

=head1 NAME

Webservice::InterMine::Path - functions for finding problems with paths

=head1 SYNOPSIS

    use Webservice::InterMine::Path qw(:validate);

    my @errors;
    push @errors, validate_path($model, $path_string);
    push @errors, end_is_class($model, $path_string);
    push @errors, b_is_subclass_of_a($model, $path_stringA, $path_stringB);
    confess @errors if @errors;

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

=head1 FUNCTIONS

=cut

use Exporter 'import';

my @validators = qw(validate_path end_is_class b_is_subclass_of_a a_is_subclass_of_b root);
our @EXPORT_OK = ( @validators, 'type_of', 'class_of', 'next_class', 'resolve');
our %EXPORT_TAGS = ( validate => \@validators );

use strict;
use InterMine::Model::Attribute;
use Carp qw/confess croak/;

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
    my ( $model, $string, $types) = @_;
    my $bit = last_bit($model, $string, $types);
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
    my $end = eval { last_bit(@_) };
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
    my ($root) = _parse(@_);
    return $root;
}

sub _parse {
    my ( $model, $path_string, $type_hashref ) = @_;

    $type_hashref ||= {};

    # split Path.string into 'Path', 'string'
    my @bits = split /\./, $path_string;
    my @parts = ();    # <-- the classdescriptors will go here

    my $top_class_name = shift @bits;
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
                if ( my $type = $type_hashref->{ $current_class } ) {
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
