package Webservice::InterMine::Bio::SequenceFeatureQuery;

=head1 NAME

Webservice::InterMine::Bio::SequenceFeatureQuery - Common behaviour for queries on sequence features.

=head1 SYNOPSIS

This module provides Sequence Feature specific behaviour for InterMine queries. 

    use Webservice::InterMine 'flymine';

    my $query = Webservice::InterMine->new_query(
        with => 'Webservice::InterMine::Bio::SequenceFeatureQuery'
    );

    $query->add_sequence_features("Gene", "Gene.exons", "Gene.transcripts");
    ...


=head1 DESCRIPTION

This module is used by GFF3 and Fasta queries to provide common sequence feature 
specific functionality. It is not really useful on its own.

=cut

use strict;
use warnings;

use Moose::Role;
requires 'model', 'add_view', 'where', 'view_is_empty', 'has_root_path';

use autodie qw(open);
use IO::Handle;
use Carp qw/confess carp/;
require URI;

use Webservice::InterMine::Path qw/type_of/;

=head1 METHODS

=head2 add_sequence_features(Path...)

Add sequence features to the output of the current query. 
All paths provided must be valid view paths, and evaluate to 
represent a SequenceFeature or a class that inherits from SequenceFeature.

  $query->add_sequence_features("Gene", "Gene.exon");

=cut

sub add_sequence_features {
    my $self = shift;
    my @features = @_;
    my $model = $self->model;
    my $seq_feature_cd = $model->get_classdescriptor_by_name('SequenceFeature');
    for my $f (@features) {
        confess "$f is not a sequence feature" unless (eval{
            $seq_feature_cd->superclass_of(type_of($model, $f))});
    }
    $self->add_view(map {$_ . '.primaryIdentifier'} @features);
}

=head2 get_seq_iterator($format)

Return a result iterator for a sequence feature query.

=cut

sub get_seq_iterator {
    my $self = shift;
    my $format = shift;
    my $url  = $self->_get_base_uri($format);
    return $self->service->get_results_iterator(
        $url,
        {$self->get_request_parameters},
        $self->view, 
        'tsv', 
        'perl',
        undef,
    );
}

=head2 get_sequence_uri($format)

Return the resource uri for a particular kind of sequence feature query.

=cut

sub get_sequence_uri {
    my ($self, $format) = @_;
    my $uri = URI->new($self->_get_base_uri($format));
    $uri->query_form($self->get_request_parameters);
    return "$uri";
}

# Memoize these lookups
my %_base_uris;

sub _get_base_uri {
    my ($self, $format) = @_;
    my $root = $self->service_root;
    if (my $uri = $_base_uris{"$root-$format"}) {
        return $uri;
    }
    my $uri = $root . $self->service->get_resource_path("query.$format");
    $_base_uris{"$root-$format"} = $uri;
    return $uri;
}

=head2 get_sequence($format)

Get a string representing the sequence in the format requested.

=cut

sub get_sequence {
    my ($self, $format) = @_;
    my $iterator = $self->get_seq_iterator($format);
    return join("\n", $iterator->get_all);
}

=head2 print_seq(format => $format, to => $fh, compress => $compress)

Prints the requested sequence query in the requested format to the requested 
location, or STDOUT if none is provided. If the location is a filename, and it ends in 
'gz', then compression will be requested, whether or not the parameter was present.

=cut

sub print_seq {
    my $self = shift;
    my %args = @_;
    my $out = $args{to};
    my $compress = $args{compress};
    my $format = $args{format};

    my $fh;
    if (ref $out eq 'GLOB' || blessed $out and $out->can('print')) {
        $fh = $out;
    } elsif (defined $out) {
        $compress = 'gzip' if ($out =~ /\.gz$/);
        open($fh, '>', $out);
    } else {
        open($fh, '>-');
    }

    if ($compress) {
        binmode $fh;
        my $uri = URI->new($self->service_root 
            . $self->service->get_resource_path('query.' . $format));
        $uri->query_form(compress => $compress, $self->get_request_parameters);
        my $response = $self->service->get($uri, ':content_cb' => sub {$fh->print($_[0])});
        confess $response->status_line if $response->is_error;
    } else {
        binmode $fh, ':encoding(utf8)'; 
        my $iterator = $self->get_seq_iterator($format);
        while (<$iterator>) {
            $fh->print($_, "\n");
        }
    }
}

before where => sub {
    my $self = shift;
    if ($self->view_is_empty and $self->has_root_path) {
        $self->add_view("id");
    }
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

