package InterMine::DB;

use strict;

use base qw(Rose::DB);

__PACKAGE__->use_private_registry;

__PACKAGE__->register_db(
  driver   => 'pg',
  database => 'production-flyatlas',
  host     => 'shadowfax',
  username => 'kmr',
  password => 'kmr',
);
