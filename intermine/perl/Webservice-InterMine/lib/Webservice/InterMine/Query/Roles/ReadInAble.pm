package Webservice::InterMine::Query::Roles::ReadInAble;

use Moose::Role;
use Webservice::InterMine::Types qw(File QueryHandler);
use MooseX::Types::Moose qw(Str);

use XML::Parser::PerlSAX;

=head1 NAME 

Webservice::InterMine::Query::Roles::ReadInAble - Composable behaviour for deserialisable queries

=head1 DESCRIPTION 

This module provides composable behaviour for deserialising queries

=head1 REQUIRES

This module demands its consumer provide the following methods:

=over 4

=item * add_constraint
=item * add_join
=item * add_pathdescription
=item * _build_handler
=item * logic
=item * name
=item * sort_order
=item * view

=back

=cut

requires (
    qw/name view sort_order logic
       _build_handler add_pathdescription
       add_join add_constraint/
);

=head1 ATTRIBUTES

=head2 handler (QueryHandler, ro)

The object responsible for parsing the query. The consumer defines this by
providing a builder

=cut

has handler => (
    is         => 'ro',
    isa        => QueryHandler,
    lazy_build => 1,
);

=head2 source_string (Str, rw)

The source xml for this query. Setting this attribute causes the query to
reinitialise.

=cut

has source_string => (
    is => 'rw',
    isa => Str,
    trigger => \&_process_xml,
);

=head2 source_file (File, ro)

The source file for this query. When provided, it will set the source string.

=cut

has source_file => (
    is => 'ro',
    isa => File,
    trigger => \&_set_xml,
);

=head1 METHODS

=head2 _set_xml (private)

# Read the xml from the source file and set the source string

=cut

sub _set_xml {
    my $self = shift;
    my $file = shift;
    open (my $XMLFH, '<', $file)
	or confess "Cannot read from xml file, $!";
    my $xml = join('', <$XMLFH>);
    close $XMLFH
	or confess "EEK, what happened there? $!";
    $self->source_string($xml);
}

=head2 _process_xml (private)

=cut

# Process the xml and initialise the query

sub _process_xml {
    my $self = shift;
    my $handler = $self->handler;
    $handler->query($self);
    my $parser  = XML::Parser::PerlSAX->new(Handler => $handler);
    $parser->parse(Source => {String => $self->source_string});
}

1;

__END__

=head1 SEE ALSO

=over 4

=item * L<Webservice::InterMine::Cookbook> for guide on how to use these modules.

=item * L<Webservice::InterMine::Query>

=item * L<Webservice::InterMine::Service>

=item * L<Webservice::InterMine::Query::Template>

=item * L<Webservice::InterMine::Query::Saved>

=back

=head1 AUTHOR

Alex Kalderimis C<< <dev@intermine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine::Query::Roles::ReadInAble

You can also look for information at:

=over 4

=item * Webservice::InterMine

L<http://www.intermine.org>

=item * Documentation

L<http://www.intermine.org/perlapi>

=back

=head1 COPYRIGHT AND LICENSE

Copyright 2006 - 2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=cut
