package Webservice::InterMine::ResultRow;

=head1 NAME

Webservice::InterMine::ResultRow

a class for providing a unified hash and array reference style interface
for result rows.

=head1 SYNOPSIS

    # Rows should be requested as results for a query.

    use Webservice::InterMine;

    my $query = resultset("Gene")->select("symbol", "proteins.name");

    while (my $row = <$query>) {
        # The following are equivalent:
        print $row->{symbol}, $row->{proteins.name}, "\n";
        print $row->[0], $row->[1];
        print @$row;
    }

=head1 DESCRIPTION

This class exists to unify access to row based data regardless 
of whether you want to use key or column index based lookups for the 
data. This means you do not have to decide in advance which 
mechanism you want to use for retrieving data from a webservice.

=cut

use strict;
use warnings;

use Scalar::Util qw/looks_like_number/;
use Class::InsideOut qw(private readonly new id);

use overload (
    '""' => 'to_string',
    '@{}' => 'to_aref',
    '%{}' => 'to_href',
    fallback => 1,
);

readonly cells => my %cells;
private views => my %views;
private root => my %root;
private aref => my %aref;
private href => my %href;
private key_to_index => my %key_to_index;

=head1 METHODS

=head2 keys

Return the keys for this row, ie. the columns selected for out-put.

=cut

sub keys {
    my $self = shift;
    return @{$views{id $self}};
}

sub _head_and_tail {
    my $in = shift;
    return split(/\./, $in, 2);
}

=head2 to_string

Provides a readable representation of the data in this row.

=cut

sub to_string {
    my $self = shift;
    my $id = id $self;
    my $string = $root{$id};
    unless ($string) {
        ($string) = _head_and_tail($views{$id}[0]);
        $root{$id} = $string;
    }
    for my $view ($self->keys) {
        my (undef, $headless) = _head_and_tail($view);
        my $value = $self->get_value($view);
        $value = 'undef' unless (defined $value);
        $string .= "\t$headless: $value"
    }
    return $string;
}

=head2 get_value($key | $index)

If a string is provided, than a hash-style key-lookup will return
the value for the matching column, and if an integer is provided, then 
the appropriate value will be selected via a array based lookup.

=cut

sub get_value {
    my ($self, $idx) = @_;
    unless (looks_like_number($idx)) {
        $idx = $self->_index_for($idx);
    } 
    my $len = @{ $self->cells };
    if ($idx <= -$len || $idx >= $len) {
        # We don't want the default behaviour, 
        # because we want "not in array" and "undef" to 
        # be distinct.
        die "Index Error: $idx out of range";
    }
    my $cell = $self->cells->[$idx];
    return ((ref $cell eq "HASH") ? $cell->{value} : $cell);
}

=head2 to_aref

Return this row as an array-reference. This returns a copy
of the data in the row.

=cut

sub to_aref { 
    my $self = shift; 
    my $id = id $self;
    if (my $aref = $aref{$id}) {
        return $aref;
    } else {
        my $aref = [map {ref($_) eq 'HASH' ? $_->{value} : $_} @{$self->cells}];
        return $aref{$id} = $aref;
    }
}

=head2 to_href($style)

return this row as a hash-reference. This returns a copy of the
data in the row.

The three available styles are "full", "short" and "long", depending
on whether you want "long" keys ("Gene.proteins.name") or "short" ones
("proteins.name"). "full" gives both, and is the default.

=cut

sub to_href { 
    my $self = shift; 
    my $style = lc(shift || "full");
    my $id = id $self;
    my $href;
    unless ($href = $href{$id}) {
        $href = {map {$_ => $self->get_value($_)} $self->_available_keys};
        $href{$id} = $href;
    }
    if ($style eq "short") {
        return {map {substr($_, index($_, ".") + 1) => $self->get_value($_)} $self->keys};
    } elsif ($style eq "long") {
        return {map {$_ => $self->get_value($_)} $self->keys};
    } else {
        return $href;
    }
}

## Internal logic ##

sub _available_keys {
    my $self = shift;
    my $h = $key_to_index{id $self} || $self->_build_key_to_index_map;
    return CORE::keys %$h;
}

sub _index_for {
    my $self = shift;
    my $key  = shift;
    my $h = $key_to_index{id $self} || $self->_build_key_to_index_map;
    return $h->{$key};
}

sub _build_key_to_index_map {
    my $self = shift;
    my $h = {};
    my $c = 0;
    for my $v ($self->keys) {
        my (undef, $headless) = _head_and_tail($v);
        $h->{$v} = $c;
        $h->{$headless} = $c;
        $c++;
    }
    return $key_to_index{id $self} = $h
}

1;

__END__

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

