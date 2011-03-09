use strict;
use warnings;

use Test::More tests => 4;
use Webservice::InterMine::Service;
use InterMine::Model::TestModel;

my $service = Webservice::InterMine::Service->new(
    root => "www.foo.com/path",
    model => InterMine::Model::TestModel->instance,
    version => 1,
);

my $query = $service->new_query;

ok($query->service == $service, "Has a service");
is($query->service_root, "http://www.foo.com/path/service", "Has a service root");
is($query->query_path, "/query/results", "Has a query path");
is($query->templatequery_path, "/template/results", "Has a template query path");

