package Webservice::InterMine::Query::Roles::Templateable;

use Moose::Role;

requires qw(all_constraints);
use Webservice::InterMine::Query::Template;
use Webservice::InterMine::TemplateConstraintFactory;
use Webservice::InterMine::Query::TemplateHandler;

=head2 to_template

Converts a query to a new template query. When converting
from a non-template, all constraints will be set to 'editable' 
and 'locked' (the default values).

=cut 

sub to_template {
    my $self  = shift;
    my %attr = %$self;
    $attr{constraint_factory} 
        = Webservice::InterMine::TemplateConstraintFactory->new;
    $attr{handler}
        = Webservice::InterMine::Query::TemplateHandler->new;
    my $clone = bless {%attr}, 'Webservice::InterMine::Query::Template';
    $clone->{constraints} = [];
    $clone->suspend_validation;
    for my $con ($self->all_constraints) {
        my %args = %$con;
        $clone->add_constraint(%$con);
    }
    $clone->resume_validation;
    return $clone;
}

1;
