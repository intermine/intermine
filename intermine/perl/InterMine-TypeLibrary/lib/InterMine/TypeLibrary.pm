package InterMine::TypeLibrary;
{

    our $VERSION = '0.9701';

=head1 NAME

InterMine::TypeLibrary - a MooseX::Types library

=head1 SYNOPSIS

  use InterMine::TypeLibrary qw(Model Join);
  ...

=head1 DESCRIPTION

This module supplies MooseX::Type types for use by Webservice::InterMine modules

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::TypeLibrary;

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

    # Declare Our Own Types
    use MooseX::Types -declare => [
        qw(
          File
          LogHandler
          DirName PathClassDir
          )
    ];

    # Import built-in Moose types
    use MooseX::Types::Moose qw/Str ArrayRef HashRef Undef Maybe Int Value Object/;
    use Scalar::Util qw(blessed);

    subtype DirName, as Str, where {-d $_}, 
        message {"'$_' should be the name of an existing directory"};
    class_type PathClassDir, { class => 'Path::Class::Dir'};

    # Type coercions

    coerce DirName, from PathClassDir, via {$_->stringify};
}
__PACKAGE__->meta->make_immutable;

1;
