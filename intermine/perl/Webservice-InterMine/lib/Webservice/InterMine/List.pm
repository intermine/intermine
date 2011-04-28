package Webservice::InterMine::List;

use Moose;
use InterMine::Model::Types qw(PathString);
use Webservice::InterMine::Types qw(Date);

use overload
  '""'     => \&to_string,
  fallback => 1;

has [qw/title name/] => (
    is  => 'rw',
    isa => 'Str',
);

has 'size' => (
    is  => 'ro',
    isa => 'Int',
);

has 'type' => (
    is  => 'ro',
    isa => PathString,
);

has 'date' => (
    init_arg  => 'dateCreated',
    isa       => Date,
    is        => 'ro',
    coerce    => 1,
    predicate => 'has_date',
);

sub to_string {
    my $self = shift;
    my $ret  = sprintf( "%s (%s %ss)%s",
        $self->name, $self->size, $self->type,
        ( ( $self->has_date ) ? " " . $self->date->datetime : "" ) );
    return $ret;
}

__PACKAGE__->meta->make_immutable;
no Moose;

1;
