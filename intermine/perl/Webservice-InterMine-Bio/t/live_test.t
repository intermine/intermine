use lib 'lib';
use feature qw/say/;

use Webservice::InterMine::Bio qw/GFF3/;
use Webservice::InterMine 'localhost/flymine';

my $query = Webservice::InterMine->new_query(with => GFF3);

$query->add_sequence_features(qw/Gene Gene.exons Gene.exons.transcripts/);

$query->add_constraint(qw/Gene.chromosome.primaryIdentifier = 4/);

$query->print_gff3(to => '/tmp/out-uncompressed.gff3');
$query->print_gff3(to => '/tmp/out-compressed.gff3.gz');

$query->print_gff3;

my $db = $query->get_sequence_feature_store;

say $_ for ($db->get_features_by_type('ncRNA'));

