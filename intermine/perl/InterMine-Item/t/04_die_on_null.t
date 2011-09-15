#!/usr/bin/perl

use Test::More tests => 6;
use InterMine::Model;
use Test::Exception;
my $doc = 'InterMine::Item::Document';
use_ok($doc);

my $model = new InterMine::Model(file => 't/data/testmodel_model.xml');
my $document = new_ok($doc => [model => $model], 'Can make a document');

my $output;
my $autowriter = new_ok(
    $doc => [
        model  => $model, 
        output => \$output,
        ],
    );

dies_ok(
    sub {
        $autowriter->add_item(
            Employee => (
                name => 'Fred',
                age  => undef,
            ),
        );
    },
    'Dies on items that are null',
);

my $fred;

lives_ok(
    sub {
        $fred = $autowriter->add_item(
            Employee => (
                name => 'Fred',
                age  => 17,
            ),
        );
    },
    'Can still make items',
);

$fred->{age} = undef;

dies_ok(
    sub {$autowriter->write},
    "dies writing items that are somehow null"
);

$autowriter->writer->endTag("item");

