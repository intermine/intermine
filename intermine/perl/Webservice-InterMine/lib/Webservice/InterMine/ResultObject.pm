package Webservice::InterMine::ResultObject;

sub AUTOLOAD {
    my $self = shift;
    my $called = our $AUTOLOAD;
    $called =~ s/.*:://;
    if ($called eq "DESTROY") {
        return;
    }

    if (exists $self->{$called}) {
        my $prop = $self->{$called};
        if (ref $prop eq "ARRAY" and wantarray) {
            return @$prop;
        } else {
            return $prop;
        }
    } else {
        die "No property named $called on this object";
    }
}

1;

=head1 NAME

Webservice::InterMine::ResultObject - a class for inflating jsonobjects into

=head1 SYNOPSIS

    # This package should not be used directly, but if you did:

    use Webservice::InterMine::ResultObject;

    my $hashref = {
        foo => "bar",
        bop => "bip",
        quuxes => [
            {zip => 1, zop => 2},
            {zip => 3, zop => 4}
        ]
    };
    bless $hashref, "Webservice::InterMine::ResultObject";
    for my $quux ($hashref->quuxes) {
        bless $quux, "Webservice::InterMine::ResultObject";
    }

    # Now:

    use Test::More;
    is($hashref->bar, "bar");
    is($hashref->bop, "bip");
    is(hashref->quuxes->[0]->zip, 1);
    # and so on...

=head1 DESCRIPTION

This package is used to provide inflated objects with property accessors 
as results from queries. It is used by Runnable queries to generate the
appropriate results.

=head2 Usage

A hashref blessed into this class will have accessors for any
of its keys. It will automatically dereference arrayref values when 
called in list context.

=head1 METHODS

This package has no methods as such, but uses AUTOLOAD to handle 
method calls.

=head1 SEE ALSO

=over 4

=item * L<Webservice::InterMine::Cookbook> - A guide to using the Webservice::InterMine Perl API

=item * L<Webservice::InterMine::Query>

=item * L<Webservice::InterMine>

=item * L<Webservice::InterMine::Service>

=back

=head1 AUTHOR

Alex Kalderimis C<dev@intermine.org>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine::ResultObject

You can also look for information at:

=over 4

=item * InterMine

L<http://www.intermine.org>

=item * Documentation

L<http://www.intermine.org/perlapi>

=back

=head1 COPYRIGHT AND LICENSE

Copyright 2006 - 2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

