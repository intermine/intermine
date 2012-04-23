
=head1 NAME

InterMine::Model::Collection - represents a collection in an InterMine class

=head1 SYNOPSIS

 use InterMine::Model::Collection
 ...
 my $field = InterMine::Model::Collection->new(name => 'genes',
                                               model => $model,
                                               referenced_type_name =>
                                                    $ref_type,
                                               reverse_reference_name =>
                                                    $reverse_reference);
 ...

=head1 DESCRIPTION

Objects of this class describe the collections of a class
in an InterMine model.  Collection objects are generally part of
ClassDescriptor objects.


=cut
package InterMine::Model::Collection;

use Moose;
extends 'InterMine::Model::Reference';

=head1 CONSTANTS

=head2 TAG_NAME

the name for serialising references to xml

=cut

use constant TAG_NAME => "collection";

override '_get_moose_type' => sub {
    my $self = shift;
    return 'ArrayOf' . super;
};

override '_get_moose_options' => sub {
    my $self = shift;
    my @ops = super;
    my $push_method = "add" . ucfirst($self->name);
    my $get_method = "get" . ucfirst($self->name);
    my $size_method = $self->name . "_count";
    my $empty_method = $self->name . "_is_empty";

    $push_method =~ s/s$//;
    $get_method =~ s/s$//;
    $get_method .= "ByIndex";
    my $handles = {};
    $handles->{$push_method} = "push";
    $handles->{$get_method} = "get";
    $handles->{$size_method} = "count";
    $handles->{$empty_method} = "is_empty";

    push @ops, (
        traits => ['Array'], 
        auto_deref => 1, 
        handles => $handles,
        default => sub { [] },
    );
    return @ops;
};

__PACKAGE__->meta->make_immutable;
no Moose;
1;

__END__

=head1 SEE ALSO

=over 4

=item L<InterMine::Model::ClassDescriptor>

=item L<InterMine::Model::Reference>

=item L<InterMine::Model::Role::Field>

=item L<InterMine::Model::Role::Descriptor>

=back

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Model::Collection

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2006,2007,2008,2009,2010,2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.
