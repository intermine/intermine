package DataDownloader::Source::RedFly;

use Moose;
use Web::Scraper;
extends 'DataDownloader::Source::ABC';

use constant {
    TITLE       => 
        'RedFly GFF',
    DESCRIPTION => 
        "Transcriptional CRMs and TFBSs from RedFly",
    SOURCE_LINK => 
        "http://redfly.ccr.buffalo.edu",
    SOURCE_DIR => "flymine/redfly",
    METADATA_URL => "http://redfly.ccr.buffalo.edu/index.php",
};

sub BUILD {
    my $self = shift;
    $self->set_sources([
        {
            SERVER  => 'http://redfly.ccr.buffalo.edu/datadumps',
            FILE    => "tfbs_dump.gff",
            SUB_DIR => ["tfbs"],
        },
        {
            SERVER  => 'http://redfly.ccr.buffalo.edu/datadumps',
            FILE    => "crm_dump.gff",
            SUB_DIR => ["crm"],
        },
    ]);
}

sub generate_version {
    my $self = shift;
    my $scraper = scraper {
        process 'div.lastupdate', version => 'TEXT';
    };
    my $ua = LWP::UserAgent->new(agent => 'Mozilla/5.0');
    my $response = $ua->get(METADATA_URL);
    confess $response->status_line unless $response->is_success;
    my $scraps = $scraper->scrape($response);
    my ($version) = $scraps->{version} =~ /(\d+-\S*-\d{4})/g;
    die "Could not determine RedFly version" unless $version;
    return $version;
}
1;

