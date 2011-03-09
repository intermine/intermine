package InterMine::Model::Types;


# Declare Our Own Types
use MooseX::Types -declare => ["ISO8601DateStamp"];

use MooseX::Types::Moose qw/Str Bool Object/;

subtype ISO8601DateStamp, as Str, 
    where {/ 
            \d{4} - (0[1-9]|1[012]) - (1[1-9]|2[0-9]|3[0-1]) 
            T 
            ([01][0-9]|2[0-4]) : [0-5][0-9] : [0-5][0-9]
            /x}, 
    message {"Value provided ('$_') was not in the ISO8601 time stamp format"};

coerce Bool, from Object, via {$$_};

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

