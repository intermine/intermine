#!/usr/bin/perl

use strict;

my $infile_path = shift @ARGV;
open( my $infile, $infile_path) or die $infile_path.": $!";

my @action_time = ();
my $maxlength = 0;
while(<$infile>){
	if(/action (\S+) took (\d+) seconds/){

		my ($action, $time) = ($1, $2);
		push @action_time, {action => $action, time => $time};
		if($maxlength < length($action)){
			$maxlength = length($action);
		}
	}
}
print "=" foreach (1..$maxlength+10);
print "\n";
foreach(@action_time){
	printf("%-*s\t%s\n", 
		$maxlength,
		$_->{action},
		&smart_time($_->{time})
	);
}
print "=" foreach (1..$maxlength+10);
print "\n";
print "TOTAL: ".&smart_time(&total_time)."\n";

sub total_time{
	my $total_time = 0;
	foreach(@action_time){
		$total_time += $_->{time};
	}
	return $total_time;
}

sub smart_time{
	my $time = shift;
	if( $time < 60 ){
		return $time."s";
	}elsif( $time < 3600 ){
		my $sec = $time % 60;
		return sprintf("%dm%ds", $time/60, $sec);
	}elsif( $time < 86400 ){
		my $min = ($time / 60) % 60;
		return sprintf("%dh%dm", $time/3600, $min);
	}else{
		my $hours = ($time / 3600) % 24;
		return sprintf("%dd%dh", $time/86400, $hours);
	}
}