#!/usr/bin/perl

use strict;
use warnings;

my %replacements = (
    qr{\s*compile\.dependencies\s=\sintermine/integrate/main\s*$} => q{$&, bio/core/main},
    qr{org\.postgresql\.jdbc3\.Jdbc3PoolingDataSource} => q{"org.postgresql.ds.PGPoolingDataSource"},
)

my $svn_loc = $ARGV[0];
my $ext = '.orig';

my @files_to_process;

# FIND ALL THE PROJECT PROPERTIES FILES IN DBMODEL DIRECTORIES
opendir(my $svn_dir, $svn_loc) or die;
for (map {"$svn_loc/$_"} readdir($svn_dir)) {
    if (-d) {
        opendir(my $subdir, $_) or die;
        for my $f (readdir($subdir)) {
            if ($f eq 'dbmodel' and -d $f) {
            push @files_to_process, $_.'/'.$f.'/project.properties';
           }
        }
    }
}

# FIND ALL THE .intermine/$MINE.properties FILES
my $dot_intermine_loc = $ENV{HOME} . '/' . '.intermine'
opendir(my $home_dir, $dot_intermine_loc) or die;
for (map {"$dot_intermine_loc/$_"} readdir($home_dir)) {
    if (/\.properties$/) {
        push @files_to_process, $_;
    }
}

# RUN THE REPLACEMENT LIST OVER THE FILES
for my $file (map {"$_/project.properties"} @files_to_process) {
    open($in, '<', $file) or die;
    my $backup = $file . $ext;
    rename($file, $backup)
    open(my $out, '>', $file) or die;
    select($out);
    while (<$in>) {
        while (my ($k, $v) = each %replacements) {
            s/$k/eval($v)/e;
        }
        print;
    }
    close $in;
    close $out;
}

exit()

