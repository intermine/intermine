#!/usr/bin/perl -w

use strict;

my @pages = ();

if (@ARGV != 3) {
  die <<USAGE;
Wrong number of argument

usage:
  $0 input_file "Some title" destination_directory
USAGE
}

my $help_file_name = shift;
my $help_title = shift;
my $dest_dir = shift;

open my $help_file, '<', $help_file_name or die;

my $title;
my $id;
my $text = "";

while (my $line = <$help_file>) {
  if ($line =~ m@<h1(?:\s+id=[\'\"](\S+)[\'\"])?>(.*)</h1>@i) {
    if (defined $title) {
      save($id, $title, $text);
      $text = "";
      $id = undef;
    }
    $id = $1;
    $title = $2;
  } else {
    $text .= $line;
  }
}

save($id, $title, $text);

# split the text of a page into tabs
sub split_text
{
  my $re = qr/
     <div
     \s+
     id="([^\"]+)"
     \s+
     class="tabset_content">
     \s*
     <h2\s+class="tabset_label">(.*?)<\/h2>
     \s+
     (.*?)
     <\/div>
     \s*
     (?=<div
     \s+
     id="[^\"]+"
     \s+
     class="tabset_content">|\Z)
   /xs;

  my @tabs = ();

  while ($text =~ /$re/g) {
    push @tabs, { id => $1, title => $2, text => $3 };
  }

  return @tabs;
}

sub save
{
  my ($id, $title, $text) = @_;

  if ($title =~ m[(?:\d+\.\s+)?(.*)]i) {
    my @tabs = split_text $text;

    push @pages, {
                  title => $1,
                  text => $text,
                  id => $id,
                  tabs => \@tabs,
                 }
  } else {
    die $title;
  }
}

sub make_name
{
  my $page = shift;
  my $active_tab = shift;
  my $suffix = $page->{title};
  my $id = $page->{id};

  if (defined $id) {
    $suffix = $id;
  }

  if (defined $active_tab) {
    $suffix .= '-' . $active_tab->{id};
  }

  my $name = "$suffix.html";
  $name =~ s/\s/_/g;
  return $name;
}

sub make_html
{
  my $page = shift;
  my $num = shift;
  my $active_tab = shift;

  my $title = $page->{title};
  my $text = $page->{text};
  my $id = $page->{id};

  my $filename = "$dest_dir/" . make_name($page, $active_tab);
  open my $f, '>', $filename or die "Failed to create file: $filename";

  warn "generating: $filename\n";

  my $prev_url = '';
  my $prev_link = '';
  my $prev_title = '';
  if ($num > 0) {
    $prev_url = make_name($pages[$num - 1]);
    $prev_link = qq[<a href="$prev_url">previous</a>];
    $prev_title = $pages[$num - 1]{title};
  }

  my $next_url = '';
  my $next_link = '';
  my $next_title = '';
  if ($num < @pages - 1) {
    $next_url = make_name($pages[$num + 1]);
    $next_link = qq[<a href="$next_url">next</a>];
    $next_title = $pages[$num + 1]{title};
  }

  my $onclick = "";
  #  if (length $next_url > 0) {
  #    $onclick = qq[onclick="window.location.replace('$next_url');"];
  #  }

  my $style_path = 'style';
  my $js_path = 'js';

  if (!-d "$dest_dir/$style_path") {
    $style_path = "../$style_path";
    $js_path = "../$js_path";
    if (!-d "$dest_dir/$style_path") {
      $style_path = "../$style_path";
      $js_path = "../$js_path";
    }
  }

  my $display_page_num = $num + 1;

  my $x_of_y = $display_page_num . "/" . scalar(@pages);

  my @tabs = ();

  if ($page->{tabs}) {
    @tabs = @{$page->{tabs}};
  }

  my $tab_list = "";

  if (@tabs) {
    $tab_list = qq|<ul class="tabset_tabs">\n|;
    for (my $tab_idx = 0; $tab_idx < @tabs; $tab_idx++) {
      my $tab = $tabs[$tab_idx];
      my $tab_title = $tab->{title};
      my $tab_id = $tab->{id};
      if (!defined $active_tab && $tab_idx == 0 ||
          defined $active_tab && $tab_id eq $active_tab->{id} ) {
        $tab_list .= qq|<li><a href="#$tab_id" class="active">$tab_title</a></li>\n|;
      } else {
        $tab_list .= qq|<li><a href="#$tab_id">$tab_title</a></li>\n|;
      }
    }
    $tab_list .= "</ul>\n";

    $text = '';

    for my $tab (@tabs) {
      my $tab_title = $tab->{title};
      my $tab_id = $tab->{id};
      my $tab_text = $tab->{text};

      $text .= <<"TEXT";
<div id="$tab_id" class="tabset_content">
<h2 class="tabset_label">$tab_title</h2>
$tab_text
</div>
TEXT
    }
  }

  print $f <<"HTML";
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" style="padding: 0px">
  <head>
    <title>$help_title page $display_page_num - $title</title>
    <link media="screen,print" href="$style_path/genhelp.css" type="text/css" rel="stylesheet" />
    <link media="screen,print" href="$style_path/tabtastic.css" type="text/css" rel="stylesheet" />
    <script type="text/javascript" src="$js_path/addclasskillclass.js"></script>
    <script type="text/javascript" src="$js_path/attachevent.js"></script>
    <script type="text/javascript" src="$js_path/addcss.js"></script>
    <script type="text/javascript" src="$js_path/tabtastic.js"></script>
    <meta content="text/html; charset=utf-8" http-equiv="Content-Type" />
  </head>
  <body>
    <div class="genhelp">
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
$tab_list
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

for (my $num = 0; $num < @pages; $num++) {
  my $page = $pages[$num];

  make_html($page, $num);

  if (exists $page->{tabs}) {
    my @tabs = @{$page->{tabs}};
    for my $tab (@tabs) {
      make_html($page, $num, $tab);
    }
  }
}
