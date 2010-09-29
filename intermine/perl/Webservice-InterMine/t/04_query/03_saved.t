use lib 't/tests';

use Test::Webservice::InterMine::Query::Saved;
eval (require Perl::Tidy);
if ($@) {
    Test::Webservice::InterMine::Query::Saved->SKIP_CLASS("We need Perl::Tidy to run these tests");
}
Test::Class->runtests;
