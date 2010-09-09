#!/usr/bin/perl

use strict;
use warnings;

my $build = Module::Build->new(module_name => 'InterMine');
$build->dispatch('build');
$build->dispatch('test');
$build->dispatch('install');
