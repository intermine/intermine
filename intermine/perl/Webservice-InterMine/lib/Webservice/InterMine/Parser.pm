package Webservice::InterMine::Parser;

use Moose::Role;

=head1 Webservice::InterMine::Parser 

Defines the common parser interface.

=head1 REQUIRED METHODS

The following methods must be provided by consumers:

=over 4

=item * header_is_parsed()
report whether the header has been parsed yet.

=item * parse_header($line) 
Parse a line from the connection as header information. 
This method may be called multiple times, depending on 
the return value of C<header_is_parsed>.

=item * parse_line($line)
Given a line of text, return whatever the parser returns.

=back

=cut

requires qw/header_is_parsed parse_header parse_line is_complete/;

no Moose;
1;
