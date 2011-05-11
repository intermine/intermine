package Webservice::InterMine::Bio;

use warnings;
use strict;

=head1 NAME

Webservice::InterMine::Bio - The great new Webservice::InterMine::Bio!

=head1 VERSION

Version 0.01

=cut

our $VERSION = '0.01';

use Exporter qw/import/;

our @EXPORT_OK = qw/GFF3 BIO_PERL/;

=head1 SYNOPSIS

Quick summary of what the module does.

Perhaps a little code snippet.

    use Webservice::InterMine::Bio qw/BIO_PERL/;
    use Webservice::InterMine 'flymine';

    my $query = Webservice::InterMine->new_query(with => BIO_PERL);
    ...

=head1 EXPORT

A list of functions that can be exported.  You can delete this section
if you don't export anything, such as for a purely object-oriented module.

=cut

use constant {
    GFF3 => ['Webservice::InterMine::Bio::GFFQuery'],
    BIO_PERL => ['Webservice::InterMine::Bio::GFFQuery'],
};

=head1 AUTHOR

Alex Kalderimis, C<< <dev at intermine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<bug-webservice-intermine-bio at rt.cpan.org>, or through
the web interface at L<http://rt.cpan.org/NoAuth/ReportBug.html?Queue=Webservice-InterMine-Bio>.  I will be notified, and then you'll
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


=head1 LICENSE AND COPYRIGHT

Copyright 2011 Alex Kalderimis.

This program is free software; you can redistribute it and/or modify it
under the terms of either: the GNU General Public License as published
by the Free Software Foundation; or the Artistic License.

See http://dev.perl.org/licenses/ for more information.


=cut

1; # End of Webservice::InterMine::Bio
