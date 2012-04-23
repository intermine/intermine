package Webservice::InterMine::Parser::JSON;

=head1 NAME

Webservice::InterMine::Parser::JSON - parse rows of JSON results

=head1 DESCRIPTION

One of the parsers used to intepret results sent from 
the webservice.

=cut

use Moose;
with 'Webservice::InterMine::Parser';

use JSON::XS;
use MooseX::Types::Moose qw(Str);
use InterMine::Model::Types qw(Model);

=head1 IMPLEMENTED PARSER METHODS

The following methods implement the Parser interface.

=over 4

=item * header_is_parsed()
reports whether the header has been parsed yet.
Here, this reports whether the beginning of the JSON
object has been seen, and whether the results array
is open.

=item * parse_header($line) 
Parse a line from the connection as header information. 
This method may be called multiple times, depending on 
the return value of C<header_is_parsed>.

=item * parse_line($line)
Given a line of text, returns whatever its processor returns.
This implementation checks for headers reported in the footer.

=back

=cut

has json_decoder => (
    default => sub {JSON::XS->new},
    handles => {
        decode => 'decode'
    }
);

has model => (
    is => 'ro',
    isa => Model,
);

has json_format => (
    is => "ro",
    isa => Str,
    default => "perl",
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

sub parse_header {
    my $self = shift;
    my $line = shift;
    warn "HEADER-LINE: " . ((defined $line) ? $line : "NULL") if $ENV{DEBUG};
    return unless (defined $line);
    $self->add_to_header($line);
}

sub header_is_parsed {
    my $self = shift;
    return $self->header =~ /results["']:\[$/;
}

has completeness => (
    reader => '_get_completeness', 
    writer => '_set_completeness',
    isa => 'Bool', 
    default => 0,
);

# Horrible hack to get around issue with role requirements...
sub is_complete {
    my $self = shift; 
    return $self->_get_completeness;
};

sub check_status {
    my $self = shift;
    my $container_text = $self->header . $self->footer;
    $container_text =~ s/'/"/g; # Fix bad quotes.
    warn $container_text if $ENV{DEBUG};
    my $container = eval {$self->decode($container_text)};
    unless ($container) {
        confess "Problem decoding container", $@, $container_text;
    }
    return unless (exists $container->{wasSuccessful});
    confess "Results returned error: ", $container->{statusCode}, " - ", $container->{error}
        unless ($container->{wasSuccessful});
}

sub parse_line {
    my $self = shift;
    my $line = shift;
    if ($line and $line =~ /^\]/) {
        $self->add_to_footer($line);
        $self->check_status;
        $self->_set_completeness(1);
        return undef;
    }
    if ($line and length($line)) {
        chomp($line);
        $line =~ s/,\s*$//;
        my $json = eval {$self->decode($line);};
        unless ($json) {
            confess "error: " => $@, "problem line: " => $line;
        }
        return $self->process($json) || $line;
    } else {
        return undef;
    }
}

=head2 process 

Process a parsed json data structure into either a perl structure 
(which is what it now is), or two different flavours of object.

If this method returns a false value, it indicates that the line
parser should return the line as it was received.

=cut

sub process {
    my $self = shift;
    my $perl = shift;

    if ($self->json_format eq "inflate") {
        return inflate($perl);
    } elsif ($self->json_format eq "instantiate") {
        return $self->model->make_new($perl)
    } elsif ($self->json_format eq "raw") {
        return undef;
    } else {
        return $perl;
    }
}

=head1 FUNCTIONS

=head2 inflate( $thing )

Inflates the thing passed in, blessing hashes into 
L<Webservice::InterMine::ResultObjects>, 
and recursing through their values, and iterating over arrays.

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
