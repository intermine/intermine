package DataDownloader::Source::RNAiScreens;

use Moose;
extends 'DataDownloader::Source::ABC';
use URI;
use Web::Scraper;

use constant {
    TITLE       => 'RNAi Screens',
    DESCRIPTION => 'Screen Data From www.flyrnai.org',
    SOURCE_LINK => 'http://www.flyrnai.org',
    SOURCE_DIR  => 'drsc',
    COMPARE     => 1,
    SOURCES => [{
        FILE   => 'RNAi_all_hits.txt',
        SERVER => 'http://www.flyrnai.org',
        POST_PROCESSOR => sub {
            my ($self, $src, $dest) = @_;
            open( my $orig, '<', $src );
            open( my $fixed, '>', $dest );

            my $printing = 0;

            while (<$orig>) {
                $printing = $printing || /Key:/;
                print $fixed $_ if $printing;
            }

        },
    }],
};

override fetch_all_data => sub {
    my $self = shift;
    super();
    $self->fetch_screen_details();
};

sub fetch_screen_details {
    my $self = shift;
    my $out = $self->get_destination_dir->file("RNAi_screen_details")->openw();
    binmode $out, ':encoding(utf8)';
    my $citations = scraper {
        # Parse all <p> elements with the class 'cite'
        process "p.cite",  "citations[]" => { text => 'TEXT' };
        process "p.cite",  "citation_links[]" => scraper {
            process 'a', 'links[]' => '@href';
        };
    };

    my $screen_names = scraper {
        process 'h2', title => 'TEXT';
    };

    my $res = $citations->scrape( URI->new("http://www.flyrnai.org/DRSC-PRY.html") );
    for my $index (0 .. $#{ $res->{citations} }) {
        my @cols;
        my $citation = $res->{citations}[$index]{text};
        my ($first_author) = split(/\s+/, $citation);
        $cols[1] = $first_author;
        my $citation_links = $res->{citation_links}[$index];
        for my $link (@{ $citation_links->{links} }) {
            if ($link =~ m!ncbi\.nlm\.nih.*list_uids!) {
                my ($uid) = $link =~ /list_uids=(\d+)/g;
                $cols[0] = $uid || '';
            }
            if ($link =~ /RNAi_public_screen\.pl/) {
                my $title = $screen_names->scrape($link)->{title};
                $title =~ s/ - .*//;
                $cols[2] = $title;
            }
        }
        next unless (@cols == 3);
        my $line = join("\t", map {$_} @cols) . "\n";
        $self->debug($line);
        print $out $line;
    }
    close $out;
}

1;
