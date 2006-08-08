#!/usr/bin/perl

# Run the FlyMine production build and dump the database occasionally
# Targets are listed at the end of the script
# The database to dump will be found by reading the properties from $HOME/flymine.properties

use strict;
use warnings;
use Cwd 'chdir';
use Expect;
use Getopt::Std;
use Cwd;
use XML::DOM;
use Data::Dumper;

# Directory aliases and usefull hashkeys.
my $dbmodel = "dbmodel";
my $integrate = "integrate";
my $postprocess = "post-process";

# Dont forget to rename this.
my $projectXmlFile= "project.xml";

my $antCmd;
my $targetMine;
my $fsep = "/";



# Command line vars.
my $prodHost = undef;
my $prodDb = undef;
my $prodUser = undef;
my $prodPass = undef;
my $prodPort;
my $dumpPrefix = undef;

# Defaults to user home unless specified as a cmd line arg
my $dumpDataDir = "$ENV{HOME}";

# Command line switches.
my $verbose = 0;
my $dryRun = 0;
my $carryOnRestart = 0;
my $prevDmpRestart = 0;
my $release;

my %opts = ();

getopts('vdrRV:', \%opts);

if ($opts{r}) {
  $carryOnRestart = 1;
}

if ($opts{R}) {
  $prevDmpRestart = 1;
}

if ($carryOnRestart && $prevDmpRestart) {
    printStdOut("Switches r and R are mutually exclusive - try using only one\n\n");
    usage();
    die;
}

if ($opts{v}) {
  $verbose = 1;
  $antCmd = "ant -v "
} else {
  $antCmd = "ant ";
}

if ($opts{V}) {
  $release=$opts{V};
  $antCmd .= "-Drelease=$release ";
}

if ($opts{d}) {
  $dryRun = 1;
}


sub usage
{
  die <<'EOF';
usage:
  $0 full_name_of_mine_to_target [restart_dump_prefix [data_dump_directory]]

note: restart_dump_prefix -- restarts from a source or post-process
note: data_dump_directory -- an alternative to the users home directory

  switches/options

    NOTE: -r and -R are mutually exlusive options.
    -r restart from where we last got up to.
    -R restart from the previous dump file.

examples:
  $0 malariamine [uniprot-malaria [data_dump_dir]]
or
  $0 malariamine [transfer-sequences [data_dump_dir]]

EOF
}

if (@ARGV == 1) {
  $targetMine = $ARGV[0];
} elsif (@ARGV == 2) {
  $targetMine = $ARGV[0];
  $dumpPrefix = $ARGV[1];
} elsif (@ARGV == 3) {
  $targetMine = $ARGV[0];
  $dumpPrefix = $ARGV[1];
  $dumpDataDir = $ARGV[2];
} else {
  usage;
}

if (-e $projectXmlFile) {
    print("Found $projectXmlFile\n");
} else {
    die "Can't find a file named $projectXmlFile check that you are in the root directory of the chosen mine.\n";
}

if (($carryOnRestart or $prevDmpRestart) and (!defined $dumpPrefix)) {
    die "Need to specify a dump file prefix (i.e. a source or postprocess name) if using -r or -R";
} elsif( !($carryOnRestart or $prevDmpRestart) and (defined $dumpPrefix)) {
    printStdOut("DUMP_PREFIX:" . $dumpPrefix);
    die "Need to specify -r or -R if supplying a restart dump prefix";
} else {
    printStdOut("DUMP_PREFIX:" . defined $dumpPrefix ? $dumpPrefix : "NOT SET!");
    #die"testing...";
}

my $parser = new XML::DOM::Parser;
my $doc = $parser->parsefile ($projectXmlFile);
my @dump_command = qw[pg_dump -c];
my @psql_command = qw[psql -q];

my $mine_properties = getMinePropsFile();
printStdOut("$mine_properties\n");

runProduction();

#--------------------------------------------- Sub Routines -------------------------------------------------#
sub runProduction
{
  # Initialize any required params...
  init();

  my @cmdList = ();

  # Get command to rebuild the production database - by current default this stage doesn't dump...
  push @cmdList, @{buildDbCommand("build-db")};

  # Get commands for the integration pipeline on included sources.
  push @cmdList, @{buildCommandsForTag("source", "integrate", "integrate -Dsource=")};

  # Get commands for the post processing part of the pipeline.
  #$cmdList{$postprocess} = buildCommandsForTag("post-process", "postprocess", "postprocess -Doperation=");
  push @cmdList, @{buildCommandsForTag("post-process", "postprocess", "postprocess -Doperation=")};

  #printStdOut(Data::Dumper->Dump([\%cmdList]));
  # Do the actual work...
  executeCommands(\@cmdList);

  # Avoid memory leaks - cleanup circular references for garbage collection
  $doc->dispose;
}

sub init
{

  $prodHost = get_prop_val ("db.production.datasource.serverName", $mine_properties);
  $prodDb = get_prop_val ("db.production.datasource.databaseName", $mine_properties);
  $prodUser = get_prop_val ("db.production.datasource.user", $mine_properties);
  $prodPass = get_prop_val ("db.production.datasource.password", $mine_properties);

  if ($prodHost =~ /(.+):(\d+)/) {
    $prodHost = $1;
    $prodPort = $2;
  }

  if ($verbose) {
    printStdOut ("read properties:");
    printStdOut ("  prod_host: $prodHost");
    printStdOut ("  prod_port: " . (defined($prodPort) ? $prodPort : "default"));
    printStdOut ("  prod_db: $prodDb");
    printStdOut ("  prod_user: $prodUser");
    printStdOut ("  prod_pass: $prodPass");
  }
}

sub getMinePropsFile
{
    #perl -e '$a="/sfwuierf/wefjiweofj/wefjiweuof"; if ($a =~ m:.*/(.*):) { print "$1\n";}'
  my $mine_prps = $ENV{HOME};

    if (getcwd() =~ m:.*/(.*): ) {

      #$mine_prps .= "/$1.properties";
      $mine_prps .= "/$targetMine.properties";

      if (! -e $mine_prps) {
        die "Unable to find file $mine_prps";
     }
      if (! -r $mine_prps) {
        die "Unable to read file $mine_prps";
     }
      return $mine_prps;
    }
    else {
      die "Can't resolve the current mine dir\n";
    }
}
# Gets a value from a properties file.
# usage: get_prop_val prop_name file_name
sub get_prop_val
{
  my $key = shift;
  my $file = shift;

  open F, "$file" or die "cannot open $file: $!\n";

  my $ret_val;

  while (my $line = <F>) {
    if ($line =~ /$key=(.*)/) {
      $ret_val = $1;
    }
  }

  close F;

  return $ret_val;
}

sub getProjectElementsByTagName
{
  my $tagName = shift;
  return $doc->getElementsByTagName($tagName);
}

# NOTE: In the objectstore.xml there is a target alias 'dbmodel' which points to 'build-db'
# NOTE: Added another target to objectstore.xml 'analyse-db-production'
#
sub buildDbCommand
{
  my $dbCmd = $_[0];
  my $curDir = getcwd();
  my $dbModelDir = $curDir . $fsep . $dbmodel;

  if (-d $dbModelDir) {
    my @dbCmd;
    push @dbCmd, {"dir" => "dbmodel", "cmd" => "$antCmd $dbCmd", "tag" => "build-db", "dmp" => 0, "idx" => 0};
    return \@dbCmd;
  }
  else {
    die $dbModelDir. " is not a valid directory!\n";
  }
}

sub buildCommandsForTag {

  my $tagAlias = $_[0];
  my $stageDir = $_[1];
  my $cmdStub = $_[2];

  unless (defined $tagAlias and defined $cmdStub) {
    die "Need to supply both a tag name and a cmd stub!";
  }

  my $tags = getProjectElementsByTagName("$tagAlias");
  my @cmds;

  printStdOut("TAG: $tagAlias has this many occurances:" . $tags->getLength . "\n");

  for (my $i = 0; $i < $tags->getLength; $i++)
  {
    my $tag = $tags->item ($i);
    my $tagName = $tag->getAttributeNode ("name")->getValue;
    my $cmd = $antCmd . "$cmdStub$tagName";

    #Check to see if we have encountered a dump or index instruction.
    my $dump = $tag->getAttributeNode("dump");
    my $index = $tag->getAttributeNode("index");
    push @cmds, {"dir" => $stageDir, "cmd" => $cmd, "tag" => $tagName,
                 "dmp" => (defined $dump ? 1 : 0), "idx" => (defined $index ? 1 : 0) };
  }
  return \@cmds;
}

# Iterate over the command sets & execute them as appropriate.
# usage: executeCommands (%commands)
sub executeCommands {

  if(defined $dumpPrefix) {

    # Build our expected dump file name then check to see if it's valid
    my $restartDumpFile = "$dumpDataDir$fsep$targetMine.$dumpPrefix.dmp";
    unless(-s $restartDumpFile) {
        die "Restart Dump File $restartDumpFile was not found or is empty!";
    }

    my @cmdList = @{$_[0]};

    #Copy the cmd list first to avoid concurrent modification issues
    my @shortList;
    foreach my $hashRefTmp (@cmdList) {
      push @shortList, $hashRefTmp;
      my %hashTmpBlah = %$hashRefTmp;
      if ($verbose) {
        printStdOut("COPYING:" . $hashTmpBlah{"tag"});
        }
    }

    #Now loop over the commands looking for the tag we want to start from
    foreach my $hashRef (@cmdList) {
      my %hashtemp = %$hashRef;
      my $tag = $hashtemp{"tag"};
      if ($dumpPrefix eq $tag) {
        last;
      } else {
        #push unwanted elements off the 'shorter' list
        if ($verbose) {
            printStdOut("DITCHING TAG:$tag");
        }
        shift @shortList;
      }
    }

    unless (scalar(@shortList) >= 0) {
      die "Restart Dump Prefix $dumpPrefix does not match any known source or postprocess!";
    }

    iterateOverCommands(\@shortList);

  } else {
    iterateOverCommands($_[0]);
  }
}

# Iterates and executes the supplied set of ant commands
# param $cmdHashRef a reference to the set of commands
# param $cmdStage the sub stage we are at, i.e. integrate, used to locate the correct build.xml file
sub iterateOverCommands {

  my @cmdHashList = @{$_[0]};

  for my $hashRef (@cmdHashList) {

    my %hashtemp = %{$hashRef};

    my $dir = $hashtemp{"dir"};
    my $cmd = $hashtemp{"cmd"};
    my $tag = $hashtemp{"tag"};
    my $dmp = $hashtemp{"dmp"};
    my $idx = $hashtemp{"idx"};

    executeCommand($dir, $cmd, $dmp);

    if ($dmp) {
      my $prefix = $dir . "__" . $tag;
      dumpDatabase($prefix);
    }

    if ($idx) {
      my %cmd = %{shift @{buildDbCommand("analyse-db-production")}};
      executeCommand($cmd{"dir"},  $cmd{"cmd"}, $cmd{"dmp"});
    }
  }
}

# Executes a single command for each stage - either builddb, integrate or postprocess
# usage: executeCommand ($stage_name, $stage_command);
# note: The stage name is also used as the alias of the sub-directory that the supplied
#          command is to be run in.
sub executeCommand {
  my $dir = $_[0];
  my $cmd = $_[1];
  my $dmp = $_[2];

  if ($dir && $cmd)
  {
    my $curDir = getcwd();
    my $cmdDir = $curDir . $fsep . $dir;

    if (-d $cmdDir)
    {
      chdir($cmdDir);
      if ($dryRun) {
        printStdOut("Pretending todo: $cmd");
      }
      else {
        if ($verbose) {
          printStdOut(`date`);
        }
        open F, "$cmd |" or die "can't run $cmd: $?\n";
        if ($verbose) {
          while (<F>) {
            chomp;
            print STDOUT "  [$cmd] $_";
          }
          print STDOUT `date`, "\n\n";
          printStdOut("finished\n\n");
        }
        close F;
        if ($? != 0) {
          die "failed with exit code $?: @_\n";
        }
      }
      chdir($curDir);
    }
    else {
      die $cmdDir. " is not a valid exectution directory!\n";
    }
  }
  else {
    die "Error; Sub executeCommand must have a valid stage name and command to work!";
  }
}

# Dumps the database to disk.
# usage: dumpDatabase ($dumpStagePrefix)
sub dumpDatabase {
  my $dumpStagePrefix = shift;
  my $dumpFile = "$dumpDataDir$fsep$targetMine.$dumpPrefix.dmp";
  my @params = ('-U', $prodUser, '-h', $prodHost, '-W', '-f', $dumpFile, $prodDb);

  if (defined $prodPort) {
    unshift @params, "-p", $prodPort;
  }

  if ($dryRun) {
    printStdOut("Pretending todo: @dump_command @params");
  }
  else {

    if ($verbose) {
      printStdOut (`date`, "\n");
      printStdOut ("\ndumping: @dump_command @params\n");
    }

    my $exp = new Expect;
    $exp->raw_pty(1);
    $exp->spawn("ssh", $prodHost, "@dump_command @params; echo __DUMP_FINISHED__") or die "Cannot spawn @dump_command @params: $!\n";
    $exp->expect(10, 'Password: ');
    $exp->send("$prodPass\n");
    $exp->expect(9999999, '__DUMP_FINISHED__\n');
    $exp->soft_close();

    if ($verbose) {
      printStdOut (`date`, "\n");
      printStdOut ("finished dump\n");
    }
  }
}

sub load_db
{
  my $dumpFile = shift;

  my @params = ('-U', $prodUser, '-h', $prodHost, '-W', '-f', $dumpFile, $prodDb);

  if (defined $prodPort) {
    unshift @params, "-p", $prodPort;
  }

  printStdOut(`date`, "\n\n");
  printStdOut("\nloading: @psql_command @params\n");

  my $exp = new Expect;

  $exp->raw_pty(1);
  $exp->spawn("ssh", $prodHost, "@psql_command @params; echo __LOAD_FINISHED__")
    or die "Cannot spawn @psql_command @params: $!\n";

  $exp->expect(10, 'Password: ');
  $exp->send("$prodPass\n");
  $exp->expect(9999999, '__LOAD_FINISHED__\n');
  $exp->soft_close();

  printStdOut(`date`, "\n\n");
  printStdOut("finished load - now analysing\n\n");
  analyse_db_prod();
  printStdOut(`date`, "\n\n");
  printStdOut("finished analysing\n\n");
}

sub printStdOut
{
  my $message = shift;

  if ($verbose && defined($message)) {
    print STDOUT "$message\n";
  }
}
