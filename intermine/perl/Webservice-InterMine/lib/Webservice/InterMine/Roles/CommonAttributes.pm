package Webservice::InterMine::Roles::CommonAttributes;

use Moose::Role;
use Webservice::InterMine::TypeLibrary qw(Model);
use MooseX::Types::Moose qw(Str);

has [qw/name description/] => (
    is	    => 'rw',
    isa	    => Str,
    default => '',
);

has model => (
    is => 'ro',
    isa => Model,
    required => 1,
    coerce => 1,
    lazy => 1,
# Invalid default, but consumers that define their own defaults will get better
# results, otherwise it will need to be in the constructor
    default => sub {},
    handles => {
	model_name => 'model_name',
    },
);

1;
