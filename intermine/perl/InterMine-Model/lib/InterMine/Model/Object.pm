package InterMine::Model::Object;

use Moose;
use MooseX::Types::Moose qw(Str);

=head1 NAME

InterMine::Model::Object - the class all instantiated objects inherit from

=head1 SYNOPSIS

 # Do not use this class directly.

 use Test::More;

 my $gene = $model->make_new("Gene");

 can_ok($gene, qw/getObjectId hasObjectId class equals merge/);

=head1 DESCRIPTION

A base class for instantiated objects.

=head2 ATTRIBUTES

=head2 objectId

The internal object id

=over 4

=item reader: getObjectId

get the object id attribute.

=item predicate: hasObjectId

returns true if the object has an id.

=back 

=cut

has objectId => (
    reader => "getObjectId",
    predicate => "hasObjectId",
    isa => Str,
);

=head2 id

Read-Only synonym for getObjectId

=cut

sub id { 
    my $self = shift;
    return $self->getObjectId;
}

=head1 METHODS 

=head2 equals($something)

returns true if the other object has the same objectId

=cut 

sub equals {
    my ($self, $other) = @_;
    if (blessed $other && $other->isa(__PACKAGE__)) {
        return 
            ($self->hasObjectId && $other->hasObjectId && 
                $self->getObjectId eq $other->getObjectId);
    } else {
        return 0;
    }
}

=head2 merge($other)

merge the properties of other into self

=cut

sub merge {
    my ($self, $other) = @_;
    for my $attr ($other->class->get_all_attributes) {
        my $reader = $attr->get_read_method;
        my $writer = $attr->get_write_method or next;
        if (my $value = $other->$reader()) {
            next if (ref $value eq 'ARRAY' and not @$value);
            $self->$writer($value);
        }
    }
}

=head2 class

make class a synonym for meta

=cut

sub class {
    my $self = shift;
    return $self->meta;
}

=head2 isa

overrides isa to respond to unqualified names as well as full ones.

=cut

sub isa {
    my $self = shift;
    my $other = shift;
    my $extended_name = $self->class->model->{perl_package} . $other;
    if (UNIVERSAL::isa($self, $extended_name)) {
        return 1;
    } else {
        return UNIVERSAL::isa($self, $other);
    }
};

=head1 OVERLOADING

=head2 EQUALITY (==, eq)

return true if objectIds match

=cut

use overload (
    '==' => 'equals',
    'eq' => 'equals',
    fallback => 1,
);

__PACKAGE__->meta->make_immutable;
no Moose;
1;

=head1 SEE ALSO

=over 4

=item * L<Moose::Object>

=back

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Model::Object

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2006,2007,2008,2009, 2010, 2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

