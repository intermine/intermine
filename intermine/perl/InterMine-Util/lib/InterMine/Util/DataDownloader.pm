package InterMine::Util::DataDownloader;

use strict;
use warnings;
use Carp;
use Net::FTP;
use DateTime;
use IO::All;
use IO::All::LWP;
use File::Compare qw(compare);
use File::Spec::Functions;
use File::Path qw(make_path);
use XML::DOM;
use constant VERSION => 'VERSION';
require Exporter;

our @ISA    = qw(Exporter);
our @EXPORT = qw(ftp_connect make_link ftp_download http_download compare_files
  checkdir_exists date_string_file unzip_dir convert_date
  config_species write_version write_log search_webpage get_taxonIds
  make_download_dir get_download_path);

=head2 ftp_connect(server, user, pass)

 Function: Connect to a given FTP server with the given credentials
 Throws:   fatal errors on failed connection or logins
 Returns:  The ftp connection object

=cut

sub ftp_connect {
    my ( $server, $user, $password ) = @_;

    my $ftp = Net::FTP->new( $server, Passive => 1 )
      or croak "Cannot connect to $server: $@";

    $ftp->login( $user, $password )
      or croak
      "Cannot login to $server with credentials: user=$user, pass=$password",
      $ftp->message;

    return $ftp;
}

=head2 ftp_download(connection, destination-directory, file-on-server)

 Function: download file from ftp connection to a given directory
 Throws:   Fatal erros on failure to get the file

=cut

sub ftp_download {
    my ( $ftp, $dir, $file ) = @_;
    print STDERR "downloading $file to $dir\n";

    $ftp->binary();
    $ftp->get( $file, catfile( $dir, $file ) )
      or croak "Failed to get $file: ", $ftp->message;
}

=head2 http_download(source, destination)

 Function: Download a file from an http server
 Throws:   Fatal errors for IO related issues

=cut

sub http_download {
    my ( $source, $destination ) = @_;

    print "downloading $source to $destination\n";
    io($source) > io($destination);
}

#compare two files, return 1 if it is a new version or if
#the current data link is missing. Otherwise return 0
sub compare_files {
    my ( $old, $new ) = @_;

    my $comparison = compare( $old, $new );

    if ( $comparison == 1 ) {
        print "New version found.\n";
        return 1;
    }

    #delete the downloaded files if there are no differences
    elsif ( $comparison == 0 ) {
        print "Current version up to date - ";
        return 0;

        #download anyway if no comparison can be made
    }
    else {
        print "Current data file not found - cannot compare files. ";
        return 1;
    }
}

#check if a directory exists and return 0 if it does,
#create it and return 1 if it doesn't
sub checkdir_exists {
    my $dir = shift;
    if ( !( -d $dir ) ) {
        print STDERR "creating directory: $dir\n";
        mkdir $dir
          or croak "failed to create directory $dir";
        return 1;
    }
    else {
        print STDERR "$dir exists\n";
        return 0;
    }
}

sub get_download_path {
    my @parts = @_;
    my $path  = catfile(@parts);
    return $path;
}

sub make_download_dir {
    my $path = get_download_path(@_);
    if ( not -d $path ) {
        make_path( $path, { verbose => 1 } );
    }
    return $path;
}

#get the date stamp from a file to be downloaded
sub date_string_file {
    my ( $ftp, $file ) = @_;

    return convert_date( $ftp->mdtm($file) );
}

#convert epoch string into year-month-day, defaulting to now
sub convert_date {
    my $time = shift || time();
    return DateTime->from_epoch( epoch => $time )->ymd();
}

#unzip files
sub unzip_dir {
    my $dir     = shift;
    my $command = "gzip -dr $dir\n";
    print "Executing: ", $command;
    if ( ( system $command) != 0 ) {
        croak qq|"$command" failed: $?\n|;
    }
}

#create symbolic links to the latest file, removing the link if it already exists
sub make_link {
    croak "Incorrect arguments to make_link: @_" if ( @_ != 2 );
    my ( $dir, $link ) = @_;
    unlink $link;
    symlink( $dir, "$link" ) or croak "can't create $link";
}

#get taxon Ids from config file
sub config_species() {
    croak "Incorrect arguments to config_species: @_" if ( @_ != 2 );
    my ( $file, $trigger ) = @_;
    my %data;

    for ( io($file)->getlines ) {
        my @f = split(/\t/);
        if ( $f[0] =~ /^$trigger/ ) {

            #for 2 value configs i.e. get_go_annoatation
            if ( @f == 4 ) {
                chomp $f[3];
                $data{ $f[1] }{ $f[2] } = $f[3];
            }
            elsif ( @f == 2 ) {
                chomp $f[1];
                $data{ $f[1] } = $f[1];
            }
            else {
                croak "Bad config file: got $_";
            }
        }
    }
    return %data;
}

sub get_organisms {
    croak "Incorrect arguments to get_organism: @_" if ( @_ != 2 );
    my ( $file, $trigger ) = @_;
    my @organisms;

    for ( io($file)->getlines ) {
        my @f = split(/\t/);
        if ( $f[0] =~ /^$trigger/ ) {
            chomp $f[1];
            push( @organisms, $f[1] );
        }
    }
    return @organisms;
}

#get taxon Ids from project.xml. not implemented yet, but will work with uniprot
sub get_taxonIds {
    my ( $file, $trigger ) = @_;

# parse file looking for this element: <property name="uniprot.organisms" value="7955 9606"/>
    my $parser = new XML::DOM::Parser;
    my $doc    = $parser->parsefile($file);
    my $taxon_list;
  NODES: for my $node ( $doc->getElementsByTagName("property") ) {
        if ( $node->getAttribute("name") eq $trigger ) {
            $taxon_list = $node->getAttribute("value");
            last NODES;
        }
    }
    $doc->dispose();
    warn "processing $taxon_list\n";

    # Return a hash of the taxon ids, where the value for each key is 1
    return map { ( $_ => 1 ) } split( /\s/, $taxon_list );
}

#write the version file, removing it if it exists
sub write_version {
    my ( $root_dir, $buffer ) = @_;

    my $destination = catfile( $root_dir, VERSION );
    unlink $destination;
    write_file( $destination, $buffer );
}

#write the download log file
sub write_log {
    my ( $buffer, $logdir, $logname ) = @_;

    checkdir_exists($logdir);
    write_file( catfile( $logdir, $logname ), $buffer );
}

#for write_version and write_log - appends the buffer to the destination
sub write_file {
    my ( $destination, $source ) = @_;
    $source >> io($destination);
}

#use a reg exp to get a version/release number from a web page
sub search_webpage {
    my ( $source, $reg_exp ) = @_;
    my $number;

    my $page < io($source);
    if ( $page =~ $reg_exp ) {
        $number = $1;
    }
    return $number;
}

1;
