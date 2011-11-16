use Test::More tests => 5;
use strict;
use warnings;

use File::Copy;
use JSON;

# the order is important
use Registry;
use Dancer::Test;

BEGIN {
    require Dancer;
    # Set-up fixtures.
    for my $file (qw/mines_file secrets_file mine_logs administrators error_log/) {
        copy("t/data/$file.yml" => "t/fixture/$file.yml") or die "Copy of $file failed: $!";
        Dancer::set($file => "t/fixture/$file.yml");
    }
}

route_exists [GET => '/mines'], "A route exists for mines";
response_status_is ['GET' => '/mines'], 200, 'response status is 200 for /mines';
response_status_is ['GET' => '/mines.json'], 200, 'response status is 200 for /mines.json';

my $response = dancer_response GET => '/mines.json';
my $json = decode_json($response->{content});

is_deeply(
    [sort map {$_->{name}} @{ $json->{mines} }],
    [qw/Test1 Test2/],
    "Response has the right mine names"
);

is($json->{mines}[0]{webServiceRoot}, "http://somewhere.org/service", "Can retrieve the ws root");

END {
    # Tear down fixtures
    my $count = unlink <t/fixture/*.yml>;
    die "Only deleted $count - expected to delete 5" unless ($count == 5);
}


