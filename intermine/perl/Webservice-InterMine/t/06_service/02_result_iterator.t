use strict;
use warnings;
use Test::More tests => 4;
use Test::Exception;
use IO::Handle;

use Webservice::InterMine::ResultIterator;

require Webservice::InterMine::Parser::JSON::ArrayRefs;
require Webservice::InterMine::Parser::JSON::HashRefs;

my $content = '{"views":["Foo", "Bar", "Baz"],"results":[
[{"value":"String-A"}, {"value": 123}, {"value": 1.23}],
[{"value":"String-B"}, {"value": 456}, {"value": 1.24}]
], "wasSuccessful":true, "error": null}';

my $error_content = '{"views":["Foo", "Bar", "Baz"],"results":[
[{"value":"String-A"}, {"value": 123}, {"value": 1.23}],
[{"value":"String-B"}, {"value": 456}, {"value": 1.24}]
], "wasSuccessful":false, "error": "Some error", "statusCode": 600}';

my $incomplete_content = '{"views":["Foo", "Bar", "Baz"],"results":[
[{"value":"String-A"}, {"value": 123}, {"value": 1.23}],';

my $tsv_content = "String-A\t123\t1.23\nString-B\t456\t1.24\n";

TEST_ARRAYS: {
    open (my $content_handle, '<', \$content);

    my $iterator = Webservice::InterMine::ResultIterator->new(
        url => 'http://foo.com',
        parameters => {bar => 'quux'},
        authorization => 'zip',
        request_format => "jsonrows",
        row_parser =>  Webservice::InterMine::Parser::JSON::ArrayRefs->new(),
        content => $content_handle,
        error_code => 200,
    );

    is_deeply(
        scalar($iterator->get_all),
        [
            ["String-A", 123, 1.23],
            ["String-B", 456, 1.24]
        ],
        "Can parse json results to arrays"
    );
}

TEST_HASHES: {
    open (my $content_handle, '<', \$content);
    my $iterator = Webservice::InterMine::ResultIterator->new(
        url => 'http://foo.com',
        parameters => {bar => 'quux'},
        authorization => 'zip',
        request_format => "jsonrows",
        row_parser =>  Webservice::InterMine::Parser::JSON::HashRefs->new(view => [qw/Foo Bar Baz/]),
        content => $content_handle,
        error_code => 200,
    );

    is_deeply(
        scalar($iterator->get_all),
        [
            {Foo => "String-A", Bar => 123, Baz => 1.23},
            {Foo => "String-B", Bar => 456, Baz => 1.24}
        ],
        "Can parse json results to hashes"
    );
}

TEST_ERRORS: {
    open (my $content_handle, '<', \$error_content);
    my $iterator = Webservice::InterMine::ResultIterator->new(
        url => 'http://foo.com',
        parameters => {bar => 'quux'},
        authorization => 'zip',
        request_format => "jsonrows",
        row_parser =>  Webservice::InterMine::Parser::JSON::ArrayRefs->new(),
        content => $content_handle,
    );

    throws_ok {$iterator->get_all} qr/Some error/, "Reports errors from footers";
}

TEST_INCOMPLETENESS: {
    open (my $content_handle, '<', \$incomplete_content);
    my $iterator = Webservice::InterMine::ResultIterator->new(
        url => 'http://foo.com',
        parameters => {bar => 'quux'},
        authorization => 'zip',
        request_format => "jsonrows",
        row_parser =>  Webservice::InterMine::Parser::JSON::ArrayRefs->new(),
        content => $content_handle,
        error_code => 200,
    );

    throws_ok {$iterator->get_all} qr/Incomplete/, "Reports incomplete result sets";
}


