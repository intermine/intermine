package Webservice::InterMine::ResultRow;

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

sub keys {
    my $self = shift;
    return @{$views{id $self}};
}

sub _head_and_tail {
    my $in = shift;
    return split(/\./, $in, 2);
}

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

sub get_value {
    my ($self, $idx) = @_;
    unless (looks_like_number($idx)) {
        $idx = $self->_index_for($idx);
    } 
    my $cell = $self->cells->[$idx]
        or die "$idx out of range";
    return $cell->{value};
}

sub to_aref { 
    my $self = shift; 
    my $id = id $self;
    if (my $aref = $aref{$id}) {
        return $aref;
    } else {
        my $aref = [map {$_->{value}} @{$self->cells}];
        return $aref{$id} = $aref;
    }
}

sub to_href { 
    my $self = shift; 
    my $id = id $self;
    if (my $href = $href{$id}) {
        return $href;
    } else {
        my $href = {map {$_ => $self->get_value($_)} $self->_available_keys};
        return $href{$id} = $href;
    }
}

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

