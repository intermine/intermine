use Dancer ':syntax';
use Dancer::App;
use Plack::Builder;
use Cwd;

setting apphandler => 'PSGI';

my $appdir = cwd();
warn("Running in $appdir\n");

my $view_dir = $appdir . '/views';

sub make_app {
    my $config = shift;
    return sub {
        my $env = shift;
        set appdir      => $appdir;
        set appname     => 'mineview-' . $config;
        set environment => $config;
        set views       => $view_dir;
        set public      => 'public';

        load_app "MineViewer";
        Dancer::Config->load;
        Dancer::Handler->init_request_headers($env);
        my $request = Dancer::Request->new(env => $env);
        Dancer->dance($request);
    };
}
my $mineview    = make_app('testing');
my $flymineview = make_app('flymineview');
my $neurogenes  = make_app('neurogenes');

builder {
    mount "/mineview"    => builder { $mineview };
    mount "/neurogenes"  => builder { $neurogenes };
    mount "/flymineview" => builder { $flymineview };
};
