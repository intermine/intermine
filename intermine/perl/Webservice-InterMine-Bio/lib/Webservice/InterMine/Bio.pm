package Webservice::InterMine::Bio;

use warnings;
use strict;

=head1 NAME

Webservice::InterMine::Bio - Access data from InterMine queries in biological formats.

=head1 VERSION

Version 0.9802

=cut

our $VERSION = '0.9802';


=head1 SYNOPSIS

This module provides roles that can be used to access data in standard biological formats,
and even interface directly with BioPerl classes.

    use Webservice::InterMine::Bio qw/GFF3/;
    use Webservice::InterMine 'flymine';

    my $query = Webservice::InterMine->new_query(with => GFF3);
    $query->add_sequence_features("Gene", "Gene.exons", "Gene.transcripts");
    my $feature_store = $query->get_feature_store(); # Get a Bio::DB::SeqFeature::Store
    ...

=head1 EXPORT

This module exports (optionally) constants that simplify references to the 
roles in this package, by providing means to name these roles in the 
manner expected by C<new_query>. So:

=over 2

=item * GFF3 => ['Webservice::InterMine::Bio::GFFQuery']

=item * FASTA => ['Webservice::InterMine::Bio::FastaQuery']

=item * BED => ['Webservice::InterMine::Bio::BEDQuery']

=back

=cut

use Exporter qw/import/;

our @EXPORT_OK = qw/GFF3 BIO_PERL FASTA BED/;

use constant {
    GFF3 => ['Webservice::InterMine::Bio::GFFQuery'],
    FASTA => ['Webservice::InterMine::Bio::FastaQuery'],
    BED => ['Webservice::InterMine::Bio::BEDQuery'],
};

=head1 AUTHOR

Alex Kalderimis, C<< <dev at intermine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<bug-webservice-intermine-bio at rt.cpan.org>, 
or through the web interface at 
L<http://rt.cpan.org/NoAuth/ReportBug.html?Queue=Webservice-InterMine-Bio>.  
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

Copyright 2011 Alex Kalderimis.

This program is free software; you can redistribute it and/or modify it
under the terms of either: the GNU General Public License as published
by the Free Software Foundation; or the Artistic License.

See http://dev.perl.org/licenses/ for more information.


=cut

1; # End of Webservice::InterMine::Bio
