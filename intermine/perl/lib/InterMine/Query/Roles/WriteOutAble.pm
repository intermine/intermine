package InterMine::Query::Roles::WriteOutAble;

use Moose::Role;
requires (qw/name view sort_order logic joins
             path_descriptions model_name
             constraints coded_constraints/);
use XML::Smart;

sub to_xml {
    my $self = shift;
    my $XML = XML::Smart->new;
    my %query = (
	name            => $self->name,
	view            => $self->joined_view(' '),
	sortOrder       => $self->sort_order,
	model		=> $self->model_name,
	);
    $query{longDescription} = $self->description if $self->description;
    $query{constraintLogic} = $self->logic->code
	if ($self->coded_constraints > 1);

    push @{$XML->{query}}, \%query;
    for my $pd   ($self->path_descriptions) {
	push @{$XML->{query}{pathDescription}}, {$pd->to_hash};
    }
    for my $join ($self->joins) {
	push @{$XML->{query}{join}}, {$join->to_hash};
    }
    for my $con ($self->all_constraints) {
	push @{$XML->{query}{constraint}}, {$con->to_hash};
    }
    my $xml =$XML->{query}('<xml>');
    return $xml;
}

1;
