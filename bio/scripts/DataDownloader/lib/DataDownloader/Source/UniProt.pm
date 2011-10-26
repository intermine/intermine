package DataDownloader::Source::UniProt;

use Moose;
extends 'DataDownloader::Source::FtpBase';
use MooseX::FollowPBP;
use URI;
use Ouch qw(:traditional);

use constant {
    TITLE => 'UniProt',
    DESCRIPTION => 'Proteins from UniProt (TREmbl and SwissProt)',
    SOURCE_LINK => 'http://www.ebi.uniprot.org/index.shmtl',
    SOURCE_DIR => 'uniprot', 
    # For retrieving version info.
    HOST => 'ftp.uniprot.org',
    REMOTE_DIR => 'pub/databases/uniprot/current_release',
    REF_FILE => 'relnotes.txt',
};
use constant NON_XML_FILES => qw(
    uniprot.xsd            uniprot_sprot_varsplic.fasta.gz  
);

sub BUILD {
    my $self = shift;
    my @sources = map {{
      HOST       => 'ftp.uniprot.org', 
      REMOTE_DIR => 'pub/databases/uniprot/current_release/knowledgebase/complete',
      FILE       => $_,
      EXTRACT    => (/gz$/) ? 1 : 0,
    }} NON_XML_FILES;
    my $organisms = $self->get_options->{organisms} || [];

    my $excluded_organisms = $self->get_options->{excluded_organisms} || [];
    my $excluded_str = join(' ', map { "NOT $_" } @$excluded_organisms);

    my $header_checker = sub {
        my $self = shift;
        my $response = shift;
        my $total = $response->header("X-Total-Results");
        if (defined $total and $total) {
            $self->debug("Returned $total results.");
            return 1;
        } elsif (not defined $total) {
            $self->debug("No total count found, assuming it went ok");
            return 1;
        } else {
            $self->debug("$total results returned. Not saving file");
            return 0;
        }
    };

    for my $org (@$organisms) {
        my $sp_uri = URI->new("http://www.uniprot.org/uniprot/");
        my %sp_params = (
            query => "taxonomy:" . $org . ' AND fragment:no AND reviewed:yes ' . $excluded_str,
            #compress => 'yes', 
            format => 'xml',
        );
        $sp_uri->query_form(%sp_params);
        push @sources, {
            SUBTITLE => "Swissprot $org",
            URI => "$sp_uri",
            FILE => $org . '_uniprot_sprot.xml', #.gz',
            #EXTRACT => 1,
            METHOD => 'HTTP',
            HEADER_CHECKER => $header_checker,
        };

        my $tr_uri = URI->new("http://www.uniprot.org/uniprot/");
        my %tr_params = (
            query => "taxonomy:" . $org . ' AND fragment:no AND reviewed:no ' . $excluded_str,
            #compress => 'yes', 
            format => 'xml',
        );
        $tr_uri->query_form(%tr_params);
        push @sources, {
            SUBTITLE => "Trembl $org",
            URI => "$tr_uri",
            FILE => $org . '_uniprot_trembl.xml', #.gz',
            #EXTRACT => 1,
            METHOD => 'HTTP',
            HEADER_CHECKER => $header_checker,
        };
    }

    push @sources, {
        HOST => 'ftp.uniprot.org', 
        REMOTE_DIR => 'pub/databases/uniprot/current_release/knowledgebase/complete/docs',
        FILE => 'keywlist.xml.gz',
        EXTRACT => 1,
    }; 
    $self->set_sources([@sources]);
}

override generate_version => sub {
    my $self       = shift;
    my $ftp        = $self->connect;
    my $notes;

    open( my ($string_buffer), '>', \$notes );
    $ftp->get( "relnotes.txt", $string_buffer )
      or die "Failed to retrieve release notes";
    close $string_buffer;
    $ftp->quit;

    if ( $notes =~ /^UniProt \s+ Release \s+ (\S+)/mx ) {
        return $1;
    } else {
        throw DownloadError => "Failed to retrieve version";
    }
};

1;
