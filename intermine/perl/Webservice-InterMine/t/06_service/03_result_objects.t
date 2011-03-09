use strict;
use warnings;
use Test::More tests => 7;

use Webservice::InterMine::ResultObject;

my $hashref = {
    foo => "bar",
    bop => "bip",
    quuxes => [
        {zip => 1, zop => 2},
        {zip => 3, zop => 4}
    ]
};
bless $hashref, "Webservice::InterMine::ResultObject";

is($hashref->foo, "bar");
is($hashref->bop, "bip");

for my $quux ($hashref->quuxes) {
    bless $quux, "Webservice::InterMine::ResultObject";
}
# Accesses arrays as arrayrefs in scalar context
is($hashref->quuxes->[0]->zip, 1);
is($hashref->quuxes->[0]->zop, 2);
is($hashref->quuxes->[1]->zip, 3);
is($hashref->quuxes->[1]->zop, 4);
# Accesses arrays as lists in list context
my $sum;
map {$sum += $_->zip + $_->zop} $hashref->quuxes;
is($sum, 10);
