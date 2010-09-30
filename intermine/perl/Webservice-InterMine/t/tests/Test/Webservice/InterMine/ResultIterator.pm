package Test::Webservice::InterMine::ResultIterator;


use strict;
use warnings;

use base 'Test::Class';
use Encode;
use IO::File;
use Test::More;
use Test::Exception;
use Test::MockObject::Extends;
use InterMine::Model;

sub class {'Webservice::InterMine::ResultIterator'}

# need good results, bad ones, long ones, and interrupted ones
sub mock_results {'t/data/mock_results'}
sub bad_results  {'t/data/mock_bad_results'}
sub long_results {'t/data/mock_long_results'}
sub interrupted_results {'t/data/mock_interrupted_results'}
sub missing_chunk {'t/data/mock_missing_chunk_results'}

sub line1 {'S000000001	YAL001C	10531351	Rubbi L	J Biol Chem	1999	Saccharomyces cerevisiae'}
sub line2 {'S000000001	YAL001C	10384303	Chédin S	Cold Spring Harb Symp Quant Biol	1998	Saccharomyces cerevisiae'}
sub line3 {'S000000004	YAL005C	16267268	Roberts TM	Mol Biol Cell	2006	Saccharomyces cerevisiae'}
sub last_line {'S000000007	YAL009W	4613605	Esposito MS	Genetics	1974	Saccharomyces cerevisiae'}

#BEGONE OF { use Carp qw(confess); $SIG{'__WARN__'} = sub {confess $_[0]}}

sub args {
    my $test = shift;
    return (
    connection => $test->fake_connection($test->mock_results),
    view_list  => $test->view_list,
)}
sub view_list {[
    qw/Col.one Col.two Col.three Col.four Col.five Col.six Col.seven/
]}
sub fake_connection {
    my $test    = shift;
    my $results = shift;
    my $fake_connection = Test::MockObject->new;
    $fake_connection->{io} = IO::File->new($results, 'r');
    $fake_connection->set_isa('Net::HTTP');
    $fake_connection->mock(
	getline => sub {
	    my $self = shift;
	    return $self->{io}->getline;
	},
    );
    $fake_connection->mock(
	getlines => sub {
	    my $self = shift;
	    return $self->{io}->getlines;
	},
    );
    $fake_connection->mock(
	close => sub {
	    my $self = shift;
	    return $self->{io}->close;
	},
    );
    return $fake_connection;
}


sub startup : Test(startup => 1) {
    my $test = shift;
    use_ok($test->class);
}

sub setup : Test(setup) {
    my $test = shift;
    my $iter = $test->class->new($test->args);
    $test->{object} = $iter;
}

sub teardown : Test(teardown) {
    my $test = shift;
    delete $test->{object};
}

sub methods : Test {
    my $test = shift;
    my @methods = (qw/
	connection error_code error_message csv
	view_list headers is_chunked chunk_bytes_left
	subtract_from_current_chunk
	set_headers is_success is_error status_line
	string arrayref hashref all_lines
    /);
    can_ok($test->class, @methods);
}

sub construction : Test(12) {
    my $test = shift;
    my $iter = new_ok($test->class, [$test->args]);
    is($iter->error_code, 200, "... and sets error code from header");
    is($iter->error_message, "OK", "... and sets error message");
    is($iter->is_chunked, 1, "... and correctly parses chunking header");
    is($iter->status_line, "OK (200): OK", "... and sets status line");
    ok($iter->is_success, "... and reports success accurately");
    ok(! $iter->is_error, "... and reports error accurately");
    dies_ok(
	sub {$test->class->new()},
	"... and dies building without args",
    );
    dies_ok(
	sub {$test->class->new(
	    content => $test->fake_connection($test->mock_results),
	    );
	 },
	"... and dies building without a view list",
    );
    dies_ok(
	sub {$test->class->new(
	    view_list  => $test->view_list,
	    );
	 },
	"... and dies building without a connection",
    );
    dies_ok(
	sub {$test->class->new(
	    content => 'foo',
	    view_list  => $test->view_list,
	    );
	 },
	"... and dies building with a bad connection",
    );
    dies_ok(
	sub {$test->class->new(
	    content => $test->fake_connection($test->mock_results),
	    view_list  => 'foo',
	    );
	 },
	"... and dies building with a bad view list",
    );
}

sub interupted_connection : Test(2) {
    my $test = shift;
    my $iter = new_ok(
	$test->class,
	[connection => $test->fake_connection($test->interrupted_results),
	 view_list  => $test->view_list]
    );
    throws_ok(
	sub {while (defined $iter->string) {}},
	qr/Transfer interrupted/,
	"... and catches interrupted transfers",
    );
}

sub missing_chunk_connection : Test(2) {
    my $test = shift;
    my $iter = new_ok(
	$test->class,
	[connection => $test->fake_connection($test->missing_chunk),
	 view_list  => $test->view_list]
    );
    throws_ok(
	sub {while (defined $iter->string) {}},
	qr/Unexpected end.*Transfer interrupted/,
	"... and catches missing chunks",
    );
}


sub bad_connection : Test(4) {
    my $test = shift;
    my $iter = new_ok(
	$test->class,
	[connection => $test->fake_connection($test->bad_results),
	 view_list  => $test->view_list]
    );
    ok($iter->is_error, "Detects bad connection ok");
    ok(! $iter->is_success, "... and does not report false positives");
    is($iter->string, undef, "... attempts to get lines return undef");
}

sub long_connection : Test(3) {
    use Data::Dumper;
    my $test = shift;
    my $iter = new_ok(
	$test->class,
	[connection => $test->fake_connection($test->long_results),
	 view_list  => $test->view_list]
    );
    my ($rows, $cols, $expected) = (0, 0, 6_000);
    my $b;
    while (defined ($a =$iter->arrayref)) {
	$cols += scalar(@$a);
	warn Dumper($a) if (scalar(@$a) != 7);
	$rows++;
	$b = $a;
    };
    is($rows, $expected, "... Gets all rows, even for long results");
    is($cols, $expected * 7, "... and all rows have the right number of columns");
}

sub resultline1_as_string : Test(4) {
    my $test = shift;
    my $obj = $test->{object};

    is($obj->string, $test->line1, "... correctly reads the first body line as a string");
    is($obj->string, $test->line2, "... and it is ok with wide characters");
    my $i = 0;
    my $line;
    until ($i == 100) {
	$line = $obj->string;
	$i++;
    }
    is($line, $test->line3, "... and it correctly puts run-on lines together");
    while (defined (my $l = $obj->string)) {
	$line = $l;
    }
    is(
	$line, $test->last_line,
	"... and it reads all the way up to the end, and no further"
    );
}

sub resultline3_as_hashref : Test(5) {
    my $test = shift;
    my $obj = $test->{object};
    my $href1 = $obj->hashref;
    isa_ok($href1, 'HASH', "what hashref returns");
    my $href2 = $obj->hashref;
    isnt($href1, $href2, "... and it doesn't return the same one");
    is_deeply([sort keys %$href1], [sort @{$test->view_list}], "... and it has the right keys");
    is($href1->{'Col.four'}, "Rubbi L", "... and it associates the fields with the right key");
    is($href2->{'Col.four'}, "Chédin S", "... and it handles wide characters");
}

sub resultline2_as_arrayref : Test(6) {
    my $test = shift;
    my $obj = $test->{object};
    my $aref1 = $obj->arrayref;
    isa_ok($aref1, 'ARRAY', "what arrayref returns");
    my $aref2 = $obj->arrayref;
    isnt($aref1, $aref2, "... and it doesn't return the same one");
    is(scalar(@$aref1), 7, "... and it has the right number of columns");
    is($aref1->[2], 10531351, "... and it is ok with ints");
    is($aref1->[3], "Rubbi L", "... and it is ok with strings");
    is($aref2->[3], "Chédin S", "... and it is ok with wide characters");
}

sub results_all_lines : Test(4) {
    my $test = shift;
    my $obj = $test->{object};
    throws_ok(
	sub {$obj->all_lines},
	qr/invalid row format/,
	"catches lack of row format",
    );
    my @rows_as_strings = $obj->all_lines('string');
    is(
	scalar(@rows_as_strings), 202,
	"... and returns the right number of rows",
    );
    is(
	$rows_as_strings[0], $test->line1,
	"... and the first line is ok",
    );
    is(
	$rows_as_strings[-1], $test->last_line,
	"... and so is the last line",
    );
}

sub content : Test(2) {
    my $test = shift;
    my $contentf = $test->mock_results;
    open(my $content, '<', $contentf);
    my $ri = new_ok(
	$test->class,
	[content => $content,
	 view_list  => $test->view_list]
    );
    my @linesRI = $ri->all_lines('string');
    my @linesFC = $test->fake_connection($test->mock_results)->getlines;

    # These should be the only transformations done to the raw content
    map {s/\015?\012//} @linesFC;
    @linesFC = map {encode_utf8($_)} @linesFC;

    is_deeply(
	\@linesRI, \@linesFC, "Can handle results in content too"
    );
}


1;
