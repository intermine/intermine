package DataDownloader::Source::IntAct;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use Carp "confess";
use Perl6::Junction qw(any);

use constant {
    TITLE => 'IntAct',
    DESCRIPTION => "All protein-protein interaction data from IntAct . See Protein Interactions aspect to retrieve full list of publcationsProtein interaction database",
    SOURCE_LINK => "http://www.ebi.ac.uk/intact",
    SOURCE_DIR => 'psi/intact',

    # For determining versions
    HOST => "ftp.ebi.ac.uk",
    REMOTE_DIR => "pub/databases/IntAct/current",
    REF_FILE => "all.zip",
};

use constant ORGANISMS => qw(mouse human caeel drome yeast ecoli bacillus);

sub BUILD {
    my $self = shift;
    my $ftp = $self->connect('pub/databases/IntAct/current/psi25/species');
    my @psi_files = $ftp->ls or confess "Could not ls directory";
    my @resources = ();
    for my $file (@psi_files) {
        # find what organism the file represents and 
        # check if we are interested in it
            
	    my ($start) = split(/_/, $file);
        if ($start eq any(ORGANISMS)) {
            push @resources, {
                SUBTITLE => "PSI - " . $start,
                HOST => "ftp.ebi.ac.uk",
                REMOTE_DIR => "pub/databases/IntAct/current/psi25/species",
                FILE => $file,
            };
	    }             
    }
    $self->set_sources([@resources]);
}

1;
