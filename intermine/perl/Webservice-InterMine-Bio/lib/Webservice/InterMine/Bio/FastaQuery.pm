package Webservice::InterMine::Bio::FastaQuery;

use Moose::Role;
with 'Webservice::InterMine::Bio::SequenceFeatureQuery';

use Carp qw/confess carp/;
use autodie qw(open);
require File::Temp;
use PerlIO::gzip;

requires 'service', 'service_root', 'get_request_parameters', 'view', 'model';

use constant NO_SEQIO => "Cannot create seq-io object - is Bio::SeqIO (part of BioPerl) installed?";

sub get_seq_io {
    my $self = shift;
    eval 'require Bio::SeqIO; 1' or confess NO_SEQIO;
    my $temp = File::Temp->new(SUFFIX => '.fa.gz')->filename;
    $self->print_fasta(to => $temp);
    open (my $fa, '<:gzip', $temp);
    return Bio::SeqIO->new(-fh => $fa, -format => 'fasta');
}

sub get_fasta_iterator {
    my $self = shift;
    return $self->get_seq_iterator('fasta');
}

sub get_fasta {
    my $self = shift;
    return $self->get_sequence('fasta');
}

sub print_fasta {
    my $self = shift;
    my %args = @_;
    return $self->print_seq(%args, format => 'fasta');
}

sub get_fasta_uri {
    my $self = shift;
    return $self->get_sequence_uri('fasta');
}

1;

