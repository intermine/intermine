package Webservice::InterMine::Query::Roles::Listable;

use MooseX::Role::WithOverloading;

requires 'list_upload_path', 'service_root', 'to_query', 
    'get_request_parameters', 'service', 'list_append_path';

use overload 
   '|' => \&overload_adding,
   '+' => \&overload_adding,
   '&' => \&overload_intersecting,
   '^' => \&overload_diffing,
   '-' => \&overload_subtraction,
   fallback => 1;

sub get_list_upload_uri {
    my $self = shift;
    return $self->service_root . $self->list_upload_path;
}

sub get_list_request_parameters {
    my $self = shift;
    my $clone = $self->to_query->clone;
    my @views = $clone->views;
    if (@views == 0 && $clone->has_root_path) {
        $clone->select("id");
    } elsif (@views > 1 || $views[0] !~ /\.id$/) {
        my %froms = map {$clone->path($_)->prefix() => 1} @views;
        my @froms = keys %froms;
        if (@froms > 1) {
            confess "Cannot generate a valid list request - more than one class is selected";
        } else {
            $clone->select($froms[0] . ".id");
        }
    } 
        
    my %params = $clone->get_request_parameters;
    return %params;
}

sub get_list_append_uri {
    my $self = shift;
    return $self->service_root . $self->list_append_path;
}

sub overload_adding {
    my ($self, $other, $reversed) = @_;
    return $self->service->join_lists([$self, $other]);
}

sub overload_intersecting {
    my ($self, $other, $reversed) = @_;
    return $self->service->intersect_lists([$self, $other]);
}

sub overload_diffing {
    my ($self, $other, $reversed) = @_;
    return $self->service->diff_lists([$self, $other]);
}

sub overload_subtraction {
    my ($self, $other, $reversed) = @_;
    if ($reversed) {
        return $self->service->subtract_lists([$other], [$self]);
    } else {
        return $self->service->subtract_lists([$self], [$other]);
    }
}

1;
