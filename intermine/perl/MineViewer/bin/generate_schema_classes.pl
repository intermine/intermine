#!/usr/bin/perl -w

use strict;
use warnings;
use Carp;

use DBIx::Class::Schema::Loader qw(make_schema_at);

@ARGV or "$0: error: I need the location of the sqlite.db file\n";

my $db_file = shift;

my $schema_class = 'MineViewer::DB';

make_schema_at($schema_class,
    {
        debug => 0, dump_directory => './lib',
        naming => 'current',
        use_moose => 1,
    },
    ['dbi:SQLite:dbname=' . $db_file, '', '' ]
);
