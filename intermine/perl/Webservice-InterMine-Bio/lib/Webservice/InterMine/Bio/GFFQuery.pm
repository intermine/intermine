package Webservice::InterMine::Bio::GFFQuery;

use Moose::Role;
with 'Webservice::InterMine::Bio::SequenceFeatureQuery';

use Carp qw/confess carp/;
use autodie qw(open);
require File::Temp;
use PerlIO::gzip;

requires 'service', 'service_root', 'get_request_parameters', 'view', 'model';

use constant {
    NO_BIODB => "Cannot create feature store - is Bio::DB::SeqFeature::Store (part of BioPerl) installed?",
};

sub get_feature_store {
    my $self = shift;
    eval 'require Bio::DB::SeqFeature::Store; 1;' or confess NO_BIODB;
    my $temp = File::Temp->new(SUFFIX => '.gff3.gz')->filename;
    $self->print_gff3(to => $temp);
    open (my $gff, '<:gzip', $temp);
    return Bio::DB::SeqFeature::Store->new(-adaptor => 'memory', -dsn => $gff);
}

sub get_gff3_iterator {
    my $self = shift;
    return $self->get_seq_iterator('gff3');
}

sub print_gff3 {
    my $self = shift;
    my %args = @_;
    return $self->print_seq(%args, format => 'gff3');
}

sub get_gff3 {
    my $self = shift;
    return $self->get_sequence('gff3');
}

sub get_gff3_uri {
    my $self = shift;
    return $self->get_sequence_uri('gff3');
}

1;
