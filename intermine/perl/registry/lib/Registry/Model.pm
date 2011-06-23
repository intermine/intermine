package Registry::Model;

use Crypt::SaltedHash;
use Dancer ':syntax';
use DateTime;
use Exporter 'import';
use File::Copy qw(move);
use File::Slurp qw(slurp);
use perl5i::2;
use Webservice::InterMine;
use YAML qw(DumpFile);

our @EXPORT_OK = qw/
    generate_secret
    get_minelist 
    get_minehash
    get_secrets
    get_logs
    get_administrator
    get_admins
    get_webservice
    get_logo
    get_organisms
    get_version
    get_error_log
    update_minelist
    update_data
    update_secrets
    update_logs
    update_admins
    update_error_log
    validate_user
  /;

our %EXPORT_TAGS = ('all' => [@EXPORT_OK]);

our $VERSION = '0.1';

use constant {
    SEC_LENGTH        => 10,
    BACKUP            => ".bak",
    ORGANISM_TEMPLATE => "im_available_organisms",
};

my @chars = ( 'a' .. 'z', 'A' .. 'Z', 0 .. 9, '_' );

func validate_user($user, $password) {
    @_ == 2 || warning("Wrong args passed to validate_user");
    return Crypt::SaltedHash->validate($user->{password}, $password);
}

sub generate_secret {
    my $random_string;
    for ( 1 .. SEC_LENGTH ) {
        $random_string .= $chars[ rand @chars ];
    }
    return $random_string;
}

sub get_minelist {
    my $mines = get_minehash();
    return map { $mines->{$_} } sort keys %$mines;
}

sub get_minehash {
    return get_data("mines_file");
}

sub get_secrets {
    return get_data("secrets_file");
}

sub get_logs {
    return get_data("mine_logs");
}

func get_administrator($name) {
    my $admins = get_admins();
    return $admins->{$name};
}

sub get_admins {
    return get_data("administrators");
}

sub get_error_log {
    return get_data( "error_log" );
}

func get_data($settings_key) {
    my $file = setting($settings_key);
    my $content = try{slurp( $file )};
    my $data = try{ from_yaml($content) } || {};
    return $data;
}

sub update_minelist {
    update_data( "mines_file", @_ );
}

sub update_secrets {
    update_data( "secrets_file", @_ );
}

sub update_logs {
    update_data( "mine_logs", @_ );
}

sub update_admins {
    update_data( "administrators", @_ );
}

sub update_error_log {
    update_data( "error_log", @_ );
}

func update_data($settings_key, $new_data) {
    my $file = setting($settings_key);
    move( $file, $file . BACKUP );
    DumpFile( $file, $new_data );
}

get '/' => sub {
    redirect request->uri_for('/mines');
};

func get_version($webservice_root) {
    return try {
        params->{version}
          || Webservice::InterMine->get_service($webservice_root)->release;
    };
}

func get_webservice($webservice_root) {
    return try {
        Webservice::InterMine->get_service( params->{webservice}
              || $webservice_root )->root->canonical;
    };
}

func get_logo($webservice_root) {
    return try {
        params->{logo} || dirname($webservice_root) . "/model/images/logo.png";
    };
}

func get_organisms($webservice_root) {
    debug("Getting organisms");
    return try {
        params()->{organisms}
          ? [ split( /,\s*/, params()->{organisms} ) ]
          : Webservice::InterMine
                ->get_service($webservice_root)
                ->template(ORGANISM_TEMPLATE)
                ->results()
                ->map( func($row) { join('', @$row) } );
    };
}

