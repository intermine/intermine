package Webservice::InterMine::Bio::GFFQuery;

=head1 NAME

Webservice::InterMine::Bio::GFFQuery - GFF3 specific query behaviour.

=head1 SYNOPSIS

This module provides access to GFF3 results services for standard InterMine
queries. 

    use Webservice::InterMine::Bio qw/GFF3/;
    use Webservice::InterMine 'flymine';

    my $query = Webservice::InterMine->new_query(with => GFF3);
    $query->add_sequence_features("Gene", "Gene.exons", "Gene.transcripts");
    $query->add_constraint("Gene", "IN", "my_gene_list");

    my $feature_store = $query->get_feature_store(); # Get a Bio::DB::SeqFeature::Store

    $query->print_gff3(to => "my_genes.gff3");       # Print the results as GFF3

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

use constant {
    _NO_BIODB => "Cannot create feature store - is Bio::DB::SeqFeature::Store (part of BioPerl) installed?",
};

=head1 METHODS

=head2 get_feature_store()

Return a Bio::DB::SeqFeature::Store object loaded with data from the query. This
method will first check for the availability of BioPerl functionality, but may
throw an exception if this is not available. See L<Bio::DB::SeqFeature::Store>.

=cut

sub get_feature_store {
    my $self = shift;
    eval 'require Bio::DB::SeqFeature::Store; 1;' or confess _NO_BIODB;
    my $temp = File::Temp->new(SUFFIX => '.gff3.gz')->filename;
    $self->print_gff3(to => $temp);
    open (my $gff, '<:gzip', $temp);
    return Bio::DB::SeqFeature::Store->new(-adaptor => 'memory', -dsn => $gff);
}

=head2 get_gff3_iterator()

Return a results iterator for this query in GFF3 format.

=cut

sub get_gff3_iterator {
    my $self = shift;
    return $self->get_seq_iterator('gff3');
}

=head2 print_gff3(to => $file)

Print the results of this query as GFF3 to a file, or to the screen if
no file is provided.

=cut

sub print_gff3 {
    my $self = shift;
    my %args = @_;
    return $self->print_seq(%args, format => 'gff3');
}

=head2 get_gff3()

Return the string representation of the results of this query in GFF3 format.

=cut

sub get_gff3 {
    my $self = shift;
    return $self->get_sequence('gff3');
}

=head2 get_gff3_uri()

Return the uri for the webservice to access the results of this query as fasta.

=cut

sub get_gff3_uri {
    my $self = shift;
    return $self->get_sequence_uri('gff3');
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

