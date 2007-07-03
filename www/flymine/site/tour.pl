#!/usr/bin/perl -w

use strict;

my @pages = ();

open my $tour_file, '<', 'tour.txt' or die;

my $title;
my $text = "";

while (my $line = <$tour_file>) {
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

  if ($title =~ m:\d+\.\s+(.*):i) {
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
  return "tour_$num.html";
}

for my $page (@pages) {
  my $num = $page->{num};
  my $title = $page->{title};
  my $text = $page->{text};

  open my $f, '>', make_name($num) or die;

  my $padding = 'padding-left:10px; padding-right:10px';

  my $prev_url = '';
  my $prev_link = '';
  my $prev_title = '';
  if ($num > 1) {
    $prev_url = make_name($num - 1);
    $prev_title = $pages[$num - 2]->{title};
    $prev_link = <<"HTML";
      <a href="$prev_url">previous</a>
HTML
  }

  my $next_url = '';
  my $next_link = '';
  my $next_title = '';
  if ($num < @pages) {
    $next_url = make_name($num + 1);
    $next_title = $pages[$num]->{title};
    $next_link = <<"HTML";
      <a href="$next_url">next</a>
HTML
  }

  my $onclick = "";
  if (length $next_url > 0) {
    $onclick = qq[onclick="window.location.replace('$next_url');"];
  }

  print $f <<"HTML";
<html>
  <head>
    <title>FlyMine Tour page $num - $title</title>
    <link media="screen,print" href="http://www.flymine.org/style/base.css" type="text/css" rel="stylesheet" />
    <link media="screen,print" href="http://www.flymine.org/style/branding.css" type="text/css" rel="stylesheet" />
    <meta content="text/html; charset=utf-8" http-equiv="Content-Type" />
  </head>
  <body>
    <table width="100%">
      <tr>
        <td align="left" valign="top" colspan="2">
          <span style="float:right; padding: 3px; font-size: 70%;"
                onmouseout="this.style.cursor='normal';"
                onmouseover="this.style.cursor='pointer';" onclick="window.close()">close
            <img src="images/close.png" title="Close" />
          </span>
        </td>
      <tr>
        <td align="left" valign="top" width="50%">
          $prev_link
        </td>
        <td align="right" valign="top" width="50%">
          $next_link
        </td>
      </tr>
      <tr>
        <td align="left" valign="top" width="50%" style="font-size: 60%">
          $prev_title
        </td>
        <td align="right" valign="top" width="50%" style="font-size: 60%">
          $next_title
        </td>
      </tr>
    </table>
    <div style="padding-top: 20px" class="box">
      <div style="font-size: 130%" class="heading2">
        $title
      </div>
      <div style="font-size: 130%" class="body">
        <div $onclick>
$text
        </div>
      </div>
    </div>
  </body>
</html>
HTML

  close $f;

}
