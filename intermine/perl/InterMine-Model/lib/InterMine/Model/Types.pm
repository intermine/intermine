package InterMine::Model::Types;


# Declare Our Own Types
use MooseX::Types -declare => [
    qw(
        ISO8601DateStamp

        JoinedPathString PathString PathList PathHash

        Model

        ClassDescriptor ClassDescriptorList MaybeClassDescriptor

        Field FieldList MaybeField FieldHash

        BigInt
    )
];

use MooseX::Types::Moose qw/Str Bool Object ArrayRef HashRef Maybe/;
use Math::BigInt;

# FOR INTERNAL USE

subtype ISO8601DateStamp, as Str, 
    where {/ 
            \d{4} - (0[1-9]|1[012]) - (1[1-9]|2[0-9]|3[0-1]) 
            T 
            ([01][0-9]|2[0-4]) : [0-5][0-9] : [0-5][0-9]
            /x}, 
    message {"Value provided ('$_') was not in the ISO8601 time stamp format"};

coerce Bool, from Object, via {$$_};

# MODEL

class_type Model,   { class => 'InterMine::Model', };
coerce Model, from Str, via {
    require InterMine::Model;
    InterMine::Model->new( string => $_ );
};

# CLASSES

class_type ClassDescriptor,
    { class => 'InterMine::Model::ClassDescriptor', };
subtype MaybeClassDescriptor, as Maybe    [ClassDescriptor];
subtype ClassDescriptorList,  as ArrayRef [ClassDescriptor];

# FIELDS

role_type Field, { role => 'InterMine::Model::Role::Field', };
subtype MaybeField, as Maybe[Field];
subtype FieldList,  as ArrayRef[Field];
subtype FieldHash,  as HashRef[Field];

# PATH

my $path_re = qr/([[:alnum:]_]+\.)*[[:alnum:]_]+/;
subtype JoinedPathString, as Str, where { /^ ( $path_re (,?\s*|\s+) )+ $path_re $/x };
subtype PathString,
    as Str, where { /^$path_re$/ },
    message {
    (defined)
        ? "PathString can only contain 'A-Z', '0-9', '_' and '.', not '$_'"
        : "PathString must be defined";
    };
subtype PathList, as ArrayRef [PathString];
subtype PathHash, as HashRef  [PathString];
coerce PathList, from JoinedPathString, via { [ split /[,\s]+/ ] };
coerce PathString, from JoinedPathString,
    via { ( split /[\s]+/ )[0] };
coerce PathString, from ClassDescriptor, via { "$_" };

# Attribute values

class_type BigInt, {class => "Math::BigInt"};

coerce BigInt, from Str, via {
    my $coerced = Math::BigInt->new($_);
    if ($coerced->is_nan) {
        return $_; # We almost certainly failed here...
    } else {
        return $coerced;
    }
};

1;

=head1 NAME

InterMine::Model::Types - types used by InterMine::Model or its subclasses.

=head1 SYNOPSIS

 use Moose;
 use InterMine::Model::Types qw(ISO8601DateStamp);

 has time_stamp => (
   isa => ISO8601DateStamp,
   is => 'rw',
 );
 
 ...

=head1 DESCRIPTION

A MooseX::Types type library. Not for direct external use.

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Model::Types

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2006,2007,2008,2009 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

