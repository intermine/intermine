#!/usr/bin/perl

use strict;
use warnings;
use Carp;
use Data::Dumper;
use DateTime;
use Storable;

use constant APP => 'InterMine-TemplatesAnalytics-0.1';

use XML::Rules;
use LWP::UserAgent;

my $months_back = 6;

my $now  = DateTime->now;
my $doy  = $now->day_of_year;
my $month = $now->month;

my $start = DateTime->now->subtract(months => $months_back);

my @times = ($now);
my $weeks;
while ($times[-1]->epoch > $start->epoch) {
    push(@times, DateTime->now->subtract(weeks => ++$weeks));
}

my $parser = XML::Rules->new(rules => [_default => 'as array']);

my $email = 'flymine.org@googlemail.com';
my $psswd = 'integrate';

my $authurl = 'https://www.google.com/accounts/ClientLogin';
my $authcontent_type = 'application/x-www-form-urlencoded';
my $authcontent = [
    accountType => 'GOOGLE',
    Email       => $email,
    Passwd      => $psswd,
    service     => 'analytics',
    source      => APP,
    ];


sub auth {
    my $ua = LWP::UserAgent->new();
    $ua->agent(APP);

    my $auth_res = $ua->post($authurl, Content_Type => $authcontent_type, Content => $authcontent);

    my $auth;
    if ($auth_res->is_success) {
	($auth) = $auth_res->content() =~ /Auth=([\w-]+)/;
#	print "Authentication successful.\n";
	return $auth;
    }
    else {
	my ($err) = $auth_res->content() =~ /Error=(\w+)/;
	croak "Authentication failed - $err";
    }
}

sub get {
    my $ua = LWP::UserAgent->new();
    $ua->agent(APP);

    my $url    = URI->new(shift);
    my %params = @_;
    $url->query_form(%params);
    my $auth = auth();
    my $res = $ua->get($url, 'GData-Version' => '2', Authorization => "GoogleLogin auth=$auth");#, %params);
    croak $res->status_line, $url if $res->is_error;
    return $parser->parse($res->content);
}

my $url = 'https://www.google.com/analytics/feeds/accounts/default';

my $feeds = get($url);
print Dumper($feeds);
exit;
my $table_id;
for my $entry (@{$feeds->{feed}[0]{entry}}) {
    if ($entry->{title}[0]{_content} eq 'www.flymine.org') {
	$table_id = $entry->{'dxp:tableId'}[0]{_content}
    }
}

$url = 'https://www.google.com/analytics/feeds/data';

my $matrix = {};
while (@times > 1) {
    my $start = pop @times;
    my %parameters = (
		      ids          => $table_id,
		      dimensions   => 'ga:pagePath',
		      metrics      => 'ga:uniquePageviews',
		      'start-date' => $start->ymd,
		      'end-date'   => $times[-1]->ymd,
		      filters      => 'ga:pagePath=@template.do?name',
		     );

    my $data = get($url, %parameters);

    my %views_by;
    for my $entry (@{$data->{feed}[0]{entry}}) {
	my $p = $entry->{'dxp:dimension'}[0]{value};
	if ($p =~ /template\.do/) {
	    $p =~ s/.*template\.do\?name=([^&]+).*/$1/;
	    my $v = $entry->{'dxp:metric'}[0]{value};
	    $views_by{$p} = $v;
	}
    }
    my $key = $start->ymd . ' to ' . $times[-1]->ymd;
    $matrix->{$key} = \%views_by;
    print "Got analytics for the period: $key\n";
    # for (sort {$views_by{$b} <=> $views_by{$a}} keys %views_by) {
    # 	print "$_ had $views_by{$_} views\n";
    # }
}
store($matrix, $ENV{HOME}.'/Projects/analytics_data');
#print Dumper($matrix);
exit;
