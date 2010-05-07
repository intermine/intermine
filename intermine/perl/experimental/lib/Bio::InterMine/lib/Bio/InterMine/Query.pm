package Bio::InterMine::Query;

use warnings;
use strict;
use Moose;

=head1 NAME

Bio::InterMine::Query - Query InterMine webservices perlishly

=head1 VERSION

Version 0.01

=cut

our $VERSION = '0.01';


=head1 SYNOPSIS

FlyMine and modMine are two examples of Genomics datastores powered by InterMine.
This module allows you to Query an InterMine webservice in a modern, object-orientated, perlish way.


    use Bio::InterMine::Query;

    my $query   = Bio::InterMine::Query->new();
    $query->url = $service_url; 
    my @views   = $query->add_view("Gene.primaryIdentifier Gene.symbol Gene.organism.name");
    my ($path, $op, $value, $extra_value) = ('Gene', 'LOOKUP', 'eve', 'D. Melanogaster'); 
    my $cons    = $query->add_constraint($path, $op, $value, $extra_value);
    my ($err, $results) = $query->get_results;
    
    my $i = 0;
    
    unless ($err) {
        for my $result (@$results) {
          print "Result $i:\n";
          print "Primary Identifier $result{Gene.primaryIdentifier}";
          print "Symbol             $result{Gene.symbol}";
          print "Organism name      $result{Gene.organism.name}";
        }
    }

An alternative functional interface is provided for queries using xml strings from the
online query builder. 

    use Bio::InterMine::Query qw(get_result, get_model);
   
    my ($err, $results) = get_results($url, $xmlstring);

The data model used by the InterMine service can also be fetched

    my ($err, %model)         = get_model($url);
    my ($err, $model_as_xml)  = get_model_as_xml($url);

=head1 EXPORT

    get_results
    get_model

=head1 SUBROUTINES/METHODS

=head2 function1

=cut

sub function1 {
}

=head2 function2

=cut

sub function2 {
}

=head1 AUTHOR

InterMine, C<< <dev at flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<bug-bio::intermine at rt.cpan.org>, or through
the web interface at L<http://rt.cpan.org/NoAuth/ReportBug.html?Queue=Bio::InterMine>.  I will be notified, and then you'll
automatically be notified of progress on your bug as I make changes.




=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Bio::InterMine::Query


You can also look for information at:

=over 4

=item * RT: CPAN's request tracker

L<http://rt.cpan.org/NoAuth/Bugs.html?Dist=Bio::InterMine>

=item * AnnoCPAN: Annotated CPAN documentation

L<http://annocpan.org/dist/Bio::InterMine>

=item * CPAN Ratings

L<http://cpanratings.perl.org/d/Bio::InterMine>

=item * Search CPAN

L<http://search.cpan.org/dist/Bio::InterMine/>

=back


=head1 ACKNOWLEDGEMENTS


=head1 LICENSE AND COPYRIGHT

Copyright 2010 InterMine.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this program; if not, write to the Free
Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.


=cut

1; # End of Bio::InterMine::Query
