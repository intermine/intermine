package Webservice::InterMine::TemplateConstraintFactory;

use base 'Webservice::InterMine::ConstraintFactory';

sub get_constraint_class {
    my $self      = shift;
    my $class     = $self->SUPER::get_constraint_class(@_);
    my $metaclass = $class->meta->create_anon_class(
        superclasses => [$class],
        roles =>
          ['Webservice::InterMine::Constraint::Role::Templated'],
        cache => 1,
    );
    return $metaclass;
}

sub make_constraint {
    my ( $self, %args ) = @_;
    my $metaclass = $self->get_constraint_class(%args);

    # is_editable is assumed 'true' if not stated
    # NB this is the reverse of the way this attribute is treated
    # when reading from xml (only editable if explicitly editable="true"
    # however, when users add constraints, we assume they are editable
    $args{is_editable} = 1 unless ( exists $args{is_editable} );
    return $metaclass->new_object(%args);
}

1;
