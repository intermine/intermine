#!/usr/bin/perl

use strict;
use warnings;
use Benchmark qw/:all/;
$|++; # autoflush

use Inline Python => <<'EOP';
import sys
def run_python():
    from intermine.webservice import Service
    s = Service("www.flymine.org/query")
    q = s.query("Gene").where(s.model.Gene.length < 300).order_by(s.model.Gene.length)
    c = 0
    t = 0
    for r in q.rows():
        c += 1
        t += r["length"]
        if c % 500 == 0: sys.stdout.write(".") 
    if c != q.count():
        print "Got mismatching counts!"
    print "Av length: %d" % (t/c)
EOP

use Inline Java => <<'EOJ';

import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.OrderDirection;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.QueryService;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import java.util.Map;
import java.util.Iterator;

class TestRunner {

    private final ServiceFactory serviceFactory 
        = new ServiceFactory("http://www.flymine.org/query/service", "MyApp");
    private final QueryService queryService;
    private final Model model;
    private final PathQuery pq;

    public TestRunner() {
        queryService = serviceFactory.getQueryService();
        model = serviceFactory.getModelService().getModel();
        pq = new PathQuery(model);
        pq.addViews("Gene.symbol", "Gene.primaryIdentifier", 
            "Gene.cytoLocation", "Gene.length", 
            "Gene.secondaryIdentifier", "Gene.summary");
        pq.addConstraint(
            Constraints.lessThan("Gene.length", "300"));
        pq.addOrderBy("Gene.length", OrderDirection.ASC);
    }


    public void run_java() {
        Iterator<Map<String, Object>> it 
            = queryService.getRowMapIterator(pq);

        int c = 0;
        int t = 0;
        try {
            while (it.hasNext()) {
                Map<String, Object> row = it.next();
                c++;
                t += (Integer) row.get("length");
                if (c % 500 == 0) {
                    System.out.print(".");
                }
            }
            int count = queryService.getCount(pq);
            if (count != c) {
                System.out.print("Got mismatching counts!");
            }
            System.out.println("Av length " + (t/c));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

}
EOJ

### Perl benchmarked code
require Webservice::InterMine;
my $s = Webservice::InterMine->get_service("www.flymine.org/query");

sub run_perl {
    my $query = $s->new_query(class => 'Gene')
                  ->where(length => {lt => 300})
                  ->order_by("length");
    my $c = my $t = 0;
    for (my $it = $query->results_iterator(as => 'rr'); $_ = <$it>;) {
        $c++;
        $t += $_->{length};
        print "." if ($c % 500 == 0);
    }
    print "Got mismatching counts!\n" unless ($c == $query->count);
    printf "Av length: %d\n", ($t/$c);
}
# EOP

my $ruby = <<'EOR';
require 'rubygems'
require 'intermine/service'

s = Service.new("www.flymine.org/query")
q = s.query("Gene").where(:length => {:lt => 300}).order_by(:length)
c = t = 0
q.each_row { |r|
    c += 1
    t += r["length"]
    print "." if (c % 500 == 0)
}
print "Got mismatching counts!" unless (c == q.count)
puts "Av length: #{t/c}"
EOR

my ($runs) = @ARGV;
$runs ||= 5;

$runs =~ s/_//g;

my $tr = new TestRunner();
my $routines = {
    "Java"   => sub {$|++; $tr->run_java()},
    "Perl"   => sub {$|++; run_perl()},
    "Python" => sub {$|++; run_python()},
    "Ruby"   => sub {$|++; open (my $pipe, '|-', 'ruby'); print $pipe $ruby; close $pipe;},
};
my %times;
for my $name (sort keys %$routines) {
    print "Running $name x $runs:\n";
    my $start = time;
    for (1 .. $runs) {
        $routines->{$name}();
    }
    $times{$name} = (time - $start);
}

my ($shortest) = sort values %times;


for my $name (sort keys %$routines) {
    print "$name took $times{$name} seconds\n";
}

open (my $fh, '>>', "benchmark.results") or die "Could not open result file";
my $line = join(",", map {$times{$_}} sort keys %times);
print $fh "$line\n";
close($fh);

