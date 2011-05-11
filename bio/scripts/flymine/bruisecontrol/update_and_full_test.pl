#!/usr/bin/perl -w

# BruiseControl
#
# runs all intermine core, webapp and bio tests and sends a report of failing
# junit and checkstyle tests to those users who have checked in a file since
# the last run

use strict;

use Date::Manip;
use File::Path;
use Email::Send;
use File::Copy;

my $debug = 0;

if ( @ARGV > 0 ) {
    if ( $ARGV[0] eq '-d' ) {
        $debug = 1;
    } else {
        die "unknown args: @ARGV\n";
    }
}

$ENV{ANT_OPTS} = '-XX:MaxPermSize=200M -Xmx1550M -server -XX:+UseParallelGC';

# make sure files and directories are readable by all
umask 0002;

# wait at least this many minutes from the last check in before starting tests
my $MIN_TIME_FROM_LAST_CHECKIN = 5;

# databases to build-db
my @DATABASES = qw(bio-fulldata-test bio-test webservice-test unittest
  notxmltest truncunittest fulldatatest flatmodetest genomictest);

my $RUNNING_FILE             = "$ENV{'HOME'}/public_html/tests/.running";
my $TRUNK_DIR                = "$ENV{'HOME'}/public_html/tests/trunk";
my $BUILD_PROJ               = "$TRUNK_DIR/intermine/all";
my $ARCHIVED_DIR             = "$ENV{'HOME'}/public_html/tests/archived";
my $TIME_STAMP               = UnixDate( "now", "%O" );
my $ARCHIVE_TO               = "$ARCHIVED_DIR/$TIME_STAMP";
my $PREVIOUS_JUNIT_FAIL_FILE = "$ENV{'HOME'}/public_html/tests/previous_junit_failures";
my $URL_PREFIX               = "http://bc.flymine.org";
my $ANT_COMMAND              = "ant -v -lib /software/noarch/junit/";
my $JUNIT_FAIL_FILE_NAME     = "/tmp/bc_$$.junit_failures";
my $BRUISER_EMAIL            = 'bruiser@flymine.org';

print "--------------------------------------------------------------------------\n";
print "Trying new build\n";

# -------------------------------------------------------------------------- #
# Check whether we really need to do anything at all - get the time and
# change into the appropriate directory
# -------------------------------------------------------------------------- #

sub now {
    return UnixDate( "now", "%g" );
}

if ( -f $RUNNING_FILE ) {
    print now(), ": Not starting tests because $RUNNING_FILE exists\n";
    exit(0);
}

chdir $TRUNK_DIR or die "couldn't change to $TRUNK_DIR\n";

# -------------------------------------------------------------------------- #
# Check to see if there are any updates available
# -------------------------------------------------------------------------- #

my $updates_available = 0;

{
    my @svn_args = qw/svn status -u/;
    open( my $svn_status, '-|', @svn_args ) or die "'", join(' ', @svn_args),  "' failed to start. $!\n";

    while (<$svn_status>) {
        $updates_available++ if /^\s+\*\s+\d+/;
    }

    close $svn_status or die "couldn't close pipe to svn: $!\n";
}

unless ($updates_available) {
    print "*** ", now(), " no updates in repository ***\n";
    exit(0);
}

# -------------------------------------------------------------------------- #
# Check the last update was at least 10 minutes ago
# -------------------------------------------------------------------------- #

my $last_change = undef;

{
    my @svn_log_args = (qw/svn log --revision HEAD/);
    open(my $svn_log, '-|',  @svn_log_args) or die "can't open pipe to svn: $!\n";

    while ( my $log_line = <$svn_log> ) {
        if ( $log_line =~ m/.*\|.*\|\s+(.*)\s+\+\d+ \(.*\)\s+\|/ ) {
            $last_change = UnixDate( ParseDate($1), "%s" );
        }
    }

    close $svn_log or die "couldn't close pipe to svn: $!\n";
}

my $diff = UnixDate( "now", "%s" ) - $last_change;
print "$diff seconds since last change\n";

if ( $diff < 60 * $MIN_TIME_FROM_LAST_CHECKIN ) {
    print now(), ": Need to wait ", ( 60 * $MIN_TIME_FROM_LAST_CHECKIN - $diff ),
      " more seconds\n";
    exit(0);
} else {
    print now(), ": $MIN_TIME_FROM_LAST_CHECKIN minutes since last checkin - starting tests\n";
}

{

    # create empty file
    open my $run_file, ">$RUNNING_FILE" or die "failed to create $RUNNING_FILE: $!\n";
    close $run_file or die "failed to close $RUNNING_FILE: $!\n";
}

# find the current checkout version
my $local_version = undef;
{
    open my $svn_info, "svn info|" or die "can't open pipe to svn: $!\n";

    while ( my $info_line = <$svn_info> ) {
        if ( $info_line =~ /^Revision: (\d+)/ ) {
            $local_version = $1;
        }
    }

    close $svn_info or die "couldn't close pipe to svn: $!\n";
}

if ( !defined $local_version ) {
    die "can't find the local revision in the output of svn info\n";
}

# find who has made changes since the last run
my @blame = ();
{
    warn "svn log -r $local_version:HEAD\n";
    open my $svn_log, "svn log -r $local_version:HEAD|" or die "can't open pipe to svn: $!\n";

    while ( my $line = <$svn_log> ) {
        if ( $line =~ /^r\d+ \| (\S+)/ ) {
            if ( !grep { $_ eq $1 } @blame ) {
                push @blame, $1;
            }
        }
    }

    close $svn_log or die "couldn't close pipe to svn: $!\n";
}

if ( @blame == 0 ) {
    die "couldn't find any user names in svn log output - noone to blame\n";
}

@blame = map { $_ . '@flymine.org' } @blame;

print "BLAME = @blame\n";

mkpath( $ARCHIVE_TO, 1, 0775 );

unlink "$ARCHIVED_DIR/latest";
symlink $ARCHIVE_TO, "$ARCHIVED_DIR/latest"
  or die "can't create symlink to $ARCHIVED_DIR/latest: $!\n";

my $ANT_LOG_NAME = "$ARCHIVE_TO/ant_log.txt";
open my $ant_log, '>', $ANT_LOG_NAME or die "can't open log file ($ANT_LOG_NAME): $!\n";

sub log_and_print {
    my $message = shift;
    print $message, "\n" if $debug;
    print $ant_log $message, "\n";
}

log_and_print("trying new build");
log_and_print("updating ...");

my $update_output = "";

{
    open my $svn_update, "svn update|" or die "couldn't open pipe to svn: $!\n";

    while ( my $update_line = <$svn_update> ) {
        $update_output .= $update_line;
    }

    close $svn_update or die "couldn't close pipe to svn: $!\n";
}

sub pipe_to_log {
    my $arg = shift;

    my @commands = ();

    if ( ref $arg ) {
        @commands = @$arg;
    } else {
        push @commands, $arg;
    }

    for my $command (@commands) {
        log_and_print("opening pipe to $command");

        open my $pipe, "$command 2>&1|" or die "can't open pipe to $command: $!\n";

        while ( my $line = <$pipe> ) {
            print STDERR $line if $debug;
            print $ant_log $line;
        }

        close $pipe or warn "problem while closing pipe to $command: $!\n";

        if ( $? != 0 && $command =~ /build-db/ ) {
            warn "build-db failed: $command returned $?\n";
            return $?;
        }
    }

    return $?;
}

log_and_print("cleaning ...");
pipe_to_log("cd $BUILD_PROJ; ant clean-all");

log_and_print("creating databases ...");
for my $test_project (@DATABASES) {
    pipe_to_log("dropdb $test_project");
    pipe_to_log("createdb $test_project");
}

# the "true" is because the dropdb will fail if /intermine-query webapp exists
pipe_to_log("dropdb testmodel-webapp; createdb testmodel-webapp; true");
pipe_to_log(
    "dropdb testmodel-webapp-userprofile; createdb testmodel-webapp-userprofile; true");

log_and_print("testmodel build-db ...");
my $build_result = pipe_to_log("cd testmodel/dbmodel; ant -v build-db");

# intermine tests

log_and_print("intermine fulltest ...");
pipe_to_log("cd $BUILD_PROJ; date; $ANT_COMMAND fulltest");

log_and_print("intermine test-report ...");
pipe_to_log("cd $BUILD_PROJ; date; $ANT_COMMAND test-report");

# bio tests

pipe_to_log(
    [
        "cd $TRUNK_DIR/bio/test-all/dbmodel; $ANT_COMMAND -v clean-all",
        "cd $TRUNK_DIR/bio/test-all/dbmodel; $ANT_COMMAND -v build-db",
        "cd $TRUNK_DIR/bio/test-all; $ANT_COMMAND clean",
        "cd $TRUNK_DIR/bio/test-all; date; $ANT_COMMAND fulltest",
        "cd $TRUNK_DIR/bio/test-all; date; $ANT_COMMAND test-report"
    ]
);

my @junit_failures = ();

close $ant_log or die "can't close $ANT_LOG_NAME: $!\n";

open $ant_log, $ANT_LOG_NAME or die "can't open log file ($ANT_LOG_NAME): $!\n";
open my $junit_fail_file, ">$JUNIT_FAIL_FILE_NAME";

my $build_failed  = 0;
my $test_failures = 0;

# find and save junit failures and find build failures and their context
my @failure_lines = ();
{
    my @prev_lines         = ();
    my $failure_line_count = 0;
    while ( my $ant_log_line = <$ant_log> ) {
        if (   $ant_log_line =~ /\[junit\].*FAILED/
            && $ant_log_line !~ /\[junit\] Tests FAILED/ )
        {
            print $ant_log_line if $debug;
            print $junit_fail_file $ant_log_line;
            push @junit_failures, $ant_log_line;
            ++$test_failures;
        } else {
            if ( $ant_log_line =~ /BUILD FAILED/ ) {
                push @failure_lines, @prev_lines;
                $failure_line_count = 10;
                $build_failed       = 1;
            }
        }

        if ( $failure_line_count > 0 ) {
            push @failure_lines, $ant_log_line;
            --$failure_line_count;
        }

        if ( @prev_lines > 20 ) {
            shift @prev_lines;
        }
        push @prev_lines, $ant_log_line;
    }
}

close $ant_log or die "can't close $ANT_LOG_NAME: $!\n";

open $ant_log, '>>', $ANT_LOG_NAME or die "can't open log file ($ANT_LOG_NAME): $!\n";

my $checkstyle_exitcode = pipe_to_log("cd $BUILD_PROJ; ant checkstyle");

close $ant_log or die "can't close $ANT_LOG_NAME: $!\n";

if ($build_failed) {
    print "*** build failed - see log for errors ***\n";
} else {
    print "*** BUILD SUCCESS ***\n";
}

if ( $test_failures == 0 ) {
    print "*** TEST SUCCESS ***\n";
} else {
    print "*** $test_failures tests failed - see log for errors ***\n";
}

if ($checkstyle_exitcode) {
    print "*** CHECKSTYLE FAILURES ***\n";
} else {
    print "*** NO CHECKSTYLE FAILURES ***\n";
}

if ( -f "$BUILD_PROJ/build/test/results/index.html" ) {
    mkpath "$ARCHIVE_TO/junit", 1, 0775 or die "can't mkpath $ARCHIVE_TO/junit: $!\n";
    system "cp -r $BUILD_PROJ/build/test/results/* $ARCHIVE_TO/junit/";
} else {
    print "There don't seem to be any intermine results\n";
}

my $bio_results_dir = "$BUILD_PROJ/../../bio/test-all/build/test/results";
if ( -f "$bio_results_dir/index.html" ) {
    mkpath "$ARCHIVE_TO/junit_bio", 1, 0775 or die "can't mkpath $ARCHIVE_TO/junit_bio: $!\n";
    system "cp -r $bio_results_dir/* $ARCHIVE_TO/junit_bio";
} else {
    print "There don't seem to be any bio results\n";
}

if ( -f "$BUILD_PROJ/build/checkstyle/index.html" ) {
    mkpath "$ARCHIVE_TO/checkstyle", 1, 0775
      or die "can't mkpath $ARCHIVE_TO/checkstyle: $!\n";
    system "cp -r $BUILD_PROJ/build/checkstyle/* $ARCHIVE_TO/checkstyle/";
} else {
    print "There don't seem to be any checkstyle results\n";
}

# Blame people via email

my $subject = "[BruiseControl] SUCCESS at " . now();

if ($build_failed) {
    $subject = "[BruiseControl] Build BROKEN at $TIME_STAMP";
} else {
    if ( $test_failures > 0 ) {
        $subject = "[BruiseControl] $test_failures tests FAILING at $TIME_STAMP";
    }
}

my @previous_junit_failures = ();

open my $prev_junit_failures, '<', $PREVIOUS_JUNIT_FAIL_FILE;

@previous_junit_failures = <$prev_junit_failures>;

close $prev_junit_failures or die "can't close $PREVIOUS_JUNIT_FAIL_FILE\n";

my $recipients = join ', ', @blame;

my $message = <<"__START__";
To: $recipients
From: Bruise Control <$BRUISER_EMAIL>
Subject: $subject

JUnit results:

 intermine core:   $URL_PREFIX/$TIME_STAMP/junit/
 intermine bio:    $URL_PREFIX/$TIME_STAMP/junit_bio/

 Checkstyle:       $URL_PREFIX/$TIME_STAMP/checkstyle/
 Ant output:       $URL_PREFIX/$TIME_STAMP/ant_log.txt

Last update:
$update_output

__START__

if ( @failure_lines > 0 ) {
    $message .= <<"__BUILD_FAILURES__";

------------------------------------------------------------
Build failures:
 @failure_lines

__BUILD_FAILURES__
}

$message .= <<"__DIFF_HEADER__";
------------------------------------------------------------
Test differences:

__DIFF_HEADER__

{
    open my $diff_pipe, "diff -u $PREVIOUS_JUNIT_FAIL_FILE $JUNIT_FAIL_FILE_NAME|"
      or die "can't open diff pipe: $!\n";

    while ( my $diff_line = <$diff_pipe> ) {
        $message .= $diff_line;
    }

    close $diff_pipe or warn "can't close diff pipe: $!\n";
}

$message .= <<"__MESSAGE_MIDDLE__";
------------------------------------------------------------
Test failures now:

 @junit_failures

------------------------------------------------------------
Previous test failures:

 @previous_junit_failures

__MESSAGE_MIDDLE__

my $sender = Email::Send->new( { mailer => 'SMTP' } );
$sender->mailer_args( [ Host => 'mail.flymine.org' ] );
$sender->send($message);

copy( $JUNIT_FAIL_FILE_NAME, $PREVIOUS_JUNIT_FAIL_FILE );

print "*** Finished build $TIME_STAMP at ", now(), " ***\n";

unlink $RUNNING_FILE or die "can't unlink $RUNNING_FILE: $!\n";
