package Webservice::InterMine::Constraint::Role::Template;

use Moose::Role;
requires 'get_xml_tags';

has editable => (
    is       => 'ro',
    isa      => 'Bool',
    required => 1,
);

has switchable => (
    is       => 'ro',
    isa      => 'Bool',
    default  => 0,
);

has switched_on => (
    is       => 'ro',
    isa      => 'Bool',
    default  => 1,
);

around 'get_xml_tags' => sub {
    my $orig = shift;
    my $self = shift;
    my %tags;
    $tags{editable} = ($self->editable) ? 'true' : 'false' ;
    if ($self->switchable) {
	$tags{switchable} = ($self->switched_on) ? 'on' : 'off' ;
    } else {
	$tags{switchable} = 'locked';
    }
    return ($self->$orig(@_), %tags);
};
1;
