package Webservice::InterMine::Bio::SequenceFeatureQuery;

use strict;
use warnings;

use Moose::Role;
requires 'model', 'add_view';

use autodie qw(open);
use IO::Handle;
use Carp qw/confess carp/;
require URI;

use Webservice::InterMine::Path qw/type_of/;

sub add_sequence_features {
    my $self = shift;
    my @features = @_;
    my $model = $self->model;
    my $seq_feature_cd = $model->get_classdescriptor_by_name('SequenceFeature');
    for my $f (@features) {
        confess "$f is not a sequence feature" unless (eval{
            $seq_feature_cd->superclass_of(type_of($model, $f))});
    }
    $self->add_view(map {$_ . '.primaryIdentifier'} @features);
}

sub get_seq_iterator {
    my $self = shift;
    my $format = shift;
    my $url = $self->service_root . $self->service->get_resource_path('query.' . $format);
    return $self->service->get_results_iterator(
        $url,
        {$self->get_request_parameters},
        $self->view, 
        'tsv', 
        'perl',
        undef,
    );
}

sub get_sequence_uri {
    my ($self, $format) = @_;
    my $uri = URI->new(
        $self->service_root . $self->service->get_resource_path('query.' . $format));
    $uri->query_form($self->get_request_parameters);
    return "$uri";
}

sub get_sequence {
    my ($self, $format) = @_;
    my $iterator = $self->get_seq_iterator($format);
    return join("\n", $iterator->get_all);
}

sub print_seq {
    my $self = shift;
    my %args = @_;
    my $out = $args{to};
    my $compress = $args{compress};
    my $format = $args{format};

    my $fh;
    if (ref $out eq 'GLOB' || blessed $out and $out->can('print')) {
        $fh = $out;
    } elsif (defined $out) {
        $compress = 'gzip' if ($out =~ /\.gz$/);
        open($fh, '>', $out);
    } else {
        open($fh, '>-');
    }

    if ($compress) {
        binmode $fh;
        my $uri = URI->new($self->service_root 
            . $self->service->get_resource_path('query.' . $format));
        $uri->query_form(compress => $compress, $self->get_request_parameters);
        my $response = $self->service->get($uri, ':content_cb' => sub {$fh->print($_[0])});
        confess $response->status_line if $response->is_error;
    } else {
        binmode $fh, ':encoding(utf8)'; 
        my $iterator = $self->get_seq_iterator($format);
        while (<$iterator>) {
            $fh->print($_, "\n");
        }
    }
}

1;
