package DataDownloader::Util;

use Exporter qw(import);
use Path::Class qw(file dir);
use Log::Handler;
use DateTime;
use LWP::Simple qw(get);
use File::Compare qw(compare);

our @EXPORT_OK = qw(make_logger get_ymd make_link search_webpage files_are_identical);

sub files_are_identical {
    @_ == 2 or die "Expected two arguments to files_are_identical";
    my ($file_a, $file_b) = @_;
    if (-f $file_a) {
        return 0 unless (-f $file_b);
        return 1 if (compare($file_a, $file_b) == 0);
    }
    if (-d $file_a) {
        return (-d $file_b);
    }
    return 0;
}

#use a reg exp to get a version/release number from a web page
sub search_webpage {
    my ( $source, $reg_exp ) = @_;

    my $page = get($source) or die "could not get $source";
    if ( $page =~ $reg_exp ) {
        return $1;
    } else {
        die "search failed in $source";
    }
}

sub make_logger {

    my $directory = shift;

    my $formatter = sub {
        my $m = shift;
        $m->{message} = sprintf( "%s [%-6s] %s",
            $m->{time}, $m->{level}, $m->{message});
    };
    my $log_format = "%m";
    my $pattern = [qw/%T %L %C %m/];

    my %common_args = (
        message_layout => $log_format,
        message_pattern => $pattern,
        prepare_message => $formatter,
    );
    my %serious = (
        maxlevel       => 'warning',
        minlevel       => 'emergency',
#        debug_trace    => 1,
        debug_mode     => 2,
    );
    my %less_serious = (
        maxlevel       => 'debug',
        minlevel       => 'notice',
    );

    my $log = Log::Handler->new(
        screen => {%common_args, %less_serious, log_to => 'STDOUT'},
        screen => {%common_args, %serious, log_to => 'STDERR'},
    );

#    if ( $directory ) {
#        my $log_dir = dir('', $directory);
#        $log_dir->mkpath() unless (-d $directory);
#        my $log_file = file('', $log_dir, get_ymd() . '.log' );
#        my %file_args = (
#            filename => "$log_file", 
#            mode => 'append', 
#            newline  => 1,
#        );
#       $log->add(
#           file => {%common_args, %less_serious, %file_args},
#          file => {%common_args, %serious, %file_args}
#       );
#   }
    return $log;
}

sub get_ymd {
    my $epoch = shift || time();
    return DateTime->from_epoch( epoch => $epoch )->ymd();
}

sub make_link {
    my ($real, $link) = @_;
    unlink $link;
    symlink( $real, $link );
}

1;

