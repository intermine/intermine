use Dancer qw(:syntax);
use Plack::Builder;

use lib './lib';
load_app 'registry';

use Dancer::Config 'setting';
setting apphandler => 'PSGI';
Dancer::Config->load;

my $registry = sub {
    my $env = shift;
    my $request = Dancer::Request->new( $env );
    Dancer->dance( $request );
};

builder {
    mount "/registry" => builder {$registry};
};
