package Webservice::InterMine::ResultIterator;

use Moose;
use InterMine::TypeLibrary 0.9500 qw(HTTPCode NetHTTP PathList TextCSV);
use MooseX::Types::Moose qw(Str HashRef Bool Num GlobRef);
use HTTP::Status qw(status_message);
use Text::CSV;
use List::MoreUtils qw(zip);
use Encode;

my $CRLF = "\015\012";

sub BUILD {
    my $self = shift;
    confess "We need a connection or some content"
      unless ( $self->has_connection or $self->has_content );
}

=head1 Webservice::InterMine::ResultIterator - An object for iterating through result rows

=head1 SYNOPSIS

  my $query = get_query_from_somewhere();
  my $iter = $query->result_iterator;
  while (defined (my $hashref = $iter->hashref)) {
    # do something with hashref
  }

=head1 DESCRIPTION

This package provides objects for iterating through result sets.

=head1 ATTRIBUTES

=cut

=head2 connection [ro]

A Net::HTTP connection to the source of results

=cut

has connection => (
    is        => 'ro',
    isa       => NetHTTP,
    trigger   => \&set_headers,
    predicate => 'has_connection',
);

=head2 content [ro]

A Glob with content. Can be a glob linked to any file-handle
or other IO.

=cut

has content => (
    is        => 'ro',
    isa       => GlobRef,
    predicate => 'has_content',
);

=head2 error_code [ro]

The error code from the request, if any.

=cut

has error_code => (
    is        => 'ro',
    isa       => HTTPCode,
    writer    => '_set_error_code',
    predicate => 'has_code',
);

=head2 error_message [ro]

The error message from the request, if any.

=cut

has error_message => (
    is      => 'ro',
    isa     => Str,
    default => '',
    writer  => '_set_error_message',
);

before qr/^error_/ => sub {
    my $self = shift;
    unless ( $self->has_code ) {
        $self->set_headers;
    }
};

=head2 csv [ro]

The CSV parser used to parse CSV/TSV lines.

=cut

has csv => (
    is   => 'ro',
    isa  => TextCSV,
    lazy => 1,
    default =>
      sub { Text::CSV->new( { binary => 1, sep_char => "\t", allow_loose_quotes => 1 } ) },
);

=head2 view_list [ro]

The list of paths in the view for the query that was run.

=cut

has view_list => (
    is       => 'ro',
    isa      => PathList,
    required => 1,
    trigger  => sub {
        my ( $self, $viewlist ) = @_;
        $self->csv->column_names(@$viewlist);
    },
);

has headers => (
    traits  => ['Hash'],
    is      => 'ro',
    isa     => HashRef,
    writer  => '_set_headers',
    handles => { get_header => 'get', },
    trigger => sub {
        my $self = shift;
        my $te   = $self->get_header('Transfer-Encoding');
        $self->_is_chunked(1)
          if ( $te and $te eq 'chunked' );
    },
);

has is_chunked => (
    is     => 'ro',
    isa    => Bool,
    writer => '_is_chunked',
);
has chunk_bytes_left => (
    traits  => ['Counter'],
    is      => 'rw',
    isa     => Num,
    lazy    => 1,
    default => 0,
    handles => { subtract_from_current_chunk => 'dec', },
);

has is_finished => (
    traits  => ['Bool'],
    is      => 'ro',
    isa     => Bool,
    default => 0,
    handles => { close => 'set', },
);

after close => sub {
    my $self = shift;
    $self->connection->close;
};

sub set_headers {
    my $self = shift;
    my %headers;
    my $i;
    while ( my $line = $self->string ) {
        my ( $version, $code, $phrase, $key, $value );
        if ( $line =~ /^HTTP/ ) {
            chomp( ( $version, $code, $phrase ) =
                  split( /\s/, $line, 3 ) );
        } else {
            chomp( ( $key, $value ) = split( /:\s*/, $line, 2 ) );
        }
        $headers{$key} = $value if $key;
        $self->_set_error_code($code)      if $code;
        $self->_set_error_message($phrase) if $phrase;
    }
    $self->_set_headers( \%headers );
}

######## ERROR CHECKING METHODS

=head1 METHODS 

=head2 [Bool] is_success

Returns true if the server responded with a success-y status.

=cut

sub is_success {
    my $self = shift;
    return HTTP::Status::is_success( $self->error_code );
}

=head2 [Bool] is_error

Returns true if the server responded with an error-ish status.

=cut

sub is_error {
    my $self = shift;
    return HTTP::Status::is_error( $self->error_code );
}

=head2 [Str] status_line

Returns a human readable status line.

=cut

sub status_line {
    my $self = shift;
    my $line = sprintf( "%s (%s): %s",
        status_message( $self->error_code ),
        $self->error_code, $self->error_message, );
    return $line;
}

####### FOR USE WITH SOCKETS

sub read_line {
    my $self = shift;
    if ( $self->has_content ) {
        return $self->content->getline;
    }
    return undef if $self->is_finished;
    if ( $self->is_chunked and $self->chunk_bytes_left < 1 ) {
        my $chunksize;
        until ( defined $chunksize and length $chunksize ) {
            $chunksize = $self->connection->getline;
            confess
              "Unexpected end of transmission - Transfer interrupted?"
              unless ( defined $chunksize );
            $chunksize =~ s/\015?\012//;
        }
        $self->chunk_bytes_left( hex($chunksize) );
        if ( $self->chunk_bytes_left == 0 ) {    # EOF
            $self->close;
            return undef;
        }
    }
    my $line = $self->connection->getline;
    if ( $self->is_chunked ) {
        if ( not defined $line ) {
            confess "Transfer interrupted"
              if ( $self->chunk_bytes_left != 0 );
        } else {
            $self->subtract_from_current_chunk( length($line) );
            if ( $self->chunk_bytes_left < 0 )
            {    # run on line, usually records a value of -2
                $line =~ s/\015?\012//;
                my $next_line = $self->read_line;
                $line .= $next_line if $next_line;
            }
        }
    }
    return $line;
}

########## USER ACCESS METHODS

=head2 [Str] string

returns the next line as a string.

=cut

sub string {
    my $self = shift;
    my $line = $self->read_line;
    if ( defined $line ) {
        $line =~ s/\015?\012//;
        $line = encode_utf8($line);
    }
    return $line;
}

=head2 [Str] arrayref

returns the next line as an arrayref.

=cut

sub arrayref {
    my $self = shift;
    my $line = $self->string or return;
    open( my $io, '<', \$line ) or die $!;
    return $self->csv->getline($io);
}

=head2 [Str] hashref

returns the next line as a hashref

=cut

sub hashref {
    my $self = shift;
    my $line = $self->string or return;
    open( my $io, '<', \$line ) or die $!;
    return $self->csv->getline_hr($io);
}

=head2 [List] all_lines( format )

Returns all the lines, in the desired format (string/arrayref/hashref).

=cut

sub all_lines {
    my ( $self, $wanted ) = @_;
    confess
"invalid row format, or none supplied for all_lines (string|arrayref|hashref) "
      unless $wanted;
    my @lines;
    while ( defined( my $line = $self->$wanted ) ) {
        push @lines, $line;
    }
    return @lines;
}

__PACKAGE__->meta->make_immutable;
no Moose;

1;
