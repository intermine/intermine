#!/usr/bin/perl -w

use strict;

my @pages = ();

if (@ARGV != 3) {
  die <<USAGE;
Wrong number of argument

usage:
  $0 input_file "Some title" output_file_prefix
USAGE
}

my $help_file_name = $ARGV[0];
my $help_title = $ARGV[1];
my $prefix = $ARGV[2];

open my $help_file, '<', $help_file_name or die;

my $title;
my $text = "";

while (my $line = <$help_file>) {
  if ($line =~ m:<h2>(.*)</h2>:i) {
    if (defined $title) {
      save($title, $text);
      $text = "";
    }
    $title = $1;
  } else {
    $text .= $line;
  }
}

save($title, $text);

for (my $i = 0; $i < @pages; $i++) {
  $pages[$i]{num} = $i + 1;
}

sub save
{
  my ($title, $text) = @_;

#  warn "$title, $text\n";

  if ($title =~ m[(?:\d+\.\s+)?(.*)]i) {
    push @pages, {
                  title => $1,
                  text => $text,
                 }
  } else {
    die $title;
  }
}

sub make_name
{
  my $num = shift;
  return "${prefix}_$num.html";
}

for my $page (@pages) {
  my $num = $page->{num};
  my $title = $page->{title};
  my $text = $page->{text};

  open my $f, '>', make_name($num) or die;

  my $prev_url = '';
  my $prev_link = '';
  my $prev_title = '';
  if ($num > 1) {
    $prev_url = make_name($num - 1);
    $prev_link = qq[<a href="$prev_url">previous</a>];
    $prev_title = $pages[$num - 2]{title};
  }

  my $next_url = '';
  my $next_link = '';
  my $next_title = '';
  if ($num < @pages) {
    $next_url = make_name($num + 1);
    $next_link = qq[<a href="$next_url">next</a>];
    $next_title = $pages[$num]{title};
  }

  my $onclick = "";
#  if (length $next_url > 0) {
#    $onclick = qq[onclick="window.location.replace('$next_url');"];
#  }

  my $style_path = 'style';

  if (!-d "$style_path") {
    $style_path = "../$style_path";
    if (!-d "$style_path") {
      $style_path = "../$style_path";
    }
  }

  my $x_of_y = "$num/" . scalar(@pages);

  print $f <<"HTML";
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" style="padding: 0px">
  <head>
    <title>$help_title page $num - $title</title>
    <link media="screen,print" href="$style_path/base.css" type="text/css" rel="stylesheet" />
    <link media="screen,print" href="$style_path/branding.css" type="text/css" rel="stylesheet" />
    <meta content="text/html; charset=utf-8" http-equiv="Content-Type" />
  </head>
  <body>
    <div class="tour">
      <table width="100%">
        <tr>
          <td colspan="3" align="right">
            <span style="padding: 3px; font-size: 70%;" onclick="window.close()">close
              <img src="../images/close.png" title="Close" onmouseout="this.style.cursor='normal';" 
                   onmouseover="this.style.cursor='pointer';"/>
            </span>
          </td>
        </tr>
      </table>
      <div class="heading">
        <table width="100%">
          <tr>
            <td width="20%" align="left" valign="top">
$prev_link
            </td>
            <td width="60%" rowspan="2" align="center" valign="top">
              <span class="title">
$title &#160; &#160; ($x_of_y)
              </span>
            </td>
            <td width="20%" align="right" valign="top">
$next_link
            </td>
          </tr>
          <tr>
            <td width="20%" align="left" class="nextprev" valign="top">
$prev_title
            </td>
            <td width="20%" align="right" class="nextprev" valign="top">
$next_title
            </td>
          </tr>
        </table>
      </div>
      <div style="padding-top: 20px" class="content">
        <div $onclick>
$text
        </div>
      </div>
      <div class="heading">
        <table width="100%">
          <tr>
            <td width="20%" align="left" valign="top">
$prev_link
            </td>
            <td width="60%" rowspan="2" align="center" valign="top">
              <span class="title">
$x_of_y
              </span>
            </td>
            <td width="20%" align="right" valign="top">
$next_link
            </td>
          </tr>
          <tr>
            <td width="20%" align="left" class="nextprev" valign="top">
$prev_title
            </td>
            <td width="20%" align="right" class="nextprev" valign="top">
$next_title
            </td>
          </tr>
        </table>
      </div>
    </div>
  </body>
</html>
HTML

  close $f;

}
