package InterMine::Query::Roles::WriteOutLegacy;

use Moose::Role;
requires (qw/name view sort_order logic joins
             path_descriptions model_name type_dict
             constraints coded_constraints/);
use XML::Smart;
use List::MoreUtils qw(uniq);
use InterMine::Path qw(type_of);

sub to_xml {
    my $self = shift;

    my %joins;
    for my $join ($self->joins) {
	my $joined_path = $join->path;
	if ($join->style eq 'OUTER') {
	    $joined_path =~ s/(.*)\./:/;
	}
	$joins{$join->path} = $joined_path;
    }

    sub put_joins_in {
	my $string = shift;
	while (my ($p, $j) = each %joins) {
	    $string =~ s/$p/$j/g;
	}
	return $string;
    }

    my $XML   = XML::Smart->new;

    my %query = (
	name            => $self->name,
	view            => put_joins_in($self->joined_view(' ')),
	sortOrder       => put_joins_in($self->sort_order),
	model		=> $self->model_name,
    );

    $query{longDescription} = $self->description if $self->description;
    $query{constraintLogic} = $self->logic->code
	if ($self->coded_constraints > 1);

    push @{$XML->{query}}, \%query;
    for my $pd   ($self->path_descriptions) {
	my %hash = $pd->to_hash;
	$hash{path} = put_joins_in($hash{path});
	push @{$XML->{query}{pathDescription}}, \%hash;
    }

    my @paths     = sort uniq(map {$_->path} $self->all_constraints);
    my $type_dict = $self->type_dict;
    for (@paths) {
	my $type = $type_dict->{$_} || type_of($self->model, $_);
	my %elem = {
	    path => put_joins_in($_),
	    type => $type,
	};
	my @cons_for_this_node =
	    sort {$a->code cmp $b->code}
		grep {$_->path eq $_} $self->coded_constraints;
	$elem->{constraint} = [] if @cons_for_this_node;
	for my $c (@cons_for_this_node) {
	    my %hash = $c->to_hash;
	    delete $hash{path};
	    push @{$elem->{constraint}}, \%hash;
	}
	push @{$XML->{query}{node}}, $elem;
    }

    my $xml =$XML->{query}('<xml>');
    return $xml;
}

1;
