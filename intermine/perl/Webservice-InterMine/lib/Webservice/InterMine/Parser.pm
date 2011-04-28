package Webservice::InterMine::Parser;

use Moose;

sub header_is_parsed {
    confess "This method must be overriden in the implementing class";
}

sub parse_header {
    confess "This method must be overriden in the implementing class";
}

sub parse_line {
    confess "This method must be overriden in the implementing class";
}

no Moose;
__PACKAGE__->meta->make_immutable;
1;
