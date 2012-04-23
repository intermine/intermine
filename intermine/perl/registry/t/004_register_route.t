use Test::More tests => 12;
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

response_status_is([POST => "/register"], 400, "Names are required");

my $response = dancer_response("post" => "/register", 
    {params => {name => "test3", authToken => "mysecret", "url" => "http://anotherplace.org"}});

is($response->{status}, 400, "Registering bad mines is an error")
    or diag $response->{content};

my $good_response = dancer_response("post" => "/register", {params => {
            name => "Test3", 
            authToken => "mysecret", 
            url => "http://squirrel.flymine.org/intermine-test"
        }});

is($good_response->{status}, 201, "Registering a good mine is ok")
    or diag $good_response->{content};

my $mines_response = dancer_response("GET" => "/mines.json");
is($mines_response->{status}, 200, "Can get mines after registration");
my $mines = decode_json($mines_response->{content});

is_deeply(
    [sort map {$_->{name}} @{ $mines->{mines} }],
    [qw/Test1 Test2 Test3/],
    "Response has the right mine names"
);

is($mines->{mines}[2]{homeUrl}, "http://squirrel.flymine.org/intermine-test", "Has the right details");

my $tokenless_response = dancer_response("POST" => "/register", {params => {
            name => "Test4", 
            url => "http://squirrel.flymine.org/intermine-test"
        }});

is($tokenless_response->{status}, 201, "Can create without a token")
    or diag $tokenless_response->{content};

ok(decode_json($tokenless_response->{content})->{authToken}, "The request returns a token if we didn't supply one");

my $returned_token = decode_json($tokenless_response->{content})->{authToken};

my $forbidden_response =  dancer_response("POST" => "/register", {params => {
            name => "Test4", 
            url => "http://squirrel.flymine.org/intermine-test"
        }});

is($forbidden_response->{status}, 403, "We are forbidden from trying to overwrite though")
    or diag $forbidden_response->{content};

$mines_response = dancer_response("GET" => "/mines.json");
is($mines_response->{status}, 200, "Can get mines after registration");
$mines = decode_json($mines_response->{content});

is_deeply(
    [sort map {$_->{name}} @{ $mines->{mines} }],
    [qw/Test1 Test2 Test3 Test4/],
    "Response has the right mine names"
);

my $update_resp = dancer_response("POST" => "/register", {params => {
            name => "Test4", 
            authToken => $returned_token,
            url => "http://squirrel.flymine.org/intermine-test"
        }});

note $returned_token;
is($update_resp->{status}, 200, "Can update with the returned token")
    or diag $update_resp->{content};


END {
    # Tear down fixtures
    my $count = unlink <t/fixture/*.yml>;
    die "Only deleted $count - expected to delete 5" unless ($count == 5);
    $count = unlink <t/fixture/*.bak>;
}

