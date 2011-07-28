use Test::More;
use Webservice::InterMine::Bio qw/GFF3 FASTA BED/;
use Webservice::InterMine;

unless ($ENV{RELEASE_TESTING}) {
    plan skip_all => "Live test for release testing only";
} else {
    my $service = Webservice::InterMine->get_service('flymine');
    my $query = $service->new_query(with => GFF3);

    $query->add_sequence_features(qw/Gene Gene.exons Gene.exons.transcripts/);

    $query->add_constraint(qw/Gene.chromosome.primaryIdentifier = 4/);

    $query->print_gff3(to => '/tmp/out-uncompressed.gff3');
    $query->print_gff3(to => '/tmp/out-compressed.gff3.gz');

    $query->print_gff3;

    my $db = $query->get_feature_store;

    print $_ for ($db->get_features_by_type('ncRNA'));

    my $faq = $service->new_query(with => FASTA);

    $faq->add_sequence_features(qw/Gene.exons/);

    $faq->add_constraint(qw/Gene.chromosome.primaryIdentifier = 4/);

    $faq->print_fasta(to => '/tmp/out-uncompressed.fa');
    $faq->print_fasta;

    my $seqio = $faq->get_seq_io;
    my $out = Bio::SeqIO->new(-file => '>-', -format => 'EMBL'); 

    while (my $seq = $seqio->next_seq() ) {
        $out->write_seq($seq);
    }

    $query = $service->new_query(class => 'Gene', with => BED);
    $query->add_sequence_features("Gene", "Gene.exons", "Gene.transcripts");
    $query->add_constraint("symbol" => [qw/h H eve zen/]);

    $query->print_bed;
}
