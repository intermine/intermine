package Webservice::InterMine::Role::Showable;

use Moose::Role;

requires qw/to_string views table_format results_iterator/;

sub show {
    my $self = shift;
    my $fh = shift || \*STDOUT;

    binmode $fh, ':encoding(utf8)';
    print $fh $self->to_string, "\n";
    printf $self->table_format, $self->views;
    my $iter = $self->results_iterator;
    while (<$iter>) {
        printf $self->table_format, map {(defined $_) ? $_ : 'UNDEF'} @$_;
    }
}

1;
