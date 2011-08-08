package Webservice::InterMine::Query::Roles::WriteOutYaml;

use Moose::Role;
use YAML::Syck qw(Dump);

requires qw(results);

sub results_to_yaml {
    my $self = shift;
    my %args = @_;
    $args{as} ||= "arrayrefs";
    my $results = $self->results(%args);
    return Dump($results);
}

1;
