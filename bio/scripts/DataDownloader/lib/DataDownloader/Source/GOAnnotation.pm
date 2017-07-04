package DataDownloader::Source::GOAnnotation;

=head1 NAME                                                                                                                                                                                                                                  
                                                                                                                                                                                                                                             
DataDownloader::Source::GOAnnotation;                                                                                                                                                                                                                  
                                                                                                                                                                                                                                             
=head1 SYNOPSIS                                                                                                                                                                                                                              
                                                                                                                                                                                                                                             
                                                                                                                                                                                                                                             
Download Gene ontology association files for specified organisms                                                                                                                                                                             
                                                                                                                                                                                                                                             
http://geneontology.org/gene-associations/gene_association.DB_SUFFIX.gz                                                                                                                                                                      
     > DATA_DIR/go-annotation/TAXON_ID/DATE/gene_association.DB_SUFFIX.gz                                                                                                                                                                    
sort downloaded file as above                                                                                                                                                                                                                
     link dir DATA_DIR/go-annotation/TAXON_ID/DATE DATA_DIR/go-annotation/TAXON_ID/current                                                                                                                                                   
                                                                                                                                                                                                                                             
=cut                                                                                                                                                                                                                                         

use Moose;
extends 'DataDownloader::Source::ABC';
use PerlIO::gzip;
use File::Basename;
use autodie qw(open close);
use DataDownloader::Util 'get_ymd';

use constant {
    TITLE => "GO Annotation",
    DESCRIPTION => "Gene Ontology Assignments from Uniprot and the Gene Ontology Site",
    SOURCE_LINK => "http://www.geneontology.org",
    SOURCE_DIR => "go-annotation",
};

my %GOA_TAXA = (flybase => 'gene_association.fb', wormbase => 'gene_association.wb', mgi => 'gene_association.mgi', human => 'goa_human.gaf', zfin => 'gene_association.zfin', sgd => 'gene_association.sgd', rgd => 'gene_association.rgd');
sub field2_of { return [ split( /\t/, shift ) ]->[1] || '' }
my $order = sub { field2_of($a) cmp field2_of($b) };

my $goa_cleaner = sub {
    my $self = shift;
    $self->unzip_dir();
    my $file = substr( $self->get_destination, 0, -3 );
    $self->debug( "Sorting " . $file );
    open( my $in, '<', $file );
    unlink $file;    # Filter - don't clobber                                                                                                                                                                                                
    open( my $out, '>', $file );
    print $out sort( $order (<$in>) );
};

sub BUILD {
    my $self    = shift;
    my @sources = (
        {   
            SUBTITLE   => "GO",
            SERVER     => "http://purl.obolibrary.org/obo",
            FILE       => "go.obo",
        }
    );

    while ( my ( $taxon, $db_suffix ) = each %GOA_TAXA ) {
        push @sources,
          {
            SUBTITLE   => "GOA - " . $taxon,
            SERVER     => "http://www.geneontology.org/gene-associations",
            FILE       => "$db_suffix.gz",
            SUB_DIR    => [$taxon],
            CLEANER    => $goa_cleaner,
          };
    }
    $self->set_sources( [@sources] );
}

1;
