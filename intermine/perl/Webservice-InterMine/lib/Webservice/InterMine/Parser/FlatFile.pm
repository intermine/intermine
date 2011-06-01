package Webservice::InterMine::Parser::FlatFile;

use Moose;
with 'Webservice::InterMine::Parser';

=head2 header_is_parsed, is_complete

Always returns true - as flat-file results do not have 
a special header section, not a footer to determine completeness.

=head2 parse_header 

No-op stub. 

=head2 parse_line

Returns the line given, checking that it does not contain a
report of an error.

=cut

sub header_is_parsed {1}
 
sub is_complete {1};

sub parse_header {}

sub parse_line {
    my $self = shift;
    my $line = shift;
    if ($line and $line =~ /^\[ERROR/) {
        confess "Results returned error:", $line;
    } else{
        return $line;
    }
}

no Moose;
__PACKAGE__->meta->make_immutable;
1;
