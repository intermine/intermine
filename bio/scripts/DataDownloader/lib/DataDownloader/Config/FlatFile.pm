package DataDownloader::Config::FlatFile;

use Exporter qw(import);
our @EXPORT_OK = qw(config_species);

use Method::Signatures;
use Carp qw(confess);
use IO::All;

func config_species($file, $trigger) {
    my %data;

    my $io = io($file);
    if ($io->exists()) {
        while ( $_ = $io->getline ) {
            my ($domain, @fields) = split(/\t/);
            chomp @fields;
            if ($domain eq $trigger) {
                if ( @fields == 3 ) {
                    my ($k1, $k2, $v) = @fields;
                    $data{$k1}{$k2} = $v;
                } elsif ( @fields == 1 ) {
                    #for 2 value configs i.e. get_go_annoatation
                    my $field = shift @fields;
                    $data{ $field } = $field;
                } else {
                    confess("I don't know how to parse this line (I can only do 1 or 3 fields): got $_");
                }
            }
        }
        return %data;
    } else {
        confess "Could not find $file";
    }
}

1;
