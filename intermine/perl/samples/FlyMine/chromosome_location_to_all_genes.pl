
use LWP::Simple;

my $serviceurl = 'http://www.flymine.org/release-24.0/service/template/results?name=ChromLocation_Gene&constraint1=Chromosome.primaryIdentifier&op1=eq&value1=SEARCHTERM0&constraint2=Chromosome.organism.name&op2=eq&value2=SEARCHTERM1&constraint3=Chromosome.genes.chromosomeLocation.start&op3=gt&value3=SEARCHTERM2&constraint4=Chromosome.genes.chromosomeLocation.end&op4=lt&value4=SEARCHTERM3&size=SEARCHTERM4&format=tab';

my @wanted = qw(Chromosome.primaryIdentifier Organism.name);

my $url = 'http://www.flymine.org/query/service/query/results?query=%3Cquery+name%3D%22%22+model%3D%22genomic%22+view%3D%22WANTED%22+sortOrder%3D%22WANTED+asc%22%3E%3C%2Fquery%3E&format=tab';

for my $wanted (@wanted) {
    my $getthis = $url;
    $getthis =~ s/WANTED/$wanted/g;
    print "\n" x 2, '-' x 70, "\n", "List of ${wanted}s", "\n" x 2;
    getprint($getthis);
}
