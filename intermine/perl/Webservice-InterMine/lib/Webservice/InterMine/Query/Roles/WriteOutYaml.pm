package Webservice::InterMine::Query::Roles::WriteOutYaml;

use Moose::Role;
use YAML::Syck;

requires qw(results);

sub dump_yaml_to_file {
    my $self = shift;
    my %args = @_;
    my $file = $args{file} or confess "dump_to_file needs a file";
    delete $args{file};
    my $results = $self->results(%args);
    DumpFile( $file, $results );
}

1;
