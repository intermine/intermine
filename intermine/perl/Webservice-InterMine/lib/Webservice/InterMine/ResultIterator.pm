package Webservice::InterMine::ResultIterator;

use strict;
use Moose;
use Carp qw/croak confess/;

use InterMine::Model::Types qw(PathList);
use Webservice::InterMine::Types qw(Uri HTTPCode NetHTTP RowFormat JsonFormat RequestFormat RowParser);
use MooseX::Types::Moose qw(Str HashRef Bool Num GlobRef Maybe);

use HTTP::Status qw(status_message);
use Encode;
use URI::Escape;
use overload (
    '<>' => 'next',
    fallback => 1,
);

my $CRLF = qr{\015?\012};

sub BUILD {
    my $self = shift;
    unless ($self->has_content) {
        warn "CONNECTING" if $ENV{DEBUG};
        $self->connect();
        warn "SETTING HEADERS" if $ENV{DEBUG};
        $self->set_headers();
    }
}

=head1 NAME

Webservice::InterMine::ResultIterator - An object for iterating through result rows

=head1 SYNOPSIS

  my $results = $query->result_iterator;
  while (<$results>) {
    # do something with $_
  }

=head1 DESCRIPTION

This package provides objects for iterating through result sets, where 
those result sets are rows from a database query.

=head1 CONSTRUCTION ARGUMENTS

The following arguments are required when constructing a new ResultIterator.

=over 4

=item * url: ro, URI|Str

The url of the resource to request. EG:

  http://some.mine.org/path/to/resource

=cut

has url => (
    is => 'ro',
    isa => Uri,
    coerce => 1,
    required => 1,
);

=item * parameters: ro, HashRef[Str], default: {}

The parameters for this request. 

=cut

has parameters => (
    is => 'ro',
    isa => HashRef[Str],
    default => sub { {} },
);

=item * authorization: ro, Str

A base 64 encoded string to use as the authorization header for 
basic authentication.

=cut

has authorization => (
    is => 'ro',
    isa => Maybe[Str],
);

=item * request_format: ro, RequestFormat

The format that will be actually requested from the server.

=cut

has request_format => (
    is => 'ro',
    isa => RequestFormat,
    required => 1,
    coerce => 1,
);

=item * row_parser: ro, Webservice::InterMine::Parser

The parser to return a formatted row of data.

=cut

has row_parser => (
    is => 'ro',
    isa => RowParser,
    required => 1,
);

=back

=head1 ATTRIBUTES

Other properties of the object. These attributes are derived from the 
original construction arguments.

=over 4

=item * user_agent: ro, Str,

A label to use to identify this request to the server. Defaults to 
"Webservice::InterMine-${version}"

=cut

has user_agent => (
    is => 'ro',
    isa => Str,
    lazy_build => 1,
);

sub _build_user_agent {
    require Webservice::InterMine;
    return "Webservice::InterMine-" . $Webservice::InterMine::VERSION . "/Perl client library";
}

=item * connection: ro, Net::HTTP

A connection to the source of results

=cut

has connection => (
    is        => 'ro',
    isa       => NetHTTP,
    writer    => 'set_connection',
    predicate => 'has_connection',
);

=item * content: ro, Glob

A Glob with content. Can be a glob linked to any file-handle
or other IO. This can be used to supply results for testing.

=cut

has content => (
    is        => 'ro',
    isa       => GlobRef,
    predicate => 'has_content',
    writer    => 'set_content',
);

=item * error_code: ro, HTTP Status Code

The status code of the request.
Once the request has been made and the headers parsed, the error
code is available to be read.

=cut

has error_code => (
    is        => 'ro',
    isa       => HTTPCode,
    writer    => '_set_error_code',
    predicate => 'has_code',
);

=item * error_message: ro, Str

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

=item * headers: ro, HashRef

The headers from the request.

=cut

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
        my $ct = $self->get_content_type;
        $self->_is_binary(1)
            if ($ct and $ct =~ /octet-stream/);
    },
);

sub get_content_type {
    my $self = shift;
    return $self->get_header('Content-Type');
}

=item * content_is_binary: ro, Bool

Whether or not the content should be treated as binary data,
for example if it is gzipped.

=cut

has content_is_binary => (
    is => 'ro',
    isa => 'Bool',
    writer => '_is_binary',
);

=item * is_chunked: ro, Bool

Whether or not the results are returned in chunked
transfer encoding.

=cut

has is_chunked => (
    is     => 'ro',
    isa    => Bool,
    writer => '_is_chunked',
);

=item * chunk_bytes_left: rw, Num

A number referring to the bytes left in the current chunk.

=cut

has chunk_bytes_left => (
    traits  => ['Counter'],
    is      => 'rw',
    isa     => Num,
    lazy    => 1,
    default => 0,
    handles => { subtract_from_current_chunk => 'dec', },
);

=item * is_finished: ro, Bool

Whether or not all the available results have been read.

=cut

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
    while ( my $line = $self->read_line ) {
        my ( $version, $code, $phrase, $key, $value );
        if ( $line =~ /^HTTP/ ) {
            chomp( ( $version, $code, $phrase ) =
                  split( /\s/, $line, 3 ) );
        } elsif ($line =~ /:/) {
            chomp( ( $key, $value ) = split( /:\s*/, $line, 2 ) );
        } 
        $headers{$key} = $value if $key;
        warn "HEADER $key = $value" if ($key and $ENV{DEBUG});
        $self->_set_error_code($code)      if $code;
        $self->_set_error_message($phrase) if $phrase;
    }
    warn "FINISHED READING HEADERS" if $ENV{DEBUG};
    $self->_set_headers( \%headers );
    if (HTTP::Status::is_error( $self->error_code )) {
        $self->_find_real_error_message();
    }
}

sub _find_real_error_message {
    my $self = shift;
    eval {$self->get_all};
    if (my $e = $@) {
        $self->_set_error_message($e);
    }
}

=back

######## ERROR CHECKING METHODS

=head1 METHODS - ERROR CHECKING 

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

=head2 METHODS - RESULTS HANDLING (EXTERNAL API)

=head2 next

returns the next row in the appropriate format

=cut

sub next {
    my $self = shift;
    until ($self->row_parser->header_is_parsed) {
        my $line = $self->read_line;
        last unless (defined $line);
        $self->row_parser->parse_header($line);
    }
    my $next = $self->row_parser->parse_line($self->read_line);
    unless (defined $next) {
        if (my $footer = $self->read_line) {
            warn "PARSING FOOTER" if $ENV{DEBUG};
            do {$self->row_parser->parse_line($footer)} while ($footer = $self->read_line);
        }
    }
    return $next;
}

=head2 get_all

Return all rows from the result set.

Returns a list of rows in list context, or an arrayref of rows in scalar context.

=cut

sub get_all {
    my $self = shift;
    my @rows;
    while (defined(my $line = $self->next)) {
        push @rows, $line;
    }
    croak "Incomplete result set received" unless $self->row_parser->is_complete;
    if (wantarray) {
        return @rows;
    } else {
        return \@rows;
    }
}

=head1 METHODS - RESULTS HANDLING (INTERNAL)

=head2 [Str] read_line

Read the next line from either the content blob, or the open connection,
in the correct encoding, with the new line characters stripped.

=cut

sub read_line {
    my $self = shift;
    my $line;
    if ( $self->has_content ) {
        $line = $self->content->getline;
    } else {
        return undef if $self->is_finished;
        if ( $self->is_chunked and $self->chunk_bytes_left < 1 ) {
            my $chunksize;
            until ( defined $chunksize and length $chunksize ) {
                $chunksize = $self->connection->getline;
                confess
                "Unexpected end of transmission - Transfer interrupted?"
                unless ( defined $chunksize );
                $chunksize =~ s/$CRLF//;
            }
            $self->chunk_bytes_left( hex($chunksize) );
            if ( $self->chunk_bytes_left == 0 ) {    # EOF
                $self->close;
                return undef;
            }
        }
        $line = $self->connection->getline;
        if ( $self->is_chunked ) {
            if ( not defined $line ) {
                confess "Transfer interrupted"
                    if ( $self->chunk_bytes_left != 0 );
            } else {
                $self->subtract_from_current_chunk( length($line) );
                if ( $self->chunk_bytes_left < 0 )
                {    # run on line, usually records a value of -2
                    $line = trim_and_decode($line) 
                        unless $self->content_is_binary;
                    my $next_line = $self->read_line;
                    $line .= $next_line if $next_line;
                }
            }
        }
    }

    if ( defined $line ) {
        $line = trim_and_decode($line);
    }
    warn "RETURNING LINE: " . ($line || "") if $ENV{DEBUG};

    return $line;
}

sub trim_and_decode {
    my $string = shift;
    $string = decode_utf8($string);
    $string =~ s/$CRLF//;
    return $string;
}

=head2 connect

Connect to the resource specified by the url construction argument.

=cut

sub connect {
    my $self = shift;
    my $uri = $self->url;
    my $query_form = $self->parameters;

    $query_form->{format} = $self->request_format;
    
    my $connection = Net::HTTP->new( Host => $uri->host, PeerPort => $uri->port )
        or confess "Could not connect to host: $uri $@";
    my %headers = (
        'User-Agent' => $self->user_agent,
        'Content-Type' => 'application/x-www-form-urlencoded'
    );


    my $method = "POST";
    if (my $auth = $self->authorization) {
        $method = "GET"; # I do not know why...
        $headers{Authorization} = $auth;
        $uri->query_form($query_form);
    }

    my @pairs;
    while (my @pair = each %$query_form) {
        warn join("=", @pair), "\n" if $ENV{DEBUG};
        push @pairs, join('=', map {uri_escape($_)} @pair);
    }
    my $content = join('&', @pairs);
    warn $connection->format_request($method => "$uri", %headers, $content), "\n"
        if $ENV{DEBUG};
    $connection->write_request(POST => "$uri", %headers, $content)
        or die "Unable to write request";
    $self->set_connection($connection);
}

=head1 OVERLOADING

The following operators are overloaded:

=over 4

=item Iteration: <>

Return the next row of data.

=back

=cut

__PACKAGE__->meta->make_immutable;
no Moose;

1;

=head1 SEE ALSO

=over 4

=item * L<Webservice::InterMine::Cookbook> for guide on how to use these modules.

=item * L<Webservice::InterMine::Query>

=item * L<Webservice::InterMine::Service>

=item * L<Webservice::InterMine::Query::Template>

=item * L<Webservice::InterMine::Query::Saved>

=back

=head1 AUTHOR

Alex Kalderimis C<< <dev@intermine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine::ResultIterator

You can also look for information at:

=over 4

=item * Webservice::InterMine

L<http://www.intermine.org>

=item * Documentation

L<http://www.intermine.org/perlapi>

=back

=head1 COPYRIGHT AND LICENSE

Copyright 2006 - 2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

