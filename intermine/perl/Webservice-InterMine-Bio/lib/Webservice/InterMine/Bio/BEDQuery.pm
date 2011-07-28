package Webservice::InterMine::Bio::BEDQuery;

=head1 NAME

Webservice::InterMine::Bio::BEDQuery - BED specific query behaviour.

=head1 SYNOPSIS

This module provides access to BED results services for standard InterMine
queries. 

    use Webservice::InterMine::Bio qw/BED/;
    use Webservice::InterMine 'flymine';

    my $query = Webservice::InterMine->new_query(with => BED);
    $query->add_sequence_features("Gene", "Gene.exons", "Gene.transcripts");
    $query->add_constraint("Gene", "IN", "my_gene_list");

    $query->print_bed(to => "my_genes.bed");       # Print the results as BED

=head1 DESCRIPTION

This module extends InterMine queries with BED specific functionality.

=cut

use Moose::Role;
with 'Webservice::InterMine::Bio::SequenceFeatureQuery';

use Carp qw/confess carp/;

requires 'service', 'service_root', 'get_request_parameters', 'view', 'model';

=head2 ucsc_compatible

Whether or not to prefix "chr" to each chromosome. Defaults to true.

=cut

has ucsc_compatible => (
    is => 'rw', 
    isa => 'Bool', 
    default => 1,
);

=head2 track_description

A description of the track. If none is provided the webservice will generate one.

=cut

has track_description => (
    is => 'rw',
    isa => 'Str',
    predicate => 'has_track_description',
);

=head2 get_bed_iterator()

Return a results iterator for this query in GFF3 format.

=cut

sub get_bed_iterator {
    my $self = shift;
    return $self->get_seq_iterator('bed');
}

=head2 print_bed(to => $file)

Print the results of this query as GFF3 to a file, or to the screen if
no file is provided.

=cut

sub print_bed {
    my $self = shift;
    my %args = @_;
    return $self->print_seq(%args, format => 'bed');
}

=head2 get_bed()

Return the string representation of the results of this query in GFF3 format.

=cut

sub get_bed {
    my $self = shift;
    return $self->get_sequence('bed');
}

=head2 get_bed_uri()

Return the uri for the webservice to access the results of this query as fasta.

=cut

sub get_bed_uri {
    my $self = shift;
    return $self->get_sequence_uri('bed');
}

=head2 get_request_parameters

modified to add compatibility and track descriptions if required.

=cut

around get_request_parameters => sub {
    my $orig = shift;
    my $self = shift;
    my %params = $self->$orig;
    $params{ucscCompatible} = "no" unless $self->ucsc_compatible;
    $params{trackDescription} = $self->track_description if $self->has_track_description;
    return %params;
};

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


