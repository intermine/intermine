#!/usr/bin/perl


# Build archives for distribution in the current directory, 
# either in zip or tar.gz format.
#
# Options:
#    --svndir [path to intermine src dir] specify source directory
#    --name   [basename for archives]     specify the basename
#    --zip                                make a zip archive
#    --tar                                make a tar archive
#    --help                               print this help text

use strict;
use warnings;

use Getopt::Long;

use File::Find            qw(find);
use File::Copy            qw(copy move);
use File::Basename        qw(basename dirname);
use File::Path            qw(make_path);
use File::Temp            qw(tempdir);
use File::Spec::Functions qw(rel2abs catfile);
use Cwd;           

sub print_help {
    my $scriptname = basename($0);
    my $helpstr = qq(
$scriptname [-z|--zip] [-t|--tar] [--name NAME]

Build archives for distribution in the current directory, 
either in zip or tar.gz format.

Options:
   --svndir [path to intermine src dir] specify source directory
   --name   [basename for archives]     specify the basename
   --zip                                make a zip archive
   --tar                                make a tar archive
   --help                               print this help text

);
    print $helpstr;
    exit;
}

# Housekeeping
my $init_dir = cwd;
(my $svn_dir  = rel2abs($0)) =~ s[/perl/.*$][]; # assuming you are building from within svn, 
                                                # is an option so can be changed by the user
my $zip;
my $tar;
my $name;
my $help = 0;

die unless GetOptions(
    'svndir=s' => \$svn_dir,
    'zip+'     => \$zip,
    'tar+'     => \$tar,
    'name'     => \$name,
    'help+'    => \$help,
    );
my $src_dir = catfile($svn_dir, 'perl');

# Print help and exit if the user asks for it, or if no archive format is specified
print_help if ($help || ! ($tar || $zip)); 

# Read the current version number from the reference file, which is named in the Makefile.PL
my ($version, $version_file);
my $makefile = catfile($src_dir, 'Makefile.PL');

open(my $mf, '<', $makefile) or die "Can't read from makefile $!";
while (<$mf>) {
    if (/VERSION_FROM\s*=>\s*'(.*)'/) {
	$version_file = catfile($src_dir, $1);
	last;
    }
}
close ($mf) or die "Can't close makefile $!";
die "Didn't find a good version file" unless (-f $version_file);

open(my $vf, '<', $version_file) or die "Can't read from $version_file $!";
while (<$vf>) {
    if (/our \$VERSION = '([0-9\.]+)'/) {
	$version = $1;
	last;
    }
}
close($vf) or die "couldn't close version holder";

# Set the root name, unless the user already did this
unless (defined $name) {
    $name = 'InterMine-' . $version; #This can be changed to whatever you want
}

# Make a list of all the files we want to include in the archive,
# along with what their corresponding archived versions would be
my ($newfiles, $oldfiles, $manifest);
open(NEWFILES, '>', \$newfiles);
open(OLDFILES, '>', \$oldfiles);

find (\&zip_files, $src_dir);
sub zip_files {
    unless ($File::Find::name =~ /(
                    \#|
                    DataDownloader|
                    [Uu]til|
                    blib|
                    \/\.|
                    experimental|
                    Makefile$|
                    ~$|
                    bak$|
                    $0|
                    MANIFEST$|
                    yml$|
                    zip$|
                    tar\.gz$
                 )/x
           ) {
	print OLDFILES $File::Find::name, "\n";
	$File::Find::name =~ s[$src_dir/?][$name/];
	print NEWFILES $File::Find::name, "\n";
    }
}

my @oldfiles = split(/\n/, $oldfiles);
my @newfiles = split(/\n/, $newfiles);

# Copy the files out to a temporary directory
my $build_dir = tempdir(CLEANUP => 1);
chdir $build_dir or die "couldn't enter build directory $build_dir $!";
my $i = 0;
for my $old_file (@oldfiles){
    if (-d $old_file) {
	mkdir $newfiles[$i] or die "Didn't make $newfiles[$i] dir\n";
    }
    else {
	copy($old_file, $newfiles[$i]) or die "Didn't copy $old_file to $newfiles[$i]\n";
    }
    $i++;
}

# Copy over the test data, so that the tests pass 
my $xml_model = '/objectstore/model/testmodel/testmodel_model.xml';

my $old_xml   = $svn_dir.$xml_model;
my $rel_data_dir   = 't/data';
my $full_data_dir  = $name.'/'.$rel_data_dir;
my $new_name= basename($xml_model);
my $new_xml   = $full_data_dir .'/'.$new_name;
unless (-d $full_data_dir) {
    make_path($full_data_dir)     or die "couldn't make $full_data_dir $!";
}
copy($old_xml, $new_xml) or die "Couldn't copy $old_xml to $new_xml $!";
push(@newfiles, $new_xml);

# Write a new manifest
open(MANIFEST, '>', "$name/MANIFEST");

find(\&manifest_files, $name);
sub manifest_files {
    if (-f) {
	$File::Find::name =~ s[$name/][];
	print MANIFEST $File::Find::name, "\n";
    }
}
print MANIFEST "META.yml # will be created by \"make dist\"\n";

# Sed doesn't do in-place editing on OS X :(
# Fix references to testmodel_model.xml in source files
my $old_path = dirname($xml_model);
for (@newfiles) {
    if (-f) {
	my $file = $_;
	move($file, $file.'old') or die "couldn't make back up for $file";
	open(NEWVERSION, '>', $file) or die "couldn't write back to $file";
	open(OLDVERSION, '<', $file.'old') or die "couldn't read from ${file}old";
	while (my $line = <OLDVERSION>) {
	    $line =~ s[\.\.$old_path][$rel_data_dir];
	    print NEWVERSION $line;
	}
	close OLDVERSION or die "couldn't close ${file}old";
	close NEWVERSION or die "couldn't close ${file}";
	unlink $file.'old';
    }
}

# Make the archives
my $file_list =  join(' ', @newfiles);
$file_list =~ s/\S+yml\s//;
my (%archive_as, %final_dest_for);
for (qw(.zip .tar.gz)) {
    $archive_as{$_}     = $name.$_;
    $final_dest_for{$_} = catfile($init_dir, $archive_as{$_});
}

system("zip ".$archive_as{'.zip'}." $file_list")     if $zip;
system("tar -czf ".$archive_as{'.tar.gz'}." $name/")    if $tar;

# Move the archives to where they should be
# But don't clobber existing archives of the same name
for (qw(.zip .tar.gz)) {
    if ( (-f $archive_as{$_}) and not (-e $final_dest_for{$_}) ) {
	move($archive_as{$_}, $final_dest_for{$_}) or die "couldn't move $archive_as{$_}, $!";
    }
}

# end where you started

chdir $init_dir or die "couldn't leave build dir";
