#!/usr/bin/perl

use Test::More tests => 7;
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
        auto_write => 1,
        ignore_null => 1,
        ],
    );

lives_ok(
    sub {
        $autowriter->add_item(
            Employee => (
                name => 'Fred',
                age  => undef,
            ),
        );
    },
    'Can add an item with a null field',
);
my $exp = '<items>
   <item id="0_1" class="Employee" implements="">
      <attribute name="name" value="Fred" />
   </item>';
is($output, $exp, 'Makes good xml');

$autowriter->add_item(
    Employee => (
        name => undef,
        age  => 43,
    )
);

$exp .= '
   <item id="0_2" class="Employee" implements="">
      <attribute name="age" value="43" />
   </item>';

is($output, $exp, "Continues to make good xml");

$autowriter->close();

$exp .= '
</items>';

is($output, $exp, "Closes the xml correctly");
