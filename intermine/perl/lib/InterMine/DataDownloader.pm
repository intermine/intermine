package InterMine::DataDownloader;

use strict;
use warnings;
use Net::FTP;
use IO::All;
require Exporter;

our @ISA = qw(Exporter);
our @EXPORT = qw(ftp_connect make_link ftp_download http_download checkdir_exists date_string_file unzip_dir);

#connect to server
sub ftp_connect(){
my ($server,$user,$password) = @_;

my $ftp = Net::FTP->new($server, Passive => 1)
or die "Cannot connect to $server: $@";

$ftp->login($user,$password)
or die "Cannot login ", $ftp->message;

return $ftp;
}

#create symbolic links to the latest file
sub make_link(){
	my ($dir, $link) = @_;
	unlink $link;
	symlink ($dir, "$link") or die "can't create $link";
}

#download file from ftp server
sub ftp_download(){
	my ($ftp,$dir, $file) = @_;
	print STDERR "getting $file to $dir\n";

  	$ftp->binary();
  	$ftp->get($file, "$dir/$file")or die "get failed ", $ftp->message;
}

#download file from http server
sub http_download(){
	my ($source, $destination) = @_;
	
	print "getting $source\n";
	io($source) > io($destination);
}

#check if a directory exists, create it if it doesn't
sub checkdir_exists(){
	my $dir = shift;
	if (!(-d $dir)) {
	    print STDERR "creating directory: $dir\n";
	    mkdir $dir	
	        or die "failed to create directory $dir";
		return 1;	
	}else{
		print STDERR "$dir exists\n";
		return 0;
	}
}
#get the date stamp from a file to be downloaded
sub date_string_file(){
	my ($ftp,$file) = @_;

	my $gene_date_stamp = $ftp->mdtm($file);
	my ($second, $minute, $hour, $day, $month, $year, $weekday, $dayofyear, $isdst) = localtime($gene_date_stamp);

	$month += 1;
	$year -= 100;
	$year += 2000;

	print "$file was created on $day $month $year\n";
	my $date_string = sprintf "%02s-%02s-%02s", $year, $month, $day;

	return $date_string;
}

#unzip files
sub unzip_dir(){
	my $dir = shift;
	print"gzip -dr $dir\n";
	if ((system "gzip -dr $dir") != 0) {
	  die qq|system "gzip -dr $dir" failed: $?\n|;
	}
}
1;
