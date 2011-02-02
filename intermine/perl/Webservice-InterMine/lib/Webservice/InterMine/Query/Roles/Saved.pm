package Webservice::InterMine::Query::Roles::Saved;

use Moose::Role;

requires(qw/name/);

has 'date' => (
    is      => 'rw',
    isa     => 'Str',
    default => '',
);

sub type { 'saved-query' }

sub head {
    my $self = shift;
    my $head = {name => $self->name};
    $head->{'date-created'} = $self->date  if ($self->date);

    return $head;
}

sub insertion { }

1;
