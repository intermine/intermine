package Registry;

use perl5i::2;
use Dancer ':syntax';
use DateTime;
use DateTime::Format::ISO8601;
use Dancer::Plugin::ProxyPath;
use Dancer::Plugin::FormattedOutput;

use Registry::Model qw(:all);

func handle_response($response) {
    debug("Setting status to " . $response->{status} );
    status( $response->{status} );
    debug("Status is " . status());
    return format_output($response);
}

use Dancer::Cookies;

my $iso_8601_parser = DateTime::Format::ISO8601->new;

before_template sub {
    my $tokens = shift;
    $tokens->{dtParser} = $iso_8601_parser;
    $tokens->{now} = DateTime->now;
    $tokens->{howLongAgo} = sub {
        my $when = $iso_8601_parser->parse_datetime(shift);
        my $days = DateTime->now->delta_days($when);
        debug(to_dumper($days->deltas));
        return $days->days ? $days->days . " days ago" : "today";
    };
};

get '/login' => sub {
    template login => {path => vars->{requested_path}, failed => params->{failed}};
};

post '/login' => sub {
    my $user = get_administrator(params->{user});
    if (not $user) {
        warning "Failed login for unrecognised user " . (params->{user} || '');
        redirect proxy->uri_for("/login", {failed => 1});
    } else {
        if (validate_user($user, params->{password})) {
            debug "Successful login by " . $user->{name};
            session user => $user->{name};
            debug("Session set to : " . session('user'));
            debug("The user wants to go to: " . params->{path}) if params->{path};
            debug(to_dumper(Dancer::Cookies->cookies->{'dancer.session'}));
            redirect params->{path} ? proxy->uri_for(params->{path}) : proxy->uri_for('/');
        } else {
            warning "Failed login for unvalidated user " . $user->{name};
            redirect proxy->uri_for("/login", {failed => 1});
        }
    }
};

get '/mines:format?' => sub {
    my $data = { mines => [get_minelist()] };
    return format_output( mines => $data, "html" );
};

get '/mines/admin:format?' => sub {
    if ((! params->{format} || params->{format} =~ /html/i)  && ! session('user')) {
        var requested_path => request->path_info;
        request->path_info('/login');
        forward '/login';
    }

    my $data = { mines => [get_minelist()], logs => get_logs() };
    return format_output( mines_admin => $data, "html" );
};

func can_administer($mine) {
    my $administrators = get_admins;
    return true if (session('user') and $administrators->{session('user')});
    my $secrets = get_secrets;
    my $secret_key = params->{authToken};
    return $secret_key and $secret_key eq $secrets->{$mine};
}

post '/register' => sub {
    no warnings 'uninitialized';
    my $mines = get_minehash;
    my $name  = params->{name};
    my $storage_name = lc($name);
    my $response =
      $name
      ? { mineName => $name }
      : { status => 'bad_request', text => 'No name provided in request' };
    return handle_response($response) unless $name;

    my $mine_is_new = $mines->{$storage_name} ? false : true;
    my $entry = $mines->{$storage_name} || { name => $name };

    if ($mine_is_new) {
        my $secrets = get_secrets;
        $secrets->{$storage_name} = params->{authToken} || generate_secret();
        update_secrets($secrets);
        $response->{authToken} = $secrets->{$storage_name};
    } else {
        unless (can_administer($storage_name)) {
            $response->{status} = 'forbidden';
            $response->{text} = sprintf( setting("forbidden_message"), $name );
        }
    }

    unless ( $response->{status} ) {
        $entry->{homeUrl} = params->{url}
          if params->{url};
        $entry->{description} = params->{description}
          if params->{description};
        my $webservice = get_webservice( $entry->{homeUrl} );
        $entry->{webServiceRoot} = "$webservice" if $webservice;
        $entry->{organisms}      = get_organisms("$webservice");
        $entry->{dataSources}    = [ split( /,\s*/, params->{dataSources} ) ]
          if params->{dataSources};
        $entry->{primaryOrganism} = params->{primaryOrganism}
          if params->{primaryOrganism};
        $entry->{version} = get_version("$webservice");
        $entry->{logo}    = get_logo("$webservice");

        if ( $entry->{webServiceRoot} and $entry->{version} ) {
            $mines->{lc($name)} = $entry;
            update_minelist($mines);

            my $logs = get_logs();
            my $now = DateTime->now->datetime;
            my $log = $logs->{$storage_name} || {createdOn => $now, allUpdates => []};
            $log->{mostRecentUpdate} = $now;
            $log->{allUpdates}->push($now);
            $log->{versionAtUpdate}{$now} = $entry->{version};
            $logs->{$storage_name} = $log;
            update_logs($logs);

            $response->{entry} = $entry;
            if ($mine_is_new) {
                $response->{status} = 'created';
                $response->{text}   = sprintf( setting("creation_message"),
                    $name, get_secrets->{name} );
            }
            else {
                $response->{status} = 'ok';
                $response->{text} = sprintf( setting("update_message"), $name );
            }
        }
        else {
            $response->{status} = 'bad_request';
            $response->{text}   = sprintf(
                setting("validation_error_message"),
                params->{webservice} || params->{url} || "NO VALUE"
            );
        }
    }
    return handle_response($response);
};

del '/:name' => sub {
    my $mines    = get_minehash;
    my $name     = params->{name};
    my $storage_name = lc($name);
    my $entry    = delete( $mines->{$storage_name} );
    my $response = { mineName => $name };
    unless ($entry) {
        $response->{status} = "not_found";
        $response->{text}   = "The requested resource is not in this registry";
        return handle_response($response);
    }
    unless (can_administer($storage_name)) {
        $response->{status} = "forbidden";
        $response->{text} =
          "$name not updated: you do not have sufficient permissions";
        return handle_response($response);
    }
    update_minelist($mines);
    $response->{status} = "ok",
    $response->{text} = "$name deleted from registry";
    return handle_response($response);
};
