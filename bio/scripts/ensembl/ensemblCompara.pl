#!/usr/bin/perl
# translates data from ensembl compara database to flat file

BEGIN {
    my $base = ( $0 =~ m:(.*)/.*: )[0];
    unshift( @INC, 
        map( {$base . $_} 
            '/../../../intermine/perl/InterMine-Util/lib',
            '/../../../intermine/perl/InterMine-Item/lib',
        ),
    );
}

use strict;
use warnings;
use Switch;

use Bio::EnsEMBL::Registry;
Bio::EnsEMBL::Registry->load_registry_from_db(
    -host => 'ensembldb.ensembl.org',
    -user => 'anonymous',
    -port => 5306);

# load all genes
my @organisms = ('homo_sapiens', 'danio_rerio', 'mus_musculus', 'caenorhabditis_elegans', 'saccharomyces_cerevisiae', 'drosophila_melanogaster', 'rattus_norvegicus');
# only load genes if they are homologues of gene of organism of interest.  can be null
my @homologues = ('homo_sapiens', 'danio_rerio', 'mus_musculus', 'caenorhabditis_elegans', 'saccharomyces_cerevisiae', 'drosophila_melanogaster', 'rattus_norvegicus');

# human, zebrafish, mouse, worm, yeast, fruitfly, rat

my $reg = "Bio::EnsEMBL::Registry";

my $member_adaptor = $reg->get_adaptor('Multi','compara','Member');
my $members = $member_adaptor->fetch_all_by_source_taxon('ENSEMBLGENE', 9606);


   while ( my $member = shift @{$gene_list} ) {
         my $taxon_id = $member->taxon_id;
         my $gene = $member->get_Gene();
         print $taxon_id . "\t" . $taxon_id . "\t";
         
         my $dblinks = $gene->get_all_DBLinks('Entrez%');
         my $symbol;
         my $secondaryIdentifier;
         while ( my $dbentry = shift @{$dblinks} ) {
            $symbol = $dbentry->display_id;
            $secondaryIdentifier = $dbentry->primary_id;
         }
         print $symbol . "\t" . $secondaryIdentifier . "\t";
   }
   print "\n";


exit 0;
