#!/usr/bin/perl

use strict;
use warnings;

use InterMine::Model;
use InterMine::Item::Document;

@ARGV == 2 or die "Usage $0: model.xml output.xml\n";

my ($model_file, $output_file) = @ARGV;

my $model = InterMine::Model->new(file => $model_file);

my $doc = InterMine::Item::Document->new(
    model => $model,
    output => $output_file,
    auto_write => 1,
);

my @companies;

sub make_company {
    my ($comp_address, $sec_names, $comp_name, $ceo_title, $ceo_name, $departments, $manager_st, $title, 
        $mans, $is_euro, $grades, $emp_st) = @_;
    my $company_address = $doc->add_item(
        Address => (address => $comp_address));

    my @secs = map {$doc->add_item(Secretary => (name => $_))} @$sec_names;

    print "Making company $comp_name\n";

    my $company = $doc->add_item(
        Company => (
            name => $comp_name,
            vatNumber => int(rand(1_000_000)),
            address => $company_address,
            secretarys => [@secs],
    ));

    print "Making CEO $ceo_name\n";
    my $ceo = $doc->add_item(
        CEO => (
            salary => int(rand(10_000_000)),
            age => (int(rand(25)) + 50),
            name => $ceo_name,
            title => $ceo_title,
            company => $company,
            seniority => (int(rand(500_000)) + 500_000),
        ));

    my @deps = map {$doc->make_item(Department => (name => $_, company => $company))} @$departments;

    my $c = 0;
    print "Making managers\n";
    my @managers;
    for my $man (@$mans) {
        my $age = (30 + int(rand(40)));
        my $seniority = int(rand(10_000 * $age));
        push @managers, $doc->add_item(Manager => (
            name => $man, 
            department => $deps[$c++],
            age => $age,
            seniority => $seniority, 
            (($title) ? (title => $title) : ()),
            address => $doc->add_item(Address => (address => 
                ($is_euro) ? ($manager_st .' '. int(rand(100))) : (int(rand(100))  .' '. $manager_st))),
        ));
    }

    $c = 0;
    for my $manager (@managers) {
        $deps[$c++]->set(manager => $manager);
    }

    $doc->write(@deps);

    print "Making employees\n";
    my $d = 0;
    for my $grade (@$grades) {
        $c = 0;
        map {$doc->add_item(Employee => (
            name => $_,
            department => $deps[$c++],
            age => (25 + int(rand(40))),
            end => int(rand(10)),
            fullTime => (int(rand(6)) % 2 == 0) ? "true" : "false",
            address => $doc->add_item(Address => (address => 
                ($is_euro) ? $emp_st .' '. int(rand(100)) : int(rand(100))  .' '. $emp_st)),
            ))} @$grade;
    }
    push @companies, $company;
}

make_company(
    "13 Friendly St, Betjeman Industrial Park, Slough",
    ["Dawn Tinsley", "Sharron McKenzie", "Rebecca Willingham"],
    "Wernham-Hogg",
    "Mrs",
    "Jennifer Taylor-Clarke",
    ["Sales", "Accounting", "Warehouse", "Human Resources"],
    "Confusion Row",
    "Mr",
    ["David Brent", "Keith Bishop", "Glynn Williams", "Neil Godwin"],
    0,
    [
        ["Ricky", "Trudy", "Alex", "Anne"],
        ["Rachel", "Oliver", "Lee", "Carol"],
        ["Tim Canterbury", "Brenda", "Nathan"],
        ["Gareth Keenan", "Simon"],
        ["Malcolm", "Helena"]
    ],
    "Worker Av");

make_company(
    "Schamstraße 31, Unterlingen",
    [qw/Irene Magda Hildegard Maja/],
    "Capitol Versicherung AG",
    "Frau", 
    "Tatjana Berkel",
    ["Schadensregulierung A-L", "Schadensregulierung M-Z", "Schadensregulierung", "Verwaltung", "Archiven", "Kantine"],
    "Leiterstraße",
    "Herr",
    ["Sinan Turçulu", "Bernd Stromberg", "Timo Becker", "Dr. Stefan Heinemann", "Burkhardt Wutke", "Frank Möllers"],
    1,
    [
        ["Jennifer Schirrmann", "Sabine Buhrer", "Maja Decker", "Steffen Lambert", "Herr Hilpers", "Theo Hölter"],
        ["Tanja Seifert", "Herr Pötsch", "Suzanne Landsfried", "Prashant Prabhakar", "Herr Fritsche", "Hans Georg Althoff"],
        ["Ulf Steinke", "Lars Lehnhoff", "Josef Müller", "Kai Dörfler", "Ina", "Nyota N'ynagasongwa"],
        ["Berthold Heisterkamp", "Nicole Rückert", "Rita Klüver", "Magdalena Prellwitz", "Herr Kitter", "Herr Grahms"],
        ["Erika Burstedt", "Frank Montenbruck", "Andreas Hermann", "Hans Schmelzer", "Vannessa Klausen", "Jochen Schüler"],
    ],
    "Mitarbeiterstraße");

make_company(
    "1725 Slough Avenue, Scanton, Penn",
    [qw/Julie Susanne Siobhan Kelly Pam/],
    "Dunder-Mifflin",
    "Mr", 
    "Charles Miner",
    ["Sales", "Accounting", "Warehouse", "Human Resources"],
    "Cowboy St",
    undef,
    ["Michael Scott", "Angela", "Lonnis Collins", "Meredith Palmer"],
    0,
    [
        ["Jim Halpert", "Kevin Malone", "Madge Madsen", "Devon White"],
        ["Dwight Schrute", "Oscar Martinez", "Jerry DiCanio", "Josh Porter"],
        ["Andy Bernard", "Jo Bennet", "Michael", "Ed Truck"],
        ["Stanley Hudson", "Gabe Lewis", "Matt", "Dan Gore"],
        ["Phyllis Lapin-Vance", "Toby Flenderson", "Hidetoshi Takinawa", "Troy Undercrook"],
    ],
    "Worker Av.");

make_company(
    "Rue Papel 37, Villepinte",
    [qw/Antoinette Julie Claudette/, "Laetitia Kadiri"],
    "Gogirep",
    "Mme.", 
    "Juliette Lebrac",
    ["Sales", "Accounting", "Warehouse", "Human Resources"],
    "Rue Représentant",
    "M.",
    ["Gilles Triquet", "Jacques Plagnol Jacques", "Didier Leguélec", "Joel Liotard"],
    1,
    [
        ["Jennifer", "Stéphane", "Bernard Giraud", "Jean-Marc"],
        ["Delphine", "Corinne", "Fabienne de Dos", "Pascal"],
        ["Patrick", "Marie-Claude", "Vincent", "Nadège"],
        ["Nicole", "Karim", "Claude Gautier"],
        ["Fatou", "Olivier", "Emmanuelle"],
    ],
    "Av. Fatigue");

make_company(
    'Duraton, Land of Illegal Characters',
    ["Timmy, the hyper-active terrier", 'Jonno "Skippy", the Aussie Kangaroo'],
    "Difficulties Я Us",
    "Prof", 
    "Bwa'h Ha Ha",
    ["Quotes", "Separators", "Slashes", "XML Entities"],
    "",
    "M.",
    ["Quote Leader", "Separator Leader", "Slash Leader", "XML Leader"],
    1,
    [
        ['Single Double Quote "',     "Comma , here",                "Forward Slash /",            "Left Angle Bracket <", ],
        ['Double Double Quote "foo"', "Tab\there",                   "Double forward Slash //",    "Right angle bracket >",],
        ["Single Single Quote '",     "New line\nhere",              q{Backwards Slash \'},        'Ampersand &', ],
        ["Double Single Quote 'foo'",                                q{Double backwards slash \\}, 'Quot "',],
        [q{Both Quotes '"}],
    ],
    "Problem St.");


my $contractor_st = "PO BOX 3078";
my $personal_st = " Haven ln";
my $c = 0;
for my $name ("Rowan", "Ray", "Jude", "Walter Loermann", "Gernot Graf", "Vikram", "Wilson Brown") {
    my @now_companies;
    my @old_companies;
    for (@companies) {
        if (int(rand(10)) % 2) {
            push @now_companies, $_;
        } else {
            push @old_companies, $_;
        }
    }

    $doc->add_item(Contractor => (
            name => $name,
            businessAddress => $doc->add_item(Address => (address => $contractor_st . ++$c)),
            personalAddress => $doc->add_item(Address => (address => $c . $personal_st)),
            companys => [@now_companies],
            oldComs => [@old_companies],
            seniority => int(rand(1_000_000)),
        ));
}

$doc->close();
