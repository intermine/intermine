package InterMine::DB;

use strict;

use base qw(Rose::DB);
use Config::Properties;

die "\$InterMine::properties_file is not set\n" unless defined $InterMine::properties_file;

open PROPS, '<', $InterMine::properties_file
  or die "unable to open configuration file: $InterMine::properties_file";

my $properties = new Config::Properties();
$properties->load(*PROPS);

my $database = $properties->getProperty("$InterMine::db_prefix.datasource.databaseName");
my $host     = $properties->getProperty("$InterMine::db_prefix.datasource.serverName");
my $username = $properties->getProperty("$InterMine::db_prefix.datasource.user");
my $password = $properties->getProperty("$InterMine::db_prefix.datasource.password");

__PACKAGE__->use_private_registry;

__PACKAGE__->register_db(
                         driver   => 'pg',
                         database => $database,
                         host     => $host,
                         username => $username,
                         password => $password,
                        );
