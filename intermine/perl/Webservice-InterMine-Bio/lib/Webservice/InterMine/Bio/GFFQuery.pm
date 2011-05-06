package Webservice::InterMine::Bio::GFFQuery;

use Moose::Role;
use autodie qw(open);
use IO::Handle;

use constant GFF3_PATH => '/query/results/gff3';

requires 'service', 'service_root', 'get_request_parameters', 'view', 'model';

require Bio::DB::SeqFeature::Store;
require File::Temp;
require URI;
use PerlIO::gzip;
require Webservice::InterMine::Path;

sub get_sequence_feature_store {
    my $self = shift;
    my $temp = File::Temp->new(SUFFIX => '.gff3.gz')->filename;
    $self->print_gff3(to => $temp);
    open (my $gff, '<:gzip', $temp);
    return Bio::DB::SeqFeature::Store->new(-adaptor => 'memory', -dsn => $gff);
}

sub get_gff3_iterator {
    my $self = shift;
    my $url = $self->service_root . GFF3_PATH;
    return $self->service->get_results_iterator(
        $url,
        {$self->get_request_parameters},
        $self->view, 
        'tsv', 
        'perl',
        undef,
    );
}

sub add_sequence_features {
    my $self = shift;
    my @features = @_;
    for my $f (@features) {
        my $feature_type = Webservice::InterMine::Path::last_cd($self->model, $f);
        confess "$f is not a sequence feature" 
            unless ($feature_type->sub_class_of('SequenceFeature'));
    }
    $self->add_view(map {$_ . '.primaryIdentifier'} @features);
}

sub print_gff3 {
    my $self = shift;
    my %args = @_;
    my $out = $args{to};
    my $fh;
    my $compress = $args{compress};
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
        my $uri = URI->new($self->service_root . GFF3_PATH);
        $uri->query_form(compress => $compress, $self->get_request_parameters);
        my $response = $self->service->get($uri, ':content_cb' => sub {$fh->print($_[0])});
        confess $response->status_line if $response->is_error;
    } else {
        binmode $fh, ':encoding(utf8)'; 
        my $iterator = $self->get_gff3_iterator();
        while (<$iterator>) {
            $fh->print($_, "\n");
        }
    }
}

1;



