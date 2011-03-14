package Webservice::InterMine::Query::Roles::TemplateParameters;

use Moose::Role;

requires qw(service name editable_constraints);

sub resource_path {
    my $self = shift;
    return $self->service->TEMPLATEQUERY_PATH;
}

sub get_request_parameters {
    my $self = shift;
    
    my %request_parameters = (name => $self->name); 

    my $i = 1;
    for my $constraint ( $self->editable_constraints ) {
        next unless $constraint->switched_on;
        my %hash = $constraint->query_hash;
        while ( my ( $k, $v ) = each %hash ) {
            $request_parameters{ $k . $i } = $v;
        }
        $i++;
    }

    return %request_parameters;
}

1;
