use strict;
use warnings;

use Test::More tests => 3;
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
is($query->resource_path, "/query/results", "Has a resource path");

