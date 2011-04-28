package Webservice::InterMine::Parser::FlatFile;

use Moose;
extends 'Webservice::InterMine::Parser';

override header_is_parsed => sub {
    return 1;
};

override parse_header => sub {};

override parse_line => sub {
    my $self = shift;
    my $line = shift;
    if ($line and $line =~ /^\[ERROR/) {
        confess "Results returned error:", $line;
    } else{
        return $line;
    }
};

no Moose;
__PACKAGE__->meta->make_immutable;
1;
