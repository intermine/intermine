package WebReports;
use Dancer::Plugin::ProxyPath;
use Dancer ':syntax';
use perl5i::2;
use Webservice::InterMine;

our $VERSION = '0.1';

get '/' => sub {
    template 'index';
};

get '/models' => sub {
    template 'models';
};

get '/model-data/:mine' => sub {
    my $mines = setting("mines");
    my $service = get_service($mines->{param("mine")});
    template 'model-data' => {service => $service}, {layout => undef};
};

true;
