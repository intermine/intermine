#!/usr/bin/perl

use File::Basename;
use Dancer::Config qw(setting);
use lib dirname(__FILE__) . '/../lib';
use perl5i::2;
use Crypt::SaltedHash;
use Registry::Model qw(get_admins update_admins);
use Term::Prompt;

setting(appdir => dirname(__FILE__) . '/..');
Dancer::Config::load();

my ($name, $pass) = @ARGV;

$name = prompt('a', 'user name:', 'please supply a valid username', undef) unless $name;

$pass = prompt('p', 'password', 'please supply a password', '') unless $pass;


my $csh = Crypt::SaltedHash->new();
$csh->add($pass);

my $salted = $csh->generate;

my $admins = get_admins();

my $updated = exists $admins->{$name};
$admins->{$name} = {name => $name, password => $salted};

update_admins($admins);

say "Successfully ", $updated ? "updated" : "added", " entry for $name";

