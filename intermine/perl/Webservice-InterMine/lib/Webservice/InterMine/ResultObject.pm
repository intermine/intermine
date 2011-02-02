package Webservice::InterMine::ResultObject;

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

=cut

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
