use lib 't/tests';

use Test::Webservice::InterMine::Query::Scripted;

eval "use Perl::Tidy";
Test::Webservice::InterMine::Query::Scripted->SKIP_CLASS("Perl::Tidy required for testing Scripted Queries")
    if $@;

Test::Class->runtests;
