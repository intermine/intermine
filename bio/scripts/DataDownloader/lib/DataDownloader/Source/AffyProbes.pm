package DataDownloader::Source::AffyProbes;

use Moose;
use MooseX::FollowPBP;
with 'DataDownloader::Role::Source';

use Bio::EnsEMBL::Registry;
use autodie qw(open close);
use Ouch qw(:traditional);

use constant {
    TITLE => "Affymetrix Probesets",
    DESCRIPTION => "Mapping of Affymetrix Probesets to Genomic location",
    SOURCE_LINK => "http://www.ensembl.org",
    SOURCE_DIR => "affy-probes",
    DB => 'drosophila_melanogaster',
};

sub fetch_all_data {
    my $self = shift;
    mkdir $self->get_source_dir unless (-d $self->get_source_dir);
    mkdir $self->get_destination_dir unless (-d $self->get_destination_dir);
    my $dest = $self->get_destination_dir;
    my $file = $dest->file("AFFY_Drosphila_2.txt");
    my $out  = $file->openw;

    Bio::EnsEMBL::Registry->load_registry_from_db(
        -host => 'ensembldb.ensembl.org',
        -user => 'anonymous',
        -port => 5306,
    );
    my $sa  = Bio::EnsEMBL::Registry->get_adaptor( DB, 'core', 'Slice' )
        or throw "DownloadError" => "No slice adaptor found for " . DB;

    my $ofa = Bio::EnsEMBL::Registry->get_adaptor( DB, 'funcgen', 'OligoFeature' )
        or throw "DownloadError" => "No oligo-feature adaptor found for " . DB;

    my @slices = @{ $sa->fetch_all('toplevel') };

    for my $slice (@slices) {
        my @genes = @{ $slice->get_all_Genes };
        for my $gene (@genes) {
            my @transcripts = @{ $gene->get_all_Transcripts };
            for my $transcript (@transcripts) {
                my @xrefs = @{ $transcript->get_all_DBEntries };
                for my $xref (@xrefs) {
                    if ( $xref->dbname eq 'AFFY_Drosophila_2' ) {
                        my @probe_features =
                        @{ $ofa->fetch_all_by_probeset( $xref->display_id ) };
                        for my $probe_feature (@probe_features) {
                            print( $out $gene->stable_id, "\t",
                                $transcript->stable_id,           "\t",
                                $xref->display_id,                "\t",
                                $probe_feature->seq_region_name,  "\t",
                                $probe_feature->seq_region_start, "\t",
                                $probe_feature->seq_region_end,   "\n"
                            );
                        }
                    }
                }
            }
        }
    }
    close $out;
}

1;
