package Webservice::InterMine::Query::Roles::WriteOutLegacy;

use Moose::Role;
requires(qw/to_DOM joins model type_dict/);

use List::MoreUtils qw(uniq);
use Webservice::InterMine::Path qw(type_of);
use XML::DOM;

sub to_legacy_xml {
    my $self        = shift;
    my $query       = $self->to_DOM;
    my $doc         = $query->getOwnerDocument;
    my @constraints = $query->getElementsByTagName('constraint');
    for ( $query->getElementsByTagName('join'), @constraints ) {
        $query->removeChild($_);
    }
    for (qw/view sortOrder/) {
        my $attr = $query->getAttribute($_);
        $query->setAttribute( $_ => $self->put_joins_in($attr) );
    }
    for ( $query->getElementsByTagName('pathDescription') ) {
        my $path = $_->getAttribute('pathString');
        $_->setAttribute( pathString => $self->put_joins_in($path) );
    }
    my @paths = sort {$a cmp $b} uniq map { $_->getAttribute('path') } @constraints;
    my $type_dict = $self->type_dict;
    for my $p (@paths) {
        my $type = $type_dict->{$p} || type_of( $self->model, $p );
        my $node = $doc->createElement('node');
        $node->setAttribute( path => $self->put_joins_in($p) );
        $node->setAttribute( type => $type );
        my @cons_for_this_node =
          sort { $a->getAttribute('code') cmp $b->getAttribute('code') }
          grep { $_->getAttribute('path') eq $p }
          grep { $_->getAttribute('code') } @constraints;
        for (@cons_for_this_node) {
            $_->removeAttribute('path');
            $node->appendChild($_);
        }
        $query->appendChild($node);
    }
    return $query->toString;
}

sub put_joins_in {
    my $self   = shift;
    my $string = shift;
    my %joins  = $self->join_dict;
    while ( my ( $p, $j ) = each %joins ) {
        $string =~ s/$p/$j/g;
    }
    return $string;
}

sub join_dict {
    my $self = shift;
    my %joins;
    for my $join ( $self->joins ) {
        my $joined_path = $join->path;
        if ( $join->style eq 'OUTER' ) {
            $joined_path =~ s/(.*)\./$1:/;
        }
        $joins{ $join->path } = $joined_path;
    }
    return %joins;
}

1;
