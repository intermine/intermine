package Webservice::InterMine::Bio::RegionQuery;

=head1 NAME

Webservice::InterMine::Bio::RegionQuery - Genomic Interval based queries.

=cut

use strict;
use warnings;

use Moose;

use Webservice::InterMine::Types qw/Service/;
use MooseX::Types::Moose qw/Str ArrayRef Int Bool/;

=head1 SYNOPSIS

    my $service = Webservice::InterMine->get_service('flymine', 'API-KEY');
    my $region_query = Webservice::InterMine::Bio::RegionQuery->new(
        service => $service, 
        organism => "D. melanogaster", 
        regions => ["2L:14614843..14619614", "Foo",], # Foo will be ignored.
        feature_types => ["Exon", "Transcript"],
    );

    print "Sequence data...", "\n";
    print $region_query->bed;
    print $region_query->fasta;
    print $region_query->gff3;

    print "Making a list... (requires an API-KEY)", "\n";
    my $list = $service->new_list(content => $region_query);

=head1 DESCRIPTION

An abstraction of the API methods offered by biological mines for performing
genomic region based queries. These queries search for features of given types
overlapping a specified set of regions in a given organism. The features may be either 
retrieved in biological formats, or stored as a list of features on the originating
server. Creating lists will require the use of an API-Key.

=head1 ATTRIBUTES

=head2 service (Service required)

A reference to an InterMine webservice.

=cut

has service => (
    isa => Service,
    is => 'ro', 
    required => 1,
    handles => {
        model => "model",
        service_root => "root",
    },
);

=head2 organism (Str required) 

The short name (eg: "D. melanogaster") for the organism
these regions refer to.

=cut

has organism => (
    isa => Str,
    is => 'rw',
    required => 1,
);

=head2 feature_types (ArrayRef[Str] required) 

A list of feature types to search the regions for. These should 
all be valid names for classes that inherit from SequenceFeature.

=head2 regions (ArrayRef[Str] required) 

A list of regions to search for features within. These should
all be valid regions in either BED format, or dotted notation (eg:
"2L:14614843..14619614" or "2R\t5866034\t5868996"). Invalid regions will
be ignored.

=cut

has [qw/feature_types regions/] => (
    isa => ArrayRef[Str],
    is => 'rw', 
    required => 1,
);

=head2 extension (Int = 0)

A number of base-pairs to extend the regions on either side. Defaults to 0.

=cut

has extension => (
    isa => Int,
    is => 'rw',
    default => 0,
);

=head2 is_interbase (Bool = false)

Whether or not the regions should be interpreted as interbase co-ordinates. Defaults
to false.

=cut

has is_interbase => (
    isa => Bool,
    is => 'rw',
    default => 0,
);

=head1 METHODS

=cut

sub _get_query {
    my $self = shift;
    my $query = {
        organism => $self->organism,
        featureTypes => $self->feature_types,
        regions => $self->regions,
        extension => $self->extension,
        isInterbase => ($self->is_interbase ? "true" : "false"),
    };
    return $self->json->encode($query);
}

=head2 get_request_parameters (List)

Returns the request parameters that this region query represents. 
This method is a required part of the ListableQuery role.

=cut

sub get_request_parameters {
    my $self = shift;
    return (query => $self->_get_query());
}

=head2 list_upload_path (Str)

Returns the path to append to the base url to create a new list.
This method is a required part of the ListableQuery role.

=cut

sub list_upload_path {
    my $self = shift;
    return "/regions/list";
}

=head2 list_append_path (Str)

Returns the path to append to the base url to append elements to an existing list.
At present this is not implemented.
This method is a required part of the ListableQuery role.

=cut

sub list_append_path {
    my $self = shift;
    confess "Region appending not yet implemented";
}

sub _sequence_results {
    my $self = shift;
    my ($format) = @_;
    my $query = $self->_get_query();
    my @params = $self->service->build_params(query => $query);
    my $uri = $self->service->root . "/regions/" . $format;
    my $response = $self->service->post($uri, [@params]);
    if ($response->is_success) {
        return $response->decoded_content;
    } else {
        confess $response->status_line, $response->content;
    }
}

=head2 bed (Str)

Returns the sequence feature data as a string in BED format.

=cut

sub bed {
    my $self = shift;
    return $self->_sequence_results("bed");
}

=head2 fasta (Str)

Returns the sequence feature data as a string in FASTA format.

=cut

sub fasta {
    my $self = shift;
    return $self->_sequence_results("fasta");
}

=head2 gff3 (Str)

Returns the sequence feature data as a string in GFF3 format.

=cut

sub gff3 {
    my $self = shift;
    return $self->_sequence_results("gff3");
}

with 'Webservice::InterMine::Role::KnowsJSON';
with 'Webservice::InterMine::Query::Roles::Listable';

no Moose;
__PACKAGE__->meta->make_immutable();

1;

