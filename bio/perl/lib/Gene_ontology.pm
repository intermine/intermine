package Gene_ontology;

use strict;

sub new {
    my $packagename = shift;
    my ($go_id, $go_descript, $go_type) = @_;
    my $self = { go_id=>$go_id,
		 go_type=>$go_type,
		 go_descript=>$go_descript,
		 go_evidences=>[], #list of go_evidence objects.
		 date => undef,
		 assignby=>undef
		 };
    bless ($self, $packagename);
    return ($self);
}


sub set_go_info {
    my ($self, $go_id, $descript, $type) = @_;
    $self->{go_id} = $go_id;
    $self->{go_descript} = $descript;
    $self->{go_type} = $type;
    return ($self);
}


sub get_evidence {
    my $self = shift;
    return (@{$self->{go_evidences}});
}


sub add_evidence {
    my ($self, $ev_code, $evidence, $with_ev) = @_;
    my $evidence_obj = Gene_ontology::Evidence->new($ev_code, $evidence, $with_ev);
    push (@{$self->{go_evidences}}, $evidence_obj);
}
    
sub toString() {
    my $self = shift;
    my @evidence = $self->get_evidence();
    my $text = "GO assignment: ID: $self->{go_id}, Type: $self->{go_type}, Descript: $self->{go_descript}\n";
    $text .= "\tEvidence:\n";
    foreach my $evidence (@evidence) {
	$text .= "\t\t" . $evidence->toString() . "\n";
    }
    return ($text);
}

###############################

package Gene_ontology::Evidence;
use strict;

sub new {
    my ($packagename, $ev_code, $evidence, $with_ev) = @_;
    my $self = { ev_code=>$ev_code,
		 evidence=>$evidence,
		 with_ev=>$with_ev };
    bless ($self, $packagename);
    return ($self);
}


sub toString() {
    my ($self) = @_;
    my $text = "ev_code: $self->{ev_code}, evidence: $self->{evidence}, with_ev: $self->{with_ev}";
    return ($text);
}


1; #EOM
