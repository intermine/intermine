package Webservice::InterMine::ConstraintFactory;

use Module::Find;
use Carp qw(confess);

my @constraint_classes = usesub Webservice::InterMine::Constraint;

sub new {
    my $class = shift;
    return bless {}, $class;
}

sub constraint_classes {
    return @constraint_classes;
}

sub make_constraint {
    my ( $self, @args ) = @_;
    my $class = $self->get_constraint_class(@args);
    return $class->new(@args);
}

sub get_constraint_class {
    my $self = shift;
    my @args = @_;
    for (@constraint_classes) {
        return $_ if $_->requirements_are_met_by(@args);
    }
    confess "No suitable constraint class found";
}

1;
