package DataDownloader::Source::Biogrid;

use Moose;
extends 'DataDownloader::Source::ABC';
use LWP;
use Web::Scraper;

use constant {
    TITLE => 'Biogrid',
    DESCRIPTION => 'Biological General Repository for Interaction Datasets',
    SOURCE_LINK => 'http://thebiogrid.org',
    SOURCE_DIR => 'psi/biogrid',
    METADATA_URL => "http://thebiogrid.org/scripts/fetchDirectoryDescription.php",
};
use constant ORGANISMS => (
    "Drosophila_melanogaster",
    "Caenorhabditis_elegans", 
    "Saccharomyces_cerevisiae",
);

sub BUILD {
    my $self = shift;
    my $version = $self->get_version;
    my @files_to_extract = 
        map { 'BIOGRID-ORGANISM-' . $_ . '-' . $version . '.psi25.xml'} 
        ORGANISMS;

    $self->set_sources([
        {
            SERVER => 'http://thebiogrid.org/downloads/archives/Release%20Archive/BIOGRID-' . $version,
            FILE => 'BIOGRID-ORGANISM-' . $version . '.psi25.zip',

            CLEANER => sub {
                my $self = shift;
                my $file = $self->get_destination;
                my @args = ('unzip', $file, @files_to_extract, '-d', 
                    $self->get_destination_dir);
                $self->execute_system_command(@args);
            },
        },
    ]);
}

sub generate_version {
    my $self = shift;
    my $scraper = scraper {
        process 'h2', heading => 'TEXT';
    };
    my $ua = LWP::UserAgent->new(agent => 'Mozilla/5.0');
    my $response = $ua->post(METADATA_URL, {directory => '/Current Release'});
    confess $response->status_line unless $response->is_success;
    my $scraps = $scraper->scrape($response);
    my ($version) = $scraps->{heading} =~ /Release\s+(.*)/g;
    die "Could not determine Biogrid version" unless $version;
    return $version;
}
