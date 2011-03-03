package Webservice::InterMine::Query::Roles::QueryUrl;

use Moose::Role;

requires qw(to_xml to_legacy_xml service query_path);

sub url {
    my $self = shift;
    my %args = @_;
    my $xml = ( $self->service->version < 2 ) ? $self->to_legacy_xml : $self->to_query_xml;
    my %query_form = (
        format => ($args{format} || "tab"),
        query  => $xml,
    );
    for my $opt (qw/start size/) {
        $query_form{$opt} = $args{$opt} if ($args{$opt});
    }

    my $url        = $self->service->root . $self->query_path;
    my $uri        = URI->new($url);
    $uri->query_form(%query_form);
    return $uri;
}

1;
