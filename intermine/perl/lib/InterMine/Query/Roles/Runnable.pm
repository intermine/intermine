package InterMine::Query::Roles::Runnable;

use Moose::Role;
requires qw(view service url);

use InterMine::Service;

around BUILDARGS => sub {
    my $orig = shift;
    my $class = shift;
    if ( @_ == 1 && ! ref $_[0] ) {
	my $service = InterMine::Service->new(root => $_[0]);
	my $model   = $service->model;
	return $class->$orig(
			     service => $service,
			     model   => $model,
			    );
    }
    else {
	return $class->$orig(@_);
    }
};


sub results_iterator {
    my $self  = shift;
    my %args  = @_;
    my $roles = $args{with} if (exists $args{with});
    return $self->service->get_results_iterator(
	$self->url,
	$self->view,
	$roles,
	);
}


# my $results = $query->results(as => 'hashref');
sub results {
    my $self = shift;
    my %args = @_;
    my $wanted = $args{as} || 'arrayref'; # string and hashref are possible
    $wanted    =~ s/s$//;         # trim trailing 's' on arrayrefs/hashrefs
    my $i = $self->results_iterator;
    my @lines = $i->all_lines($wanted);
    if ($wanted eq 'string' and $args{as} eq 'string') { #ie. the original wanted wasn't 'strings'
	return join("\n", @lines);
    } else {
	return \@lines;
    }
}

sub save { # saves queries as saved_queries, and templates as updated templates
    confess "NOT IMPLEMENTED YET";
    my $self = shift;
    my %args = @_;
    $self->name($args{name}) if (exists $args{name});
    my $xml  = $self->to_xml;
    my $url  = $self->upload_url;
    my $resp = $self->service->send_off($xml => $url);
    confess "Failed to save data to webservice"
	unless $resp->is_success;
    return;
}

sub save_as_template { # saves queries as templates,
                       # with all template constraint attributes
                       # set to defaults values - can be used to save
                       # a modified template under a new name
    confess 'NOT IMPLEMENTED YET'; # TODO
}

1;
