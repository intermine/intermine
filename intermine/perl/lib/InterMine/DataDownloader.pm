package InterMine::DataDownloader;

use strict;
use warnings;
use Net::FTP;
use IO::All;
use File::Compare;
require Exporter;

our @ISA = qw(Exporter);
our @EXPORT = qw(ftp_connect make_link ftp_download http_download compare_http_files checkdir_exists date_string_file unzip_dir convert_date config_species);

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

#compare http files, return 1 if it is a new version or if
#the current data link is missing. Otherwise return 0
sub compare_http_files(){
my ($old,$new,$main_dir,$filename,$link)=@_;

	if(compare("$old","$new") == 1){
		print "New version found.\n";
		return 1;
	}
	#delete the downloaded files if there are no differences
	elsif(compare("$old","$new") == 0){
		print "Current version up to date - ";
		return 0;
	#download anyway if no comparison can be made
	}else{
		print "Current data file not found - current link missing? ";
		return 1;
	}
}

#check if a directory exists and return 0 if it does, 
#create it and return 1 if it doesn't
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
	print "file creation ";
	my $date_string = &convert_date($gene_date_stamp);
	return $date_string;
}

#convert date string into day/month/year, if no string, use current date
sub convert_date(){
	my $string = shift;
	my ($second, $minute, $hour, $day, $month, $year, $weekday, $dayofyear, $isdst);
	
	if($string){
		($second, $minute, $hour, $day, $month, $year, $weekday, $dayofyear, $isdst) = localtime($string);
	}else{
		($second, $minute, $hour, $day, $month, $year, $weekday, $dayofyear, $isdst) = localtime;
	}
	$month += 1;
	$year -= 100;
	$year += 2000;
	print "date is $day $month $year\n";
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

#get taxon Ids from file
sub config_species(){
	my ($file,$trigger) = @_;
	my %data;

	open(F,"<$file") or die "$!";
	while(<F>){
		my @f = split/\t/;
		if($f[0] =~ /^$trigger/g) {
			#for 2 value configs i.e. get_go_annoatation
			if($f[2]){
				chomp $f[2];
				$data{$f[1]}=$f[2];	
			}
			#for everything else
			else{
				chomp $f[1];
				$data{$f[1]}=$f[1];
			}
		}	
	}
	close(F) or die "$!";

return %data;

}
1;
