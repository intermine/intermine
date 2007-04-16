#!/usr/bin/perl -w

use strict;

#### Config
my($RESDIR)="result";
my($SRCFILE)="tmp/FL100";

#### Arrays/Hashes/Variables
my(%countT)=(); # Templates
my(%countOD)=(); # ObjectDetails
my(%countBD)=(); # BagDetails
my(%countBU)=(); # BagUpload
my(%countDV)=(); # Data Volume

my(%uniqIP)=();
my(%countUIP);   # Unique IP

my(%countODNR)=(); # ObjectDetails - No Repeat
my(%countBDNR)=(); # BagDetails - No Repeat
my($fingerPrintOD)="~~~";
my($fingerPrintBD)="~~~";

my(%idxYM)=();

my(%Month)=("Jan"=>"01", "Feb"=>"02", "Mar"=>"03", "Apr"=>"04", "May"=>"05",
        "Jun"=>"06", "Jul"=>"07", "Aug"=>"08", "Sep"=>"09", "Oct"=>"10",
        "Nov"=>"11", "Dec"=>"12");

#### Functions
sub getYearMonth
{
    my($date)=@_;
    return substr($date,8,4)."-".$Month{substr($date,4,3)};
}

sub getUrl
{
    my($url)=@_;
    $url =~ s/%3D/=/ig;
    $url =~ s/%26/&/ig;
    $url =~ s/%20/~/ig;
    $url =~ s/ //g;
    return $url;
}


#### Reading loop
open(F,"<".$SRCFILE);
while (<F>)
{
    my(@f)=split(/ /);

    my($ip)=$f[0]; # 1st "field" is IP
    next if ($ip =~ /^131\.111\.146\.1/); # Remove training addresses

    my($yearMonth)=getYearMonth($f[3]); # 4th "field" is Date
    next if ($yearMonth =~ /^2005-/);

    my($url)=getUrl($f[6]); # 7th "field" is URL

    $idxYM{$yearMonth}++;

    #### Unique IPs
    my($idx)=$yearMonth." ".$ip;
    unless (defined $uniqIP{$idx})
    {
        $uniqIP{$idx}=1;
        $countUIP{$yearMonth}++;
    }

    #### Data Volume
    if (($url !~ /^\/dumps\//) && ($f[8] eq "200")
            && ($f[9] ne "-")) # 9th "field" is R. code
    {
        $countDV{$yearMonth}=0 unless (defined $countDV{$yearMonth});
        $countDV{$yearMonth}+=$f[9]; # 10th "field" is size
    }

    #### After this line only "/release-" URL are valid
    next unless (($f[5] =~ /^"(GE|POS)T/) && ($url =~ /^\/release-/));

    #### Templates
    if ($url =~ /template.do/)
    {
        next unless ($url =~ /type=global/);
        next unless ($url =~ /name=([^&]+)/);
        my($idx)=$yearMonth." ".$1;
        $countT{$idx}=0 unless (defined $countT{$idx});
        $countT{$idx}++;
    }
    #### Object Details
    elsif ($url =~ /objectDetails.do/)
    {
        next unless ($url =~ /id=([^&]+)/);
        $countODNR{$yearMonth}=$countOD{$yearMonth}=0 
            unless (defined $countOD{$yearMonth});
        $countOD{$yearMonth}++;

        next if ($ip.$yearMonth.$1 eq $fingerPrintOD);
        $fingerPrintOD=$ip.$yearMonth.$1;
        $countODNR{$yearMonth}++;
    }
    #### Bag Details
    elsif ($url =~ /bagDetails.do/)
    {
        next unless ($url =~ /bagName=([^&]+)/);
        $countBDNR{$yearMonth}=$countBD{$yearMonth}=0 
            unless (defined $countBD{$yearMonth});
        $countBD{$yearMonth}++;

        next if ($ip.$yearMonth.$1 eq $fingerPrintBD);
        $fingerPrintBD=$ip.$yearMonth.$1;
        $countBDNR{$yearMonth}++;
    }
    #### Bag uploads
    elsif ($url =~ /bagUploadConfirm.do/)
    {
        $countBU{$yearMonth}=0 unless (defined $countBU{$yearMonth});
        $countBU{$yearMonth}++;
    }
}
close(F);

#### Read Threshold (first month to recalculate)

my($threshold);

# Read threshold file
if (open(F,"<".$RESDIR."/THRESHOLD"))
{
    read F,$threshold,7;
    close(F);
}

undef $threshold if ((defined $threshold) && ($threshold !~ /^\d\d\d\d-\d\d/));

#### Publish results

my($lastMonth);
my($ym);
foreach $ym (sort keys %idxYM)
{
    $lastMonth=$ym;

    if ((defined $threshold) && ($ym lt $threshold))
    {
        print "Ignoring ".$ym." ( < ".$threshold.")\n";
        next;
    }

    print "Generating files for ".$ym."\n";

    #### Write counters files
    open(F,">".$RESDIR."/counters-".$ym.".txt");
    print F "Month: ".$ym."\n";
    print F "uniq IPs: ".$countUIP{$ym}."\n";
    printf F "Data volume (MB): %.0f\n",$countDV{$ym}/1024/1024;
    print F "objectDetails (hits): ".$countOD{$ym}."\n";
    print F "objectDetails (hits - no repeat): ".$countODNR{$ym}."\n";
    if (defined $countBD{$ym})
    {
        print F "bagDetails (hits): ".$countBD{$ym}."\n";
        print F "bagDetails (hits - no repeat): ".$countBDNR{$ym}."\n";
    }
    if (defined $countBU{$ym})
    {
        print F "bagUploads (hits): ".$countBU{$ym}."\n";
    }
    close(F);

    #### Write templates files
    open(F,">".$RESDIR."/templates-".$ym.".txt");
    print F "### Month: ".$ym."\n";
    foreach (sort {$countT{$b} <=> $countT{$a}} keys %countT)
    {
        print F $1." ".$countT{$_}."\n" if ($_ =~ /^$ym (.+)/);
    }
    close(F);
}

#### Write threshold file
open(F,">".$RESDIR."/THRESHOLD");
print F $lastMonth."\n";
close(F);
