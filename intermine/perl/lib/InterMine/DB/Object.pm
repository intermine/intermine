package InterMine::DB::Object;

use strict;

use InterMine::DB;
use base qw(Rose::DB::Object);

sub init_db { InterMine::DB->new }

1;
