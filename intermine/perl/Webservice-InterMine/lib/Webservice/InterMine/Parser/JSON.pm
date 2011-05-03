package Webservice::InterMine::Parser::JSON;

use Moose;
with 'Webservice::InterMine::Parser';
with 'Webservice::InterMine::Role::KnowsJSON';

use JSON -support_by_pp, -no_export;
use MooseX::Types::Moose qw(Str);
use InterMine::Model::Types qw(Model);

=head1 Webservice::InterMine::Parser::JSON

Return each row of results parsed from JSON into a perl data 
structure.

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
    $self->add_to_header($line);
}

sub header_is_parsed {
    my $self = shift;
    return $self->header =~ /"results":\[$/;
}

sub check_status {
    my $self = shift;
    my $container_text = $self->header . $self->footer;
    my $container = $self->decode($container_text);
    confess "Results returned error:", $container->{statusCode}, $container->{error}
        unless ($container->{wasSuccessful});
}

sub parse_line {
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
        my $json = eval {$self->decode($line);};
        unless ($json) {
            require Data::Dumper;
            confess Data::Dumper->Dump({
                "error" => $@, "problem line" => $line
            });
        }
        return $self->process($json);
    } else {
        return undef;
    }
}

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
