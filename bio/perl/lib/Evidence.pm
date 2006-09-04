#!/usr/local/bin/perl

package Evidence;
use strict;
use Data::Dumper;

sub new {
    my $packagename = shift;
    my $self = {};
    bless ($self, $packagename);
    $self->_init();
    return ($self);
}

sub _init {
    my $self = shift;
    $self->{FEAT_NAME} = undef; #string
    $self->{FEAT_TYPE} = undef; #string
    $self->{METHOD} = undef; #string
    $self->{ACCESSION} = undef; #string
    $self->{DESCRIPTION} = undef; #string
    $self->{PER_ID} = undef; #float
    $self->{SCORE} = undef; #float
    $self->{E_VALUE} = undef; #float
    $self->{ELEMENT_NUM} = undef; #int
    $self->{ASMBL_COORDS} = undef; #set to coordset
    $self->{MATCH_COORDS} = undef; #set to coordset
    $self->{RELATIVE_COORDS} = undef; #set to coordset
    $self->{DB_NAME} = undef; #string
    $self->{PREDICTION_TOOL} = undef; #string
    $self->{EV_CLASS} = undef; #string [SEQUENCE_DB_MATCH | COMPUT_PREDICTION] 
}

sub set_evidence_coords {
    my $self = shift;
    my $coord_type = shift; #[ASMBL_COORDS|MATCH_COORDS|RELATIVE_COORDS]
    my ($end5, $end3) = @_;
    $self->{$coord_type} = Coordset->new($end5,$end3);
}

sub toString () {
    my $self = shift;
    my $text = "\n\nEvidence: \n";
    foreach my $key (keys %$self) {
	my $value = $self->{$key};
	unless ($value) { next;}
			
	if ((ref $value) =~ /Coordset/) {
	    $text .= $value->toString() . "\n";
	} else {
	    $text .= "$key = $value\n";
	}
    }
    return ($text);
}



##########################################################

package Coordset;
use strict;

sub new {
    my $packagename = shift;
    my $self = {};
    bless ($self, $packagename);
    if (@_) {
	$self->set_coords(@_);
    }
    return ($self);
}

sub set_coords {
    my $self = shift;
    my ($end5, $end3) = @_;
    $self->{end5} = $end5;
    $self->{end3} = $end3;
}

sub get_coords {
    my $self = shift;
    return ($self->{end5},$self->{end3});
}
 

sub toString {
    my $self = shift;
    return ("end5: $self->{end5}, end3: $self->{end3}");
}



1; #EOM
