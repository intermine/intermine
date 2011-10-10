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
    template analysis => {things => "models", thing => "Class"};
};

get '/templates' => sub {
    template analysis => {things => "templates", thing => "Template"};
};

true;
