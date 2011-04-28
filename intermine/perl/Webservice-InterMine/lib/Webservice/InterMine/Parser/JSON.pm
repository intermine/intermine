package Webservice::InterMine::Parser::JSON;

use Moose;
extends 'Webservice::InterMine::Parser';
use JSON -support_by_pp, -no_export;
use MooseX::Types::Moose qw(Str);
use InterMine::Model::Types qw(Model);

has model => (
    is => 'ro',
    isa => Model,
);

has json_format => (
    is => "ro",
    isa => Str,
    default => "perl",
);

has json_parser => (
    is         => 'ro',
    isa        => 'JSON',
    lazy_build => 1,
);

has header => (
    is => 'rw', 
    isa => Str,
    traits => ['String'],
    handles => {
        add_to_header => 'append',
    },
    default => '',
);

has footer => (
    is => 'rw', 
    isa => Str,
    traits => ['String'],
    handles => {
        add_to_footer => 'append',
    },
    default => '',
);

sub _build_json_parser {
    return JSON->new->allow_singlequote->allow_barekey;
}

override parse_header => sub {
    my $self = shift;
    my $line = shift;
    $self->add_to_header($line);
};

override header_is_parsed => sub {
    my $self = shift;
    return $self->header =~ /"results":\[$/;
};

sub check_status {
    my $self = shift;
    my $container_text = $self->header . $self->footer;
    my $container = $self->json_parser->decode($container_text);
    confess "Results returned error:", $container->{statusCode}, $container->{error}
        unless ($container->{wasSuccessful});
}

=head2 parse_line($line)

Parses a line of results. This method checks for errors in the footer.

=cut

override parse_line => sub {
    my $self = shift;
    my $line = shift;
    if ($line =~ /^\]/) {
        $self->add_to_footer($line);
        $self->check_status;
        return undef;
    }
    if (length($line)) {
        chomp($line);
        $line =~ s/,\s*$//;
        my $json = eval {$self->json_parser->decode($line);};
        unless ($json) {
            require Data::Dumper;
            confess Data::Dumper->Dump({"error" => $@, "problem line" => $line});
        }
        return $self->process($json);
    } else {
        confess "Unexpected end of input - transfer interrupted?";
    }
};

=head2 process 

Process a parsed json data structure into either a perl structure 
(which is what it now is), or two different flavours of object.

=cut

sub process {
    my $self = shift;
    my $perl = shift;

    if ($self->json_format eq "inflate") {
        return inflate($perl);
    } elsif ($self->json_format eq "instantiate") {
        return $self->model->make_new($perl)
    } else {
        return $perl;
    }
}

=head1 FUNCTIONS

=head2 inflate( thing )

Inflates the thing passed in, blessing hashes into Webservice::InterMine::ResultObjects, 
and recursing through their values, and iterating over their arrays.

=cut

sub inflate {
    my $thing = shift;
    my $type  = ref $thing;
    if ( $type eq "HASH" ) {
        bless $thing, "Webservice::InterMine::ResultObject";
        for my $sub_thing ( values %$thing ) {
            inflate($sub_thing);
        }
    }
    elsif ( $type eq "ARRAY" ) {
        for my $sub_thing (@$thing) {
            inflate($sub_thing);
        }
    }
    return $thing;
}

no Moose;
__PACKAGE__->meta->make_immutable;
1;
