package Test::Webservice::InterMine::Service;

#TODO - add tests for apply role, and new_query

use base 'Test::Class';
use Test::More;
use Test::Exception;
use Test::MockObject;
use InterMine::Model;

sub class {'Webservice::InterMine::Service'}
sub fake_queryurl {'fake.url'}
sub fake_viewlist {[qw/one two three/]}
sub user_agent {'WebserviceInterMinePerlAPIClient'}
sub object {
    my $test = shift;
    return $test->{object};
};

sub setup :Test(setup) {
    my $test = shift;

    # Set up all the mock stuff


    my $model = InterMine::Model->new(file => 't/data/testmodel_model.xml');
    $test->{model} = $model;

    my $fake_query = Test::MockObject->new;
    $fake_query->fake_module(
	'Webservice::InterMine::Query',
	new => sub {},
    );
    $test->{fq} = $fake_query;
    my $connection = Test::MockObject->new;
    $connection->fake_module(
    	'Net::HTTP',
    	new => sub {
    	    my $class = shift;
    	    my @args  = @_;
    	    $connection->{_init_args} = {@args};
    	    return $connection;
    	},
    );
    $connection->mock(
	write_request => sub {
	    my $self = shift;
	    my @args  = @_;
	    $self->{_write_args} = {@args};
	    return $self;
	},
    );
    $test->{connection} = $connection;

    my $uri = Test::MockObject->new;
    $uri->fake_module(
	'URI',
	new => sub {
	    my $class = shift;
	    my $url = shift;
	    $uri->{_url} = $url;
	    return $uri;
	},
    );
    $uri->set_isa('URI');
    $uri->mock(host => sub{'URI-HOST'});
    $uri->mock(query_form => sub{});
    $test->{uri} = $uri;

    my $fakeRes = Test::MockObject->new;
    $fakeRes->fake_module(
	'Webservice::InterMine::ResultIterator',
	new => sub {
	    my $class = shift;
	    my @args  = @_;
	    $fakeRes->{_init_args} = {@args};
	    return $fakeRes;
	},
    );
    $fakeRes->mock(status_line => sub {"Hello, I'm a status line"})
	    ->mock(
		content => sub{
		    my $self = shift;
		    return $self->{_content}
		},
	    );
    $fakeRes->set_false('is_error')
            ->mock(code => sub {'FAKE_CODE'})
            ->mock(message => sub {'FAKE_MESSAGE'});
    $test->{Res} = $fakeRes;

    my $fakeLWP = Test::MockObject->new();
    $fakeLWP->fake_module(
	'LWP::UserAgent',
	new => sub {return $fakeLWP},
    );
    $fakeLWP->mock(env_proxy => sub {})
            ->mock(agent => sub {
		       my $self = shift;
		       my $agent_string = shift;
		       $fakeRes->{_content} .= $agent_string;
		       return $fakeRes;
	       })
            ->mock(get => sub {
		       my ($self, $uri) = @_;
		       my $url = $uri->{_url} || $uri;
		       if ($url =~ m!/model!) {
			   $fakeRes->{_content} = $model;
		       }
		       elsif ($url =~ m!/version!) {
			   $fakeRes->{_content} = "VERSION_STRONE OFG";
		       }
		       else {
			   $fakeRes->{_content} .= $url;
		       }
		       return $fakeRes;
		   });
    $test->{LWP} = $fakeLWP;

    my $fake_TemplateFactory = Test::MockObject->new;
    $fake_TemplateFactory->fake_module(
	'Webservice::InterMine::TemplateFactory',
	new => sub {
	    return $fake_TemplateFactory;
	},
    );
    $fake_TemplateFactory->set_isa('Webservice::InterMine::TemplateFactory');
    $fake_TemplateFactory->mock(
	get_template_by_name => sub {
	    my $self = shift;
	    my $name = shift;
	    return "Mock Template Result - $name";
	},
    );
    $test->{TF} = $fake_TemplateFactory;
}

sub _new : Test(6) {
    my $test = shift;
    use_ok($test->class);
    my @args = (
	root => $test->fake_queryurl,
    );
    my $service = new_ok($test->class, [@args]);
    is($service->root, $test->{uri},
       '... coerces the url correctly');
    is($service->host, 'URI-HOST',
       "... delegates host appropriately",);
    is($service->model, $test->{model},
       "... gets a model for itself",);
    is($service->version, "VERSION_STRONE OFG",
       "... gets a version string for itself",);
    $test->{object} = $service;
}

sub get_results_iterator : Test(4) {
    my $test     = shift;
    my $url      = $test->fake_queryurl;
    my $viewlist = $test->fake_viewlist;
    my $object   = $test->object;
    my $response;
    lives_ok(
	sub {$response = $object->get_results_iterator($url, $viewlist);},
	"Calls for a result iterator ok",
    );

    is_deeply(
	$response->{_init_args}{view_list}, $viewlist,
	'... handles viewlist correctly',
    );
    is(
	$response->{_content},
	$test->user_agent . $url,
	"... handles agent and url correctly"
    ) or diag(explain $test->{Res});
    # is(
    # 	$response->{_init_args}{connection}{_write_args}{'User-Agent'},
    # 	$test->user_agent,
    # 	"... sets the user agent string in the right place",
    # );
    # is(
    # 	$response->{_init_args}{connection}{_init_args}{Host}, 'URI-HOST',
    # 	'... puts host in the right place',
    # );
    $test->{Res}->set_true('is_error');
    throws_ok(
	sub {$object->get_results_iterator},
	qr/Hello, I'm a status line/,
	'... Catches response errors correctly',
    );
    # $test->{connection}->fake_module(
    # 	'Net::HTTP',
    # 	new => sub {return undef},
    # );
    # throws_ok(
    # 	sub {$object->get_results_iterator},
    # 	qr/Could not connect to host/,
    # 	'... Catches connection errors correctly',
    # );
}


sub fetch : Test(2) {
    my $test = shift;
    my $url      = $test->fake_queryurl;
    my $response = $test->class->fetch($url);
    is(
	$response,  $test->user_agent . $url,
	"... handles url and agent correctly",
    );
    $test->{Res}->set_true('is_error');
    throws_ok(
	sub {$test->class->fetch('foo')},
	qr/Hello, I'm a status line/,
	'... Catches response errors correctly',
    );
}

sub methods : Test {
    my $test = shift;
    my @methods = (qw/
	root host model version get_results_iterator
	template fetch
	QUERY_PATH MODEL_PATH TEMPLATES_PATH TEMPLATEQUERY_PATH
	VERSION_PATH USER_AGENT
    /);
    can_ok($test->class, @methods);
}

sub attributes : Test(3) {
    my $test = shift;
    my @readonly_attrs = (qw/
	root model version
    /);
    for (@readonly_attrs) {
	dies_ok(sub {$test->{object}->$_('Any Value')},
		"... dies trying to change $_");
    }
}

sub template : Test {
    my $test = shift;
    my $obj = $test->{object};
    is(
	$obj->template('foo'), 'Mock Template Result - foo',
	"Lazy builds a factory, and delegates to it correctly",
    );
}
1;
