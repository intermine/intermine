#!/usr/bin/perl

use strict;
use warnings;
use Carp;

use constant APP => 'InterMine-ModelComparer-0.1';

use HTML::Table;
use List::MoreUtils qw(uniq);
use InterMine::WebService::Service::ModelService;

my $order = sub {lc($a) cmp lc($b)};
my $file = '/tmp/out.html';

sub make_table_from_rows {
    my @rows = @_;
    my @columns = sort($order keys %{$rows[0]});
    my $table = HTML::Table->new((@rows + 1), scalar(@columns));
    
    for my $col (1 .. @columns) {
	$table->setCell(1, $col, sprintf("<strong>%s</strong>", $columns[$col -1])); # Set head
	for my $row (2 .. @rows + 1) {
	    my $content = $rows[$row - 2]->{$columns[$col -1]};
	    $table->setCell($row, $col, $content);
	}
    }
    
    for my $row (2 .. @rows + 1) {
	my $colour;
	if (uniq(grep {$_ ne 'NO'} map {$table->getCell($row, $_)} (2 .. @columns)) > 1) {
	    $colour = 'orange';
	}
	else {
	    $colour = 'green';
	}
	for my $col (2 .. @columns) {
	    $table->setCellBGColor(
		$row, $col, 
		(($table->getCell($row, $col) eq 'NO') ? 'red' : $colour),
		);
	}
    }
    return $table;
}

my %model_from;
while (<DATA>) {
    chomp;
    next unless /^http/;
    my $ms = 
	InterMine::WebService::Service::ModelService->new($_, APP);
    $model_from{$_} = $ms->get_model();
}

my %classes;
my $toprow = {Class => 'Total Number of Classes'};
while (my ($service, $model) = each %model_from) {
    my ($service_name) = $service =~ /([a-z]+mine)/;
    my $count;
    for my $class ($model->get_all_classdescriptors) {
	$classes{$class->name}++;
	$count++;
    }
    $toprow->{$service_name} = $count;
}
my @rows = map { {Class => $_} } sort($order keys %classes);
while (my ($service, $model) = each %model_from) {
    my ($service_name) = $service =~ /([a-z]+mine)/;
    for my $class_name (keys %classes) {
	my ($row) = grep {$_->{Class} eq $class_name} @rows;
	$row->{$service_name} = (($model->get_classdescriptor_by_name($class_name))  
			       ? scalar($model->get_classdescriptor_by_name($class_name)->fields)
			       : 'NO');
    }
}
# Set internal anchors to help navigation
for my $row (@rows) {
    my @values = values(%$row);
    if ( grep({$_ eq 'NO'} @values) < (@values - 2) ) {
	$row->{Class} = sprintf(qq!<a href="#%s">%s</a>!, $row->{Class}, $row->{Class});
    }
}
unshift @rows, $toprow;
my $model_class_table = make_table_from_rows(@rows);

my @class_tables;

for my $class_name (sort($order grep {$classes{$_} > 1} keys %classes)) {
    my (%fields, %fields_for);
    # Get an inclusive list of the fields
    while (my ($service, $model) = each %model_from) {
	my ($service_name) = $service =~ /([a-z]+mine)/;
	for (eval{$model->get_classdescriptor_by_name($class_name)->fields}) {
	    push @{$fields_for{$service_name}}, $_->name; 
	    $fields{$_->name}++;
	}
    }
    my @rows = map { {Field => $_} } sort($order keys %fields);
    next unless @rows;
    for my $row (@rows) {
	for my $service (keys %fields_for) {
	    $row->{$service} = (grep {$row->{Field} eq $_} @{$fields_for{$service}})
		              ? 'YES'
                              : 'NO';
	}
    }

    my $class_table = make_table_from_rows(@rows);
    push @class_tables, {name => $class_name, tbl => $class_table};
}

open(my $HTMLFH, '>', $file) or die "Blah blah blah error schmeror: $!";
print $HTMLFH '<html><head></head><body>';
print $HTMLFH '<h1>Model Comparison Tables</h1>';
print $HTMLFH q{<p>This page has several tables to compare different elements found within the genomic models used by different InterMine implementations. These models were fetched from the respective webservices, and then analysed to create these tables.</p> 
<p>The main table lists which classes appear in which Mine's model. Where the class is absent, the word "NO" appears in a red box. If the class is present, then the number of fields in that class appear in the cell. If the models with this class all have a class the same number of fields, then their cells are green. If there is a difference in the number of fields, then the cell is coloured orange.</>
<p>To further investigate classes with differences you can click on the class name, which is a link that will take you to a sub-table, showing which fields are present or absent in a particular model. Only classes that are present in more than one model have a sub-table, and thus a link.</p>};
print $HTMLFH '<h2>Classes by Model</h2></br>';
print $HTMLFH $model_class_table->getTable, '</br>';
for my $table (@class_tables) {
    print $HTMLFH '<h2><a name="',$table->{name},'">',$table->{name},'</a></h2></br>';
    print $HTMLFH $table->{tbl}->getTable, '</br>';
}
print $HTMLFH '</body></html>';

close $HTMLFH;

__DATA__

http://yeastmine.yeastgenome.org:8080/yeastmine/service
http://ratmine.mcw.edu/ratmine/service
http://www.flymine.org/query/service

