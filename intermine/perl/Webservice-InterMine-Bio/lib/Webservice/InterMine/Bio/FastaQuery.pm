package Webservice::InterMine::Bio::FastaQuery;

=head1 NAME

Webservice::InterMine::Bio::FastaQuery - FASTA specific query behaviour.

=head1 SYNOPSIS

This module provides access to FASTA results services for standard InterMine
queries. 

    use Webservice::InterMine::Bio qw/FASTA/;
    use Webservice::InterMine 'flymine';

    my $query = Webservice::InterMine->new_query(with => FASTA);
    $query->add_sequence_features("Gene");
    $query->add_constraint("Gene", "IN", "my_gene_list");

    my $feature_store = $query->get_seq_io(); # Get a Bio::SeqIO object representing the sequence

    $query->print_fasta(to => "my_genes.fa"); # Print the results as FASTA

=head1 DESCRIPTION

This module extends InterMine queries with GFF3 specific functionality.

=cut


use Moose::Role;
use namespace::autoclean;
with 'Webservice::InterMine::Bio::SequenceFeatureQuery';

use Carp qw/confess carp/;
use autodie qw(open);
require File::Temp;
use PerlIO::gzip;

requires 'service', 'service_root', 'get_request_parameters', 'view', 'model';

use constant _NO_SEQIO 
    => "Cannot create seq-io object - is Bio::SeqIO (part of BioPerl) installed?";

=head1 METHODS

=head2 get_seq_io()

Return a Bio::SeqIO object loaded with data from the query. This
method will first check for the availability of BioPerl functionality, but may
throw an exception if this is not available. See L<Bio::SeqIO>.

=cut

sub get_seq_io {
    my $self = shift;
    eval 'require Bio::SeqIO; 1' or confess _NO_SEQIO;
    my $temp = File::Temp->new(SUFFIX => '.fa.gz')->filename;
    $self->print_fasta(to => $temp);
    open (my $fa, '<:gzip', $temp);
    return Bio::SeqIO->new(-fh => $fa, -format => 'fasta');
}

=head2 get_fasta_iterator()

Return a results iterator for this query in FASTA format.

=cut

sub get_fasta_iterator {
    my $self = shift;
    return $self->get_seq_iterator('fasta');
}

=head2 get_fasta()

Return the string representation of the results of this query in FASTA format.

=cut

sub get_fasta {
    my $self = shift;
    return $self->get_sequence('fasta');
}

=head2 print_fasta(to => $file)

Print the results of this query in FASTA format to the file requested.

=cut

sub print_fasta {
    my $self = shift;
    my %args = @_;
    return $self->print_seq(%args, format => 'fasta');
}

=head2 get_fasta_uri()

Get the uri of the webservice that provides results in FASTA format.

=cut

sub get_fasta_uri {
    my $self = shift;
    return $self->get_sequence_uri('fasta');
}

1;

=head1 AUTHOR

Alex Kalderimis, C<< <dev at intermine.org> >>

=head1 BUGS

Please report any bugs or feature requests 
to C<bug-webservice-intermine-bio at rt.cpan.org>, or through
the web interface at L<http://rt.cpan.org/NoAuth/ReportBug.html?Queue=Webservice-InterMine-Bio>.  
I will be notified, and then you'll
automatically be notified of progress on your bug as I make changes.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine::Bio

You can also look for information at:

=over 4

=item * RT: CPAN's request tracker

L<http://rt.cpan.org/NoAuth/Bugs.html?Dist=Webservice-InterMine-Bio>

=item * AnnoCPAN: Annotated CPAN documentation

L<http://annocpan.org/dist/Webservice-InterMine-Bio>

=item * CPAN Ratings

L<http://cpanratings.perl.org/d/Webservice-InterMine-Bio>

=item * Search CPAN

L<http://search.cpan.org/dist/Webservice-InterMine-Bio/>

=back

=head1 ACKNOWLEDGEMENTS

The funding bodies that support InterMine:

=over 2

=item * The Wellcome Trust L<http://www.wellcome.ac.uk/>

=item * The NIH/NHGRI L<http://www.nih.gov/>

=back

=head1 LICENSE AND COPYRIGHT

Copyright 2011 InterMine

This program is free software; you can redistribute it and/or modify it
under the terms of either: the GNU General Public License as published
by the Free Software Foundation; or the Artistic License.

See http://dev.perl.org/licenses/ for more information.

