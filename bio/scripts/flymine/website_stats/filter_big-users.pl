#!/usr/bin/perl -w

use strict;
use Socket;

#### Config
my($RESDIR)="result";
my($SRCFILE)="tmp/FL100";
my($THRESHOLD_VISITS)=10;
my($THRESHOLD_HITS_BY_VISIT)=5;

#### Arrays/Hashes/Variables
my(%visits)=();
my(%action)=();
my(@visitors)=();

my(%Month)=("Jan"=>"01", "Feb"=>"02", "Mar"=>"03", "Apr"=>"04", "May"=>"05",
        "Jun"=>"06", "Jul"=>"07", "Aug"=>"08", "Sep"=>"09", "Oct"=>"10",
        "Nov"=>"11", "Dec"=>"12");

#### Functions
sub getYearMonthDay
{
    my($date)=@_;
    return substr($date,8,4)."-".$Month{substr($date,4,3)}."-".substr($date,1,2);
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

sub getName
{
    my($ip)=@_;
    my(@res)=`jwhois -a $ip`;
    foreach (@res)
    {
        return $1 if (/^descr:\s+(.*)\n$/);
    }
    my($host)=gethostbyaddr(inet_aton($ip),AF_INET);
    return (defined $host)?
        "(".substr($host,rindex($host,".",rindex($host,".")-1)+1).")"
        : "";
}



#### Reading loop
open(F,"<".$SRCFILE);
while (<F>)
{
    my(@f)=split(/ /);

    my($ip)=$f[0]; # 1st "field" is IP
    next if ($ip =~ /^131\.111\.146\.1/); # Remove training addresses

    my($yearMonthDay)=getYearMonthDay($f[3]); # 4th "field" is Date
    next if ($yearMonthDay =~ /^200[56]-/);

    my($url)=getUrl($f[6]); # 7th "field" is URL
    $url =~ s/;jsessionid=[^\?]+//;
    $url =~ s/\?.*//;

    $visits{$ip}=() unless ($visits{$ip});
    $visits{$ip}{$yearMonthDay}++;

    if (($url =~ /\.do/) || ($url =~ /bin\/gbrowse/))
    {
        $action{$ip."-".$yearMonthDay}=() unless ($action{$ip."-".$yearMonthDay});
        $action{$ip."-".$yearMonthDay}{$url}=0 unless ($action{$ip."-".$yearMonthDay}{$url});
        $action{$ip."-".$yearMonthDay}{$url}++;
    }
}
close(F);

#### Compute & Publish results
open(F,">".$RESDIR."/big-users/report.txt");
print F "Biggest Users";

foreach (sort {(scalar keys %{$visits{$b}}) <=> (scalar keys %{$visits{$a}})} keys(%visits))
{
    my($ip)=$_;
    my($nb_visits)=scalar keys %{$visits{$ip}};
    next if ($nb_visits<$THRESHOLD_VISITS);

    my($name)=0;
    foreach (sort keys %{$visits{$ip}})
    {
        my($nb_hits)=$visits{$ip}{$_};
        next if ($nb_hits<$THRESHOLD_HITS_BY_VISIT);

        if ($name==0)
        {
            print F "\n\n--------------------\n";
            print F getName($ip)." [".$ip."]      ".$nb_visits." visits\n";
            $name=1;
        }

        print F "   On ".$_."   ".$nb_hits." hits\n";
        my($visit)=$ip."-".$_;
        foreach (sort {$action{$visit}{$b} <=> $action{$visit}{$a}} keys %{$action{$visit}})
        {
            print F "        > ".$_." : ".$action{$visit}{$_}."\n";
        }
    }
}
close(F);

