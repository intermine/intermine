package DataDownloader::Source::EnsemblCompara;

use strict;
use warnings;

use Moose;
use MooseX::FollowPBP;
with 'DataDownloader::Role::Source';

use Bio::EnsEMBL::Registry;
use autodie qw(open close);
use Ouch qw(:traditional);
use File::Path qw(mkpath);
use Operator::Util qw(cross);
use Set::Object qw(set);
use List::MoreUtils qw(pairwise part uniq);

use constant {
    TITLE => "Ensembl Compara",
    DESCRIPTION => "Ensembl Comparative Genomics Data",
    SOURCE_LINK => "http://www.ensembl.org",
    SOURCE_DIR => "ensembl/compara",
    COMPARE => 1,
};

use constant ORGANISMS => qw(
    homo_sapiens danio_rerio mus_musculus caenorhabditis_elegans 
    saccharomyces_cerevisiae drosophila_melanogaster rattus_norvegicus
);
# only load genes if they are homologues of gene of organism of interest.  can be null
use constant HOMOLOGUES => qw(
    homo_sapiens danio_rerio mus_musculus caenorhabditis_elegans 
    saccharomyces_cerevisiae drosophila_melanogaster rattus_norvegicus
);

sub generate_version {
    return Bio::EnsEMBL::Registry::software_version();
}

sub fetch_all_data {
    my $self = shift;
    Bio::EnsEMBL::Registry->load_registry_from_db(
        -host => 'ensembldb.ensembl.org',
        -user => 'anonymous',
        -port => 5306);

    my $genome_db = Bio::EnsEMBL::Registry->get_adaptor("Multi", "compara", "GenomeDB")
        or throw DownloadError => "Cannot get genome-db adaptor";
    my $homology = Bio::EnsEMBL::Registry->get_adaptor('Multi','compara','Homology')
        or throw DownloadError => "Cannot get homology adaptor";
    my $mlss = Bio::EnsEMBL::Registry->get_adaptor("Multi", "compara", "MethodLinkSpeciesSet")
        or throw DownloadError => "Cannot get Method link species set";

    my $dir = $self->get_destination_dir;
    mkpath("$dir") unless (-d $dir);

    my @cp = cross([ORGANISMS], [HOMOLOGUES], flat => 0);
    my $i = 0;
    my ($list_a, $list_b) = part {$i++ % 2} @cp;
    my @combinations = pairwise {set($a, $b)} @$list_a, @$list_b;
    my @used_combinations;
    for my $combination (@combinations) {

        $self->debug("Processing", $combination->members);

        # Don't do both A-B and B-A
        next if ($combination->size < 2);
        next if (grep {$combination->equal($_)} @used_combinations);
        push @used_combinations, $combination;

        # Get a homologues object
        my ($org_a, $org_b) = $combination->members;
        my $db_a = $genome_db->fetch_by_registry_name($org_a);
        my $db_b = $genome_db->fetch_by_registry_name($org_b);
        my $species_set = $mlss->fetch_by_method_link_type_GenomeDBs(
            ENSEMBL_ORTHOLOGUES => [$db_a, $db_b]);
        my $homologues = $homology->fetch_all_by_MethodLinkSpeciesSet($species_set);

        # Open the file to write to
        my $out_file = $dir->file($org_a . '-' . $org_b . '.tsv');
        $self->debug("Writing $out_file");
        my $out = $out_file->openw;

        while (my $homologue_info = shift @$homologues) {
            my $gene_list = $homologue_info->gene_list();
            while ( my $member = shift @{$gene_list} ) {
                my $taxon_id = $member->taxon_id;
                my $stable_id = $member->stable_id;
                
                my $dblinks = $gene->get_all_DBLinks('Entrez%');
                my @symbols;
                my @secondaryIdentifiers;
                while ( my $dbentry = shift @{$dblinks} ) {
                    push @symbols, $dbentry->display_id;
                    push @secondaryIdentifiers, $dbentry->primary_id;
                }
                my $symbol = join('|', uniq(@symbols));
                my $sec_id = join('|', uniq(@secondaryIdentifiers));

                $out->print(join("\t", $taxon_id, $stable_id, $symbol, $sec_id), "\n");
            }
        }
        close $out;
    }
}

1;
