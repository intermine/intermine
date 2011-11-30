package Webservice::InterMine::Constraint;

=head1 NAME

Webservice::InterMine::Constraint - A base class for all constraints.

=head1 DESCRIPTION

All constraints inherit from this class, and thus share its properties
and behaviour.

=cut

use Moose;
use warnings FATAL => 'misc';    # We want bad hash assignment to die

extends 'Webservice::InterMine::PathFeature';

=head2 requirements_are_met_by(@args)

Returns true if the arg list passed to it would meet the requirements of the class
Note: this DOES NOT GUARANTEE that the arg list is valid, as INVALID args may be present
as well - but is used for constraint class selection in the query classes

=cut

sub requirements_are_met_by {
    my $class = shift;
    my %args  = @_;
    my @required_attributes =
      grep { $_->is_required } $class->meta->get_all_attributes;
    my $matches = 0;
    for my $name ( keys %args ) {
        if ( my ($attr) =
            grep { $name eq $_->name } @required_attributes )
        {
            my $tc = $attr->type_constraint;
            $matches++ # don't die, because that kind of defeats the point
              if eval {$tc->check($args{$name}) || $tc->assert_coerce($args{$name})};
        }
    }
    return ( $matches == @required_attributes );
}

sub _build_element_name {
    return 'constraint';
}

__PACKAGE__->meta->make_immutable;
no Moose;
1;
