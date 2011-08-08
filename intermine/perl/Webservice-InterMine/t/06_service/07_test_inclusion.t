use warnings;
use strict;

use Test::More tests => 12;

use Webservice::InterMine;

ok __PACKAGE__->can("new_query");
ok __PACKAGE__->can("new_list");
ok __PACKAGE__->can("get_template");
ok __PACKAGE__->can("get_list");
ok __PACKAGE__->can("get_service");
ok __PACKAGE__->can("load_query");

ok(Webservice::InterMine->can("new_query"));
ok(Webservice::InterMine->can("new_list"));
ok(Webservice::InterMine->can("get_template"));
ok(Webservice::InterMine->can("get_list"));
ok(Webservice::InterMine->can("get_service"));
ok(Webservice::InterMine->can("load_query"));
