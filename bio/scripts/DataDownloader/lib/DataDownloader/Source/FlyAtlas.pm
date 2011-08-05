package DataDownloader::Source::FlyAtlas;

use Moose;
extends 'DataDownloader::Source::ABC';
use autodie qw(open);

use constant {
    TITLE       => 'Fly Atlas',
    DESCRIPTION => "Microarray-based gene expression data for adult D. melanogaster tissues from FlyAtlas",
    SOURCE_LINK => 'http://www.flyatlas.org',
    SOURCE_DIR  => 'flyatlas',
    COMPARE     => 1,
    SOURCES     => [{
        SERVER         => "http://130.209.54.32/atlas",
        FILE           => "20090519all.txt",
        POST_PROCESSOR => sub {
            my ( $self, $src, $dest ) = @_;
            open( my $orig, '<', $src );
            open( my $fixed, '>', $dest );

            while (<$orig>) {
                s/FakeCall    /tubule vs whole fly - T-Test_Change Direction/gx;
                s/Grandmean   /TubuleMean                                   /gx;
                s/GrandSEM    /TubuleSEM                                    /gx;
                s/fakepresent /TubuleCall                                   /gx;
                print $fixed $_;
            }
        },
    }],
};

sub generate_version {
    return "2009-05-19";
}

1;
