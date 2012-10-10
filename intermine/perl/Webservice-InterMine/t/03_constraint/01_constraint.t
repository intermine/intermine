use lib 't/tests';
use Test::Webservice::InterMine::Constraint;
use Test::Webservice::InterMine::Constraint::Unary;
use Test::Webservice::InterMine::Constraint::Binary;
use Test::Webservice::InterMine::Constraint::Multi;
use Test::Webservice::InterMine::Constraint::Range;
use Test::Webservice::InterMine::Constraint::Ternary;
use Test::Webservice::InterMine::Constraint::SubClass;

Test::Class->runtests;
