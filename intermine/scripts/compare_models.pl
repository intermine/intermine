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
for my $model (values %model_from) {
    for my $class ($model->get_all_classdescriptors) {
	$classes{$class->name}++;
    }
}

my @rows = map { {Class => $_} } 
               sort($order keys %classes);
while (my ($service, $model) = each %model_from) {
    my ($service_name) = $service =~ /([a-z]+mine)/;
    for my $class_name (keys %classes) {
	my ($row) = grep {$_->{Class} eq $class_name} @rows;
	$row->{$service_name} = (($model->get_classdescriptor_by_name($class_name))  
			       ? scalar($model->get_classdescriptor_by_name($class_name)->fields)
			       : 'NO');
    }
}
for my $row (@rows) {
    my @values = values(%$row);
    if ( grep({$_ eq 'NO'} @values) < (@values - 2) ) {
	$row->{Class} = sprintf(qq!<a href="#%s">%s</a>!, $row->{Class}, $row->{Class});
    }
}

my $model_class_table = make_table_from_rows(@rows);

my @class_tables;

for my $class_name (sort($order grep {$classes{$_} > 1} keys %classes)) {
    my (%fields, %fields_for);
    while (my ($service, $model) = each %model_from) {
	my ($service_name) = $service =~ /([a-z]+mine)/;
	for (eval{$model->get_classdescriptor_by_name($class_name)->fields}) {
	    push @{$fields_for{$service_name}}, $_->name; 
	    $fields{$_->name}++;
	}
    }
    my @rows = map { {Field => $_} } sort($order keys %fields);
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
print $HTMLFH '<strong><em>Classes by Model</em></strong></br>';
print $HTMLFH $model_class_table->getTable, '</br>';
for my $table (@class_tables) {
    print $HTMLFH '<strong><a name="',$table->{name},'">',$table->{name},'</a></strong></br>';
    print $HTMLFH $table->{tbl}->getTable, '</br>';
}
print $HTMLFH '</body></html>';

close $HTMLFH;


__DATA__

http://yeastmine.yeastgenome.org:8080/yeastmine/service
http://ratmine.mcw.edu/ratmine/service
http://www.flymine.org/query/service

