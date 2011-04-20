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

use InterMine::Util qw(get_property_value);


use Bio::EnsEMBL::Registry;
Bio::EnsEMBL::Registry->load_registry_from_db(
    -host => 'ensembldb.ensembl.org',
    -user => 'anonymous',
    -port => 5306);

# get the MemberAdaptor
my $member_adaptor = Bio::EnsEMBL::Registry->get_adaptor('Multi','compara','Member');

# fetch a Member

#my $member = $member_adaptor->fetch_all_by_source_taxon(7227);
#my $member = $member_adaptor->fetch_all();

# fetch a Member
my $member = $member_adaptor->fetch_by_source_stable_id(
    'ENSEMBLGENE','ENSG00000004059');


# print out some information about the Member
print " dbID: "  . $member->dbID
   . " stable_id: "  . $member-> stable_id
   . " taxon_id: "  . $member-> taxon_id
   . " genome_db_id: "  . $member-> genome_db_id    
   . " source_name: "  . $member->source_name  . "\n";


my $reg = "Bio::EnsEMBL::Registry";

$reg->load_registry_from_db(-host=>"ensembldb.ensembl.org", -user=>"anonymous");
my $genome_db_adaptor = $reg->get_adaptor("Multi", "compara", "GenomeDB");
my $all_genome_dbs = $genome_db_adaptor->fetch_all();
while ( my $db = shift @{$all_genome_dbs} ) {
   print $db->dbID . " name " . $db->name . "\n";
}
exit 0;
