package InterMine::Model::Attribute;

=head1 NAME

InterMine::Model::Attribute - represents an attribute of an InterMine class

=head1 SYNOPSIS

  use InterMine::Model::Attribute;

  ...
  my $field = InterMine::Model::Attribute->new(name => 'age', model => $model,
                                               type => 'Integer');

  ...

=head1 DESCRIPTION

Objects of this class describe the attributes of class in an InterMine model.
Attribute objects are generally part of ClassDescriptor objects.

=cut

use Moose;
with (
    'InterMine::Model::Role::Field',
    'InterMine::Model::Role::Descriptor',
);

use MooseX::Types::Moose qw(Str Int Num Bool Value);
use InterMine::Model::Types qw(BigInt ISO8601DateStamp);

=head1 ATTRIBUTES

=head2 type (Str)

=head3 reader: java_type

The java type of the attribute (Integer, java.lang.String, etc...)

=cut

has type => (
    reader   => 'java_type',
    isa	     => Str,
    required => 1,
);

=head2 attribute_type

The java_type of the attribute, stripped of any dotted bits 
(Date, String, int, etc)

=cut

sub attribute_type {
    my $self = shift;
    my $value = $self->java_type;
    $value =~ s/.*\.//;
    return $value;
}

my %moose_translation_for = (
    string  => Str,
    short   => Int,
    integer => Int,
    int     => Int,
    long    => BigInt,
    double  => Num,
    float   => Num,
    boolean => Bool,
    date    => ISO8601DateStamp,
);

sub _get_moose_type {
    my $self = shift;
    my $type = lc($self->attribute_type);
    if (exists $moose_translation_for{$type}) {
        return $moose_translation_for{$type};
    } else {
        return Value; # The broadest possible scalar type
    }
}

sub _get_moose_options {
    my $self = shift;
    my @options = (isa => $self->_get_moose_type);
    if ($self->attribute_type =~ /^(?:long|boolean)$/i) {
        push @options, (coerce => 1);
    }
    return @options;
}

=head1 METHODS

=head2 to_xml

The xml representation of the attribute descriptor

=cut

sub to_xml {
    my $self = shift;
    return sprintf(qq{<attribute name="%s" type="%s"/>},
        $self->name, $self->java_type);
}

__PACKAGE__->meta->make_immutable;
no Moose;
1;

__END__

=head1 SEE ALSO

=over 4

=item L<InterMine::Model::ClassDescriptor>

=item L<InterMine::Model::Role::Field>

=item L<InterMine::Model::Role::Descriptor>

=back

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Model::Attribute

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2006,2007,2008,2009,2010,2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.
