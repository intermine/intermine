package InterMine::Query::Roles::ExtendedQuery;

use MooseX::Role::Parameterized;
use InterMine::TypeLibrary qw(QueryType);
use MooseX::Types::Moose qw(HashRef);
use XML::Smart;
with 'InterMine::Query::Roles::WriteOutAble';
parameter type => (
    isa	     => QueryType,
    required => 1,
    );

role {
    my $param = shift;
    my %args = @_;

    around to_xml => sub {
	my $orig = shift;
	my $self = shift;
	my $wanted = shift;
	my $coreXML = XML::Smart->new($self->$orig);
	my $extXML  = XML::Smart->new;
	my $type    = $param->type;
	my $core_root = $coreXML->root;
	my $insertions = $self->insertion;

	return $coreXML->{$core_root}('<xml>')
	    if ($wanted and $wanted eq 'query');

	push @{$extXML->{$type}}, $self->head;
	push @{$extXML->{$type}{$core_root}}, $coreXML->{$core_root};

	while (my ($key, $value) = each %$insertions) {
	    push @{$extXML->{$type}{$core_root}{$key}}, @$value;
	}

	my $xml = $extXML->{$param->type}('<xml>');
	return $xml;
    };
};

1;
