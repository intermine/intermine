package Webservice::InterMine::Role::ModelOwner;

use Moose::Role;
use InterMine::Model::Types qw(Model);

has model => (
    is         => 'ro',
    isa        => Model,
    required   => 1,
    coerce     => 1,
    lazy_build => 1,
    handles    => { model_name => 'model_name', },
);

1;
