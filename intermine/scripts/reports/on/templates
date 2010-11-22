#!/usr/bin/perl

use strict;
use warnings;
use Carp;
use Storable;
use DateTime;
use File::Basename;
use constant {
    APP         => 'InterMine-TemplateComparer-0.1',
    REFRESH     => 'refresh',
    DATA_FILE   => '../data/templates',
    WANTED      => 'wanted',
    SUBMITTED   => 'submitted',
    DIFFERENCES => 'only_differences',
    CSS_FILE    => '../css/style.css',
    LOGO_FILE   => '../icons/intermine_logo.png',
};
use constant WANTED_VALUES => qw(
    all_constraints views editable_constraints
);
use constant WANTED_LABELS => (
    'All Constraints', 'Views', 'Editable Constraints'
);
use constant COL_HEADERS => (
    'All Constraints', 'View Columns', 'Editable Constraints'
);

use HTML::Table;
use CGI qw(:standard);
use LWP::Simple qw(get);

use List::MoreUtils qw(uniq zip);

use Webservice::InterMine;
use Webservice::InterMine::Service;

my ($changed, %link_to, %version_of, @services, %service_from);


my $order = sub {lc($a) cmp lc($b)};

#The different methods used in an analysis, and things that change depending on them
my %label_hash    = zip(@{[WANTED_VALUES]}, @{[WANTED_LABELS]});
my %col_header_for = zip(@{[WANTED_VALUES]}, @{[COL_HEADERS]});


sub make_table_from_rows {
    my ($rows_ref, $row_order) = @_;
    my @rows = @$rows_ref;
    my @columns = ($row_order)
	    ? @$row_order
        : sort($order keys %{$rows[0]});
    if (param('only_differences')) {
        @rows = grep {uniq(@{$_}{@columns[1 .. $#columns]}) > 1} @rows;
        return '' unless @rows;
    }
    my $table = HTML::Table->new( 
        -rows        => (@rows + 1), # data rows plus header 
        -cols        => (@columns + 0), # numify
        -class       => 'list',
        -oddrowclass => 'odd',
    );
    # Fill in the content of the table
    for my $col (1 .. @columns) {
        # Set headers
        $table->setCell(1, $col, strong($columns[$col -1]) );
        $table->setCellBGColor(1, $col, 'DarkGray');
        # Fill body
        for my $row (2 .. @rows + 1) {
            my $content = $rows[$row - 2]->{$columns[$col -1]};
            $table->setCell($row, $col, $content);
            if ($col == 1) {
                $table->setCellBGColor($row, $col, 'DarkSalmon') if ($content =~ /color:red/);
            }
        }
    }
    # Now colour it in to show the differences
    for my $row (2 .. @rows + 1) { # from the first data row to the end
        my $colour;
        if (uniq(grep {$_ !~ /^NO/} map {$table->getCell($row, $_)} (2 .. @columns)) > 1) {
            $colour = 'orange';
        }
        else {
            $colour = 'YellowGreen';
        }
        for my $col (2 .. @columns) {
            $table->setCellBGColor(
                $row, $col,
                (($table->getCell($row, $col) =~ /^NO/) ? 'OrangeRed' : $colour),
            );
        }
    }
    return $table;
}

my $data        = eval{retrieve(DATA_FILE)} || {};
my $now   = DateTime->now->set_time_zone('Europe/London');
my $todays_date = $now->ymd();

while (<DATA>) {
    chomp;
    next unless /^\w/;
    my ($mine, $url) = split(/\s/);
    $link_to{$mine} = $url;
}
@services = keys(%link_to);

unless ($data->{$todays_date} || param(REFRESH)) {

    my $todays_data = {};
    $todays_data->{update_time} = $now->time();
    # Get services from urls
    while (my ($mine, $url) = each %link_to) {
        $service_from{$mine} = eval{
            Webservice::InterMine->get_service($url.'/service');
        };
        next if $@;
        $todays_data->{version_of}->{$mine} = eval{
            $service_from{$mine}->release();
        }
    }

    # Get all the data from the templates
    my @templates;
    my %template_data;
    while (my ($service_name, $service) = each %service_from) {
        warn "getting templates for $service_name";
        for my $template (eval {$service->get_templates}) {
            push @templates, (my $title = $template->title);
            for my $wanted (WANTED_VALUES) {
                for my $value (map {(ref $_)?$_->to_string:$_} $template->$wanted) {
                    $template_data{$service_name}{$title}{$wanted}{$value}++;
                    $template_data{$title}{$wanted}{$value}++;
                }
            }
        }
    }
    $todays_data->{templates} = [uniq @templates];
    $todays_data->{template_data} = \%template_data;

    $data->{$todays_date} = $todays_data;

    # store it for later
    store($data, DATA_FILE);

}

### Write Service table
my %active = map 
    {($_ => (param('submitted'))
            ? defined param($_)
            : 1)}
    @services;
@services = grep {$active{$_}} @services;

my $service_table = HTML::Table->new();
while (my ($service, $url) = each %link_to) {
    $service_table->addRow(
 	checkbox(
        -name    => $service,
        -checked => $active{$service},
        -value   => 'ON',
        -label   => '',
	),
	strong(a({-href => $url},$service)),
	"Version: " . $data->{version_of}{$service});
}

# Parse the template for data, unless that's been done already
my @templates     = @{$data->{$todays_date}{templates}};
my %template_data = %{$data->{$todays_date}{template_data}};

my $wanted = 
    (param(SUBMITTED))
    ? param(WANTED)   # User's choice
    : (WANTED_VALUES)[0]; # default starting state

### Write main table
my @rows = map { {Template => $_} } uniq(sort {$a cmp $b} @templates);
for my $row (@rows) {
    my $title = $row->{Template};
    for my $service (@services) {
        # if there is an entry for what we want for this template for this service
        # put in the number of values recorded, or NONE, if none
        $row->{$service} = (exists $template_data{$service}{$title}{$wanted})
            ? scalar(keys %{$template_data{$service}{$title}{$wanted}})
            : 'NONE';
    }
    # Set internal Anchors
	$row->{Template} = strong(
        a({href  => '#'.$title,}, $title)
    );
}
unshift @rows, {map {($_ => scalar(keys %{$template_data{$_}}) || 'Total')} (Template => @services)}; # header row
my $row_order = ['Template', sort($order grep {$_ ne 'Template'} keys %{$rows[0]})];
my $overview_table = make_table_from_rows(\@rows, $row_order);

### Write tables analysing individual templates
my @template_tables;
my $col1_name = $col_header_for{$wanted};
for my $title (@templates) {
    my @rows  = map { {$col1_name => $_} } 
                    sort($order 
                        keys %{$template_data{$title}{$wanted}});
    next unless @rows;
    for my $row (@rows) {
        for my $service (@services) {
            $row->{$service} = 
                (exists $template_data{$service}{$title}{$wanted}{$row->{$col1_name}})
                    ? 'YES'
                    : 'NO';
        }
    }
    my $row_order = [
        $col1_name, 
        sort($order grep {$_ ne $col1_name} keys %{$rows[0]})
    ];
    my $template_table = make_table_from_rows(\@rows, $row_order);
    push (@template_tables, {name => $title, tbl => $template_table}) 
        if $template_table;
}

my $time = $data->{$todays_date}{update_time};
my $title = 'Template Comparison';

my $main_table_link = a({-href => '#maintable'}, 'main table');
my $sub_table_link  = a({-href => '#subtables'}, 'sub-table');
my $table = HTML::Table->new( -rows  => 1, 
                            -cols => 2,
                            -class => 'list',
                            -oddrowclass => 'odd');
$table->setCell(1, 1, 
    radio_group(
        -name      => WANTED,
        -values    => [WANTED_VALUES],
        -default   => 'get_all_constraints',
        -linebreak => 1,
        -labels    => \%label_hash,
    ) .
    checkbox(
        -name    => DIFFERENCES,
        -checked => 0,
        -value   => 'ON',
        -label   => 'Hide rows without differences',
    ) .
    checkbox(
        -name => REFRESH,
        -checked => 0,
        -value => 'ON',
        -label => 'Refresh template data',
    )
);
$table->setCell(1, 2,
	  $service_table,
);
print(
      header,
      start_html( -title => $title,
		  -style =>{src => CSS_FILE}),
      div({id => 'heading'},
	  div({id => 'banner'}, h1($title)),
	  img({
	       id => 'logo',
	       src => LOGO_FILE,
	       alt => 'Logo',
	       width => 600,
	       height => 75,
	      }),
	  div({class => 'clearall'}, ''),
	 ),
      div({id => 'content'},
	  div({id => 'menu'},
	      ul(li({-type => 'disc'},
		    [
		     a({-href => '/reports'}, 'All Reports'),
#		     a({-href => '/webreports/analytics'}, 'Google Analytics for Templates'),
		     a({-href => 'models.pl'},    'Model Comparison'),
		     a({-href => 'templates.pl'}, 'Template Collection Comparison'),
		    ]
		   )
		),
	     ),
	  br,
	  p({align => 'right'}, small(i("Last updated on $todays_date at $time"))),
	  p(<<'ENDQUOTE'),
This page has several tables to compare the templates used by different InterMine implementations. These templates were fetched from the respective webservices, and then analysed to create these tables. You can select which mines you would like to compare by using the checkboxes next to their names.
ENDQUOTE
	  p(<<"ENDQUOTE"),
The $main_table_link lists which templates are available from which mines. Where the template is absent, the word "NO" appears in a red box. If the template is present, then either the number of constraints declared in that template, or the number of columns in the output (the "view"), will appear in the cell, depending on the option selected. If the mines with this template all have a template with the same number of constraints or view-columns, then those mine's cells are green. If there is a difference in the number of constraints/view columns, then the cell is coloured orange. To filter out the uninteresting rows (ie. the ones where all the values are the same), you can choose to "hide rows without differences".
ENDQUOTE
	  p(<<"ENDQUOTE"),
To further investigate templates with differences you can click on the template name, which is a link that will take you to a $sub_table_link, showing which constraints are present or absent in a particular model. Only templates that are present in more than one model, and where at least one model declares constraints in that template, will have a sub-table, and thus a link. If a template is invalid in one of the mines, it will be highlighted in red, and holding the mouse over its name will bring up a box with a list of the specific problems with the template, preprepended with the mine in which the template is broken. This list of problems can also be viewed by visiting the individual table for this template.
ENDQUOTE
	  start_form,
	  br,
	  strong('Analyse differences in'),
	  br,
      $table,
	  submit(SUBMITTED, 'reanalyse'),
	  end_form,
	 ),
     );

if (@services) {
    print 
	h2(a({-name => 'maintable'}, $label_hash{$wanted} . ' of Templates By Service')),
	$overview_table,
	br,
	h2(a({-name => 'subtables'}, $label_hash{$wanted} . ' of Individual Templates By Model'));

    for my $table (@template_tables) {
    	print h2(a({-name => $table->{name}}, $table->{name}));
#	if ($problems_with{$table->{name}}) {
#	    print p(strong({style => 'color:red'}, join(', ', @{$problems_with{$table->{name}}})));
#	}
    	print $table->{tbl};
    }
}
else {
    print 
	h2('No services selected - please select a service');
}
print end_html;

__DATA__
# This should be a list of mines and their urls,
# specified such that adding '/service' to the end yields
# the webservice path
YeastMine http://dough.stanford.edu:8080/yeastmine-dev
#RatMine http://ratmine.mcw.edu/ratmine
Flymine http://preview.flymine.org/preview
ModMine http://intermine.modencode.org/query
ZfinMine http://zmine.cs.uoregon.edu:8080/dev-zfinmine
