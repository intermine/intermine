#!/usr/bin/perl

use strict;
use warnings;

use feature ':5.10';

use InterMine::Model;
use InterMine::Item::Document;

use constant PI => 3.14159265359;

# SET-UP
my (@male_first_names, @female_first_names, @surnames, @dep_inital, @dep_medial, @dep_final);

my $current_list;

while (<DATA>) {
    chomp;
    my ($name) = split /\s/;
    next unless $name;
    if ($name eq "MALE_FIRST") {
        $current_list = \@male_first_names;
    } elsif ($name eq "FEMALE_NAMES") {
        $current_list = \@female_first_names;
    } elsif ($name eq "LAST_NAMES") {
        $current_list = \@surnames;
    } elsif ($name eq "DEP_INITIAL") {
        $current_list = \@dep_inital;
    } elsif ($name eq "DEP_MEDIAL") {
        $current_list = \@dep_medial;
    } elsif ($name eq "DEP_FINAL") {
        $current_list = \@dep_final;
    } else {
       push @$current_list, $name;
   }
}

sub get_random_dep_name {
    return sprintf "%s %s %s",
        $dep_inital[rand(@dep_inital)],
        $dep_medial[rand(@dep_medial)],
        $dep_final[rand(@dep_final)];
}

my @alphabet = ("A" .. "Z");

my @first_name_lists = (\@male_first_names, \@female_first_names);
my @title_lists = (["Mr"], [qw/Mrs Ms Miss/]);

my %allocated;

sub get_random_name {
    my $gender = shift // rand(@first_name_lists);
    my $first_names = $first_name_lists[$gender];
    my $new_name;
    do {
        $new_name = sprintf "%s %s. %s", 
            $first_names->[rand(@$first_names)],
            $alphabet[rand(@alphabet)],
            $surnames[rand(@surnames)];
    } while ($allocated{$new_name});
    return $new_name;
}

sub get_random_title_and_name {
    my $gender = shift // rand(@first_name_lists);
    my $titles = $title_lists[$gender];
    my $title = $titles->[rand(@$titles)];
    my $name = get_random_name($gender);
    return $title, $name;
}

# This function accepts two number (0 to 1) from a
# uniform distribution and returns two numbers from
# the standard normal distribution. (mean 0, variance 1)

sub normalise($$) {
    my $a = shift;
    my $b = shift;

    # Box-Muller Transformation
    my $x = sqrt(-2 * log($a)) * cos(2 * PI * $b);
    my $y = sqrt(-2 * log($a)) * sin(2 * PI * $b);

    return ($x,$y);
}

# convert the normal distribution to our 
# distribution for our sigma an mu

sub scale($$$) {
    my $x0 = shift;
    my $mean = shift;
    my $stdev = shift;

    my $x1 = $x0 * $stdev + $mean;

    return $x1;
}

sub norm_rand($$) {
    my ($mean, $stdev) = @_;
    my $a = rand(1);
    my $b = rand(1);
    my ($n_a, $n_b) = normalise($a, $b);
    
    return map {scale($_, $mean, $stdev)} ($n_a, $n_b);
}

sub get_employee_age {
    my @choices = norm_rand(45, 10);
    return int($choices[rand(@choices)]);
}

sub get_manager_age {
    my @choices = norm_rand(55, 6);
    return int($choices[rand(@choices)]);
}

sub get_ceo_age {
    my @choices = norm_rand(60, 4);
    return int($choices[rand(@choices)]);
}

sub get_manager_seniority($) {
    my $age = shift;
    my $seniority = ($age * 500) * (rand(2) + 1) + 20_000;
    return int($seniority);
}

sub get_ceo_seniority($) {
    my $age = shift;
    my $seniority = ($age ** 3) * (rand(1) + 1) + 40_000;
    return int($seniority);
}

sub get_ceo_salary($) {
    my $seniority = shift;
    return int((rand(1) + 1) * $seniority);
}

## MAIN LOGIC

@ARGV == 2 or die "Usage $0: model.xml output.xml\n";

my ($model_file, $output_file) = @ARGV;

my $model = InterMine::Model->new(file => $model_file);

my $doc = InterMine::Item::Document->new(
    model => $model,
    output => $output_file,
    auto_write => 1,
);

my $comp_address = "1 Enormo Way, Hugeville";
my @secretary_names = map {get_random_name(1)} 1 .. 300;
my $comp_name = "EnormoCorp";
my ($ceo_title, $ceo_name) = get_random_title_and_name();
my @dep_names = map {get_random_dep_name} 1 .. 500;
my $manager_st = "Enormo Cres.";
my $grades = 40;
my $emp_st = "Enormo. Avenue";

print "Making company $comp_name\n";

my $company_address = $doc->add_item(Address => (address => $comp_address));
my @secs = map {$doc->add_item(Secretary => (name => $_))} @secretary_names;

my $company = $doc->make_item(
    Company => (
        name => $comp_name,
        vatNumber => int(rand(1_000_000)),
        address => $company_address,
        secretarys => [@secs],
));

print "Making CEO $ceo_name\n";
my $ceo_age = get_ceo_age();
my $ceo_sen = get_ceo_seniority($ceo_age);
my $ceo_sal = get_ceo_salary($ceo_sen);

my $ceo_address = $doc->add_item(Address => (
    address => "PO Box " . int(rand(1000)) . ", Switzerland"
));

my $ceo_department = $doc->make_item(Department => (name => "Board of Directors"));

my $ceo = $doc->add_item(
    CEO => (
        salary => $ceo_sal, 
        age => $ceo_age, 
        name => $ceo_name, 
        title => $ceo_title, 
        company => $company, 
        seniority => $ceo_sen, 
        address => $ceo_address, 
        department => $ceo_department,
        end => int(rand(10)),
        fullTime => "true",
    )
);

$ceo_department->set(manager => $ceo);
$company->set(CEO => $ceo);

print "Writing company\n";

$doc->write($company, $ceo_department);

print "Making departments";

my @deps = map {$doc->make_item(Department => (name => $_, company => $company))} @dep_names;

print "Making managers\n";
my $c = 0;
my @managers;
for (my $i = 0; $i < @dep_names; $i++) {
    my $age = get_manager_age();
    my ($title, $name) = get_random_title_and_name();
    push @managers, $doc->add_item(Manager => (
        title => $title,
        name => $name, 
        department => $deps[$i],
        age => $age,
        seniority => get_manager_seniority($age),
        end => int(rand(10)),
        fullTime => (int(rand(10)) > 3) ? "true" : "false",
        address => $doc->add_item(Address => (address => (int(rand(5000)) . ' Enormo Cres.'))),
    ));
    $deps[$i]->set(manager => $managers[$i]);
}

print "Writing departments";
$doc->write(@deps);

print "Making employees\n";
for my $department (@deps) {
    for my $i (0 .. $grades) {
        $doc->add_item(Employee => (
            name => get_random_name(),
            department => $department,
            age => get_employee_age(),
            end => int(rand(10)),
            fullTime => (rand(10) > 4) ? "true" : "false",
            address => $doc->add_item(Address => (address => int(rand(10_000))  .' Enormo Av.')),
        ));
    }
}

__DATA__

MALE_FIRST
Michael (81005)
John (71521)
David (67848)
James (67656)
Robert (63091)
William (40144)
Mark (38265)
Richard (36837)
Thomas (31614)
Jeffrey (29569)
Joseph (28647)
Timothy (28331)
Kevin (28178)
Steven (27972)
Scott (25445)
Paul (24921)
Daniel (24825)
Christopher (24708)
Brian (24547)
Charles (23478)
Kenneth (22678)
Anthony (20325)
Gregory (18199)
Ronald (17627)
Eric (16827)
Donald (16036)
Gary (15292)
Stephen (14642)
Edward (14395)
Todd (13937)
Patrick (12941)
Douglas (12771)
Rodney (11064)
George (10687)
Keith (10680)
Matthew (9992)
Andrew (9681)
Larry (9416)
Peter (9390)
Terry (9094)
Jerry (9016)
Randy (8860)
Frank (8313)
Dennis (8214)
Raymond (8043)
Craig (7866)
Jeffery (7733)
Tony (6813)
Roger (6418)
Bruce (6320)
Mike (6034)
Darren (5925)
Troy (5856)
Carl (5800)
Steve (5678)
Danny (5670)
Ricky (5655)
Russell (5647)
Alan (5616)
Chris (5488)
Vincent (5420)
Jeff (5408)
Bryan (5343)
Gerald (5260)
Wayne (5119)
Lawrence (4956)
Martin (4936)
Shawn (4918)
Darryl (4916)
Phillip (4895)
Joe (4840)
Bradley (4789)
Randall (4753)
Jonathan (4722)
Johnny (4689)
Curtis (4688)
Billy (4651)
Jon (4600)
Sean (4592)
Bobby (4583)
Jimmy (4519)
Walter (4486)
Samuel (4395)
Dale (4394)
Glenn (4363)
Barry (4327)
Philip (4279)
Dean (4091)
Jose (3958)
Jay (3948)
Darrell (3889)
Allen (3830)
Henry (3804)
Willie (3801)
Roy (3789)
Arthur (3771)
Tim (3719)
Victor (3678)
Harold (3538)
Albert (3419)
Louis (3275)
Darrin (3253)
Ralph (3229)
Jack (3205)
Greg (3181)
Frederick (3156)
Ronnie (3152)
Marc (3054)
Marvin (2969)
Tracy (2931)
Jason (2893)
Kurt (2876)
Eddie (2865)
Joel (2860)
Stanley (2807)
Lee (2771)
Jim (2771)
Micheal (2758)
Tommy (2723)
Eugene (2716)
Leonard (2708)
Howard (2678)
Darin (2613)
Ernest (2575)
Adam (2561)
Dwayne (2523)
Tom (2502)
Reginald (2473)
Derrick (2416)
Aaron (2410)
Brent (2404)
Brett (2346)
Benjamin (2336)
Norman (2307)
Duane (2270)
Kelly (2260)
Rick (2253)
Melvin (2232)
Jesse (2213)
Juan (2189)
Fred (2150)
Theodore (2142)
Jerome (2139)
Carlos (2123)
Erik (2119)
Harry (2108)
Brad (2087)
Ray (2085)
Nicholas (2072)
Glen (2069)
Bill (2062)
Kirk (2054)
Calvin (2045)
Karl (2015)
Dan (1993)
Earl (1950)
Don (1912)
Edwin (1904)
Mitchell (1885)
Wesley (1867)
Kent (1845)
Warren (1822)
Clifford (1820)
Francis (1819)
Andre (1800)
Clarence (1799)
Derek (1798)
Antonio (1772)
Lance (1754)
Bernard (1675)
Alfred (1651)
Tyrone (1569)
Leroy (1536)
Luis (1518)
Manuel (1498)
Lonnie (1497)
Kerry (1476)
Gordon (1444)
Daryl (1428)
Maurice (1419)
Nathan (1417)
Leslie (1414)
Herbert (1378)
Marcus (1372)
Franklin (1371)
Perry (1364)
Guy (1359)
Vernon (1356)
Gilbert (1349)
Alvin (1327)
Alexander (1325)
Neil (1314)
Gene (1310)
Wade (1297)
Stuart (1297)
Lloyd (1295)
Mario (1290)
Ricardo (1279)
Leon (1258)
Alex (1243)
Ted (1240)
Gregg (1234)
Dwight (1232)
Byron (1225)
Dana (1219)
Marty (1218)
Kelvin (1196)
Chad (1179)
Andy (1175)
Kyle (1170)
Dave (1157)
Ruben (1152)
Ron (1144)
Joey (1120)
Lewis (1115)
Rickey (1114)
Kenny (1107)
Ken (1099)
Allan (1096)
Timmy (1092)
Ryan (1087)
Travis (1075)
Nathaniel (1068)
Roberto (1056)
Floyd (1053)
Terrence (1037)
Doug (1030)
Shane (1018)
Jesus (1014)
Bob (1000)
Roderick (998)
Oscar (991)
Ross (986)
Hector (970)
Donnie (969)
Charlie (964)
Miguel (952)
Lester (952)
Jimmie (948)
Terrance (946)
Arnold (945)
Jamie (944)
Shannon (939)
Roland (933)
Robin (921)
Clinton (920)
Leo (905)
Johnnie (888)
Jackie (886)
Neal (884)
Clayton (882)
Jessie (874)
Geoffrey (867)
Wendell (862)
Freddie (844)
Gerard (827)
Milton (826)
Fredrick (817)
Nelson (816)
Matt (800)
Raul (794)
Herman (794)
Rex (789)
Francisco (789)
Marshall (780)
Randolph (778)
Sam (774)
Clyde (770)
Adrian (770)
Terence (766)
Daren (761)
Ramon (748)
Bret (745)
Rene (744)
Cecil (730)
Ben (729)
Harvey (724)
Christian (723)
Damon (719)
Nick (719)
Angel (719)
Robbie (710)
Cedric (700)
Pedro (696)
Cary (690)
Randal (689)
Clifton (683)
Frankie (679)
Jaime (676)
Jody (676)
Angelo (658)
Hugh (657)
Everett (655)
Loren (654)
Jorge (654)
Sidney (641)
Ivan (636)
Carlton (635)
Chester (627)
Clark (625)
Gabriel (622)
Grant (621)
Sammy (620)
Rafael (616)
Kim (612)
Malcolm (610)
Stacy (605)
Claude (602)
Armando (600)
Edgar (600)
Rudy (593)
Chuck (592)
Felix (591)
Lorenzo (589)
Dewayne (584)
Bradford (582)
Orlando (582)
Curt (580)
Javier (574)
Salvatore (561)
Shaun (557)
Evan (556)
Garry (555)
Trent (552)
Fernando (545)
Wallace (539)
Bryant (535)
Alberto (527)
Teddy (523)
Eduardo (521)
Morris (518)
Pat (518)
Benny (516)
Scot (515)
Lynn (515)
Dexter (505)
Virgil (503)
Corey (503)
Darrel (501)
Dominic (499)
Myron (498)
Stacey (494)
Rod (494)
Bart (494)
Alfredo (489)
Ian (487)
Jacob (486)
Mickey (479)
Monte (476)
Toby (472)
Otis (471)
Joshua (462)
Julian (462)
Lyle (459)
Preston (457)
Justin (457)
Stewart (456)
Erick (456)
Spencer (455)
Colin (452)
Pete (449)
Cory (449)
Bennie (447)
Mathew (445)
Blake (444)
Earnest (443)
Arturo (443)
Casey (441)
Clay (440)
Rob (436)
Edmund (433)
Scotty (432)
Marion (430)
Kendall (430)
Isaac (426)
Alonzo (421)
Monty (420)
Max (419)
Kip (419)
Julius (416)
Willard (413)
Rusty (410)
Ira (410)
Drew (410)
Kris (408)
Van (407)
Sherman (407)
Stephan (405)
Clint (405)
Luke (402)
Jerald (402)
Alton (402)
Sheldon (398)
Brandon (398)
Luther (395)
Stevie (389)
Sylvester (389)
Vince (387)
Noel (385)
Alfonso (385)
Marco (384)
Jeffry (382)
Darwin (381)
Vance (380)
Elmer (375)
Rocky (373)
Julio (373)
Enrique (372)
Delbert (372)
Darron (370)
Laurence (365)
Ernesto (360)
Phil (358)
Leland (356)
Roosevelt (355)
Rory (354)
Robby (354)
Tracey (353)
Forrest (347)
Blaine (345)
Rodger (344)
Darnell (341)
Lamont (337)
Tod (336)
Tommie (330)
Rudolph (329)
Bryon (328)
Gerardo (326)
Lowell (323)
Cameron (323)
Seth (320)
Dallas (320)
Salvador (318)
Rodolfo (318)
Devin (316)
Doyle (312)
Archie (311)
Zachary (310)
Dominick (309)
Brendan (307)
Lyndon (306)
Ed (305)
Hubert (305)
Horace (304)
Sergio (302)
Oliver (300)
Ty (300)
Willis (298)
Tyler (298)
Dirk (298)
Ervin (296)
Gerry (295)
Rolando (292)
Miles (292)
Lionel (290)
Vaughn (289)
Grady (288)
Garrett (288)
Demetrius (287)
Dion (285)
Carey (285)
Boyd (285)
Alejandro (285)
Cornelius (284)
Gilberto (281)
Rickie (280)
Abel (279)
Jeremy (279)
Owen (273)
Abraham (273)
Mack (273)
Reynaldo (272)
Denis (272)
Dewey (271)
Wilbert (269)
Kurtis (269)
Freddy (268)
Ernie (264)
Bert (263)
Marlon (262)
Deron (262)
Buddy (260)
Israel (259)
Wilbur (254)
Elliott (254)
Guadalupe (252)
Louie (252)
Andres (252)
Wilson (249)
Ronny (243)
Marcos (243)
Terrell (242)
Daron (241)
Jonathon (238)
Scottie (238)
Elbert (238)
Lamar (237)
Denny (235)
Pablo (234)
Winston (233)
Homer (233)
Ellis (231)
Pierre (229)
Moses (229)
Rufus (227)
Conrad (227)
Amos (225)
Lane (225)
Tad (222)
Hans (222)
Joesph (222)
Royce (221)
Marlin (220)
Irvin (220)
Felipe (220)
Randell (219)
Cleveland (219)
Alphonso (219)
Cesar (219)
Jan (218)
Dane (216)
Sterling (215)
Kirby (215)
Reuben (214)
Dino (214)
Garland (213)
Trevor (212)
Donnell (212)
Al (211)
Sandy (210)
Danial (210)
Erich (209)
Edmond (208)
Morgan (207)
Austin (207)
Avery (206)
Johnathan (205)
Emanuel (205)
Michel (201)
Hal (200)
Ismael (200)
Jean (199)
Guillermo (199)
Blair (199)
Wilfred (197)
Sammie (197)
Will (196)
Percy (195)
Ramiro (195)
Emmett (194)
Tomas (193)
Aubrey (193)
Stan (193)
Richie (192)
Kraig (191)
Bryce (189)
Simon (189)
Brock (187)
Hank (186)
Kristopher (185)
Roman (185)
Quentin (184)
Gustavo (184)
Courtney (184)
Lorne (182)
Russ (182)
Jess (182)
Gino (181)
Ethan (181)
Donny (181)
Thaddeus (180)
Derick (180)
Jordan (179)
Wilfredo (178)
Kennith (177)
Carroll (177)
Lisa (176)
Elton (175)
Barton (174)
Domingo (173)
Cliff (173)
Ward (172)
Rocco (171)
Elias (170)
Eddy (170)
Jefferson (169)
Woodrow (167)
Saul (167)
Cody (167)
Harlan (167)
Rich (166)
Marcel (165)
Elliot (164)
Galen (163)
Damian (163)
Cornell (162)
Rogelio (162)
Nicky (162)
Fabian (162)
Billie (161)
Reggie (160)
Garth (160)
Antoine (160)
Burton (160)
Merle (159)
Lonny (159)
Gavin (159)
Carmen (159)
Shelton (157)
Rodrick (157)
Emilio (157)
Reed (157)
Efrain (157)
Quintin (156)
Elvis (156)
Xavier (154)
Stefan (153)
Norris (153)
Lon (153)
Russel (150)
Omar (150)
Junior (149)
Quinton (148)
Santiago (147)
Pernell (147)
Emil (147)
Bobbie (147)
Benito (146)
Brady (145)
Keven (145)
Kirt (144)
Gus (143)
Frederic (142)
Linwood (141)
Jake (141)
Solomon (140)
Santos (140)
Erwin (138)
Derwin (138)
Thurman (137)
Murray (137)
Thad (136)
Nicolas (136)
Eldon (136)
Mary (136)
Toney (133)
Emmanuel (133)
Mitch (132)
Donovan (132)
Lenny (132)
Darrick (131)
Kenton (130)
Mitchel (130)
Grover (130)
Barney (130)
Noe (128)
Ned (128)
Lincoln (128)
Jacques (126)
Duncan (126)
Bennett (126)
Errol (125)
Wiley (125)
Adolfo (125)
Leonardo (124)
Sonny (124)
Darius (124)
Harley (123)
Dwain (123)
Greggory (122)
Desmond (122)
Kennedy (121)
Elvin (121)
Vito (120)
Noah (120)
Kimberly (120)
Carlo (120)
Elijah (120)
Hugo (120)
Jasper (119)
Dannie (119)
Ashley (119)
Bradly (119)
Micah (118)
Bernie (118)
Andrea (118)
Ritchie (117)
Reid (117)
Eli (117)
Alec (117)
Kermit (116)
Jefferey (116)
Harrison (116)
Davis (116)
Edwardo (115)
Chet (114)
Carter (114)
Shelby (113)
Dante (113)
Anton (113)
Lanny (112)
Ignacio (112)
Nolan (111)
Norbert (111)
Lorin (111)
Irving (111)
Joaquin (111)
Humberto (111)
Everette (111)
Shayne (110)
Ulysses (110)
Kelley (110)
August (110)
Chip (109)
Loyd (108)
Jerold (108)
Heriberto (107)
Emery (107)
Antony (107)
Hiram (106)
Alexis (106)
Daryn (106)
Levi (105)
Quinn (105)
Devon (105)
Kory (105)
Coy (105)
Darien (105)
Del (105)
Sebastian (104)
Jackson (104)
Josh (103)
Sanford (102)
Zane (102)
Mikel (102)
Jerrold (102)
Myles (101)
Orville (101)
Jeremiah (101)
Esteban (101)
Wes (100)
Lenard (100)
Jared (100)
Dick (100)
Weldon (99)
Stanford (99)
Vicente (98)
Stoney (98)
Baron (98)
Brooks (98)
Trenton (97)
Dudley (97)
Lesley (96)
Timmie (96)
Wally (96)
Theron (96)
Raphael (95)
Garret (95)
Vern (94)
Timmothy (94)
Robb (94)
Augustine (94)
Carmine (94)
Les (93)
Eloy (93)
Charley (93)
Quincy (92)
Von (92)
Lindsey (92)
Burt (92)
Art (92)
Armand (92)
Raynard (91)
Roscoe (91)
Jayson (91)
Mervin (91)
Jarvis (91)
Erin (91)
Gil (91)
Cordell (91)
Silas (90)
Jamey (90)
Fidel (90)
Fredric (90)
Winfred (89)
Otto (89)
Emory (89)
Moises (89)
Ivory (88)
Graham (88)
Jude (88)
Carson (88)
Barron (88)
Odell (87)
Cyrus (87)
Agustin (87)
Gregorio (86)
Jonas (86)
Forest (86)
Hollis (86)
Felton (86)
Bruno (86)
Carmelo (86)
Whitney (85)
Lemuel (85)
Clement (85)
Layne (85)
Pasquale (84)
Brant (84)
Shon (83)
Tyron (83)
Napoleon (83)
Leif (83)
Basil (83)
Bernardo (83)
Titus (82)
Lars (82)
Andra (82)
Darryn (82)
Merlin (81)
Lucas (81)
Karen (81)
Berry (81)
Gale (81)
Taylor (80)
Donell (80)
Anderson (80)
Collin (80)
Major (79)
Fletcher (79)
Royal (78)
Parrish (78)
Raymundo (78)
Kipp (78)
Dusty (78)
Constantine (78)
Wyatt (77)
Johnie (77)
Freeman (77)
Denver (77)
Federico (77)
Millard (76)
Nickolas (76)
Len (76)
Aron (76)
Domenic (76)
Alden (76)
Delmar (76)
Regan (75)
Reinaldo (75)
Randel (75)
Trey (75)
Britt (75)
Emerson (75)
Dee (75)
Elwood (75)
Val (74)
Mac (74)
Judson (74)
Lindsay (74)
Barrett (74)
Houston (73)
Riley (73)
Mason (73)
Duwayne (73)
Bud (73)
Darryle (73)
Raleigh (72)
Landon (72)
Deon (72)
Brice (72)
Trace (71)
Fritz (71)
Adan (71)
Brien (71)
Hoyt (70)
Malcom (70)
Merrill (70)
Harris (70)
Aldo (70)
Duke (70)
Donn (70)
Drake (70)
Markus (69)
Tobias (69)
Leonel (69)
Issac (69)
Dwaine (69)
Cris (69)
Monroe (68)
Roderic (68)
Keenan (68)
Rhett (68)
Hunter (68)
Adolph (68)
Blane (68)
Dwane (68)
Isaiah (67)
Maynard (67)
Roddy (67)
Stanton (67)
Gonzalo (67)
Farrell (67)
Artie (67)
Darian (67)
Faron (67)
Kennth (66)
Ollie (66)
Josef (66)
Truman (66)
Roel (66)
Cole (66)
Jed (66)
Andreas (66)
Augustus (66)
Buford (66)
Alfonzo (66)
Maxwell (65)
Rolf (65)
Kendrick (65)
Benedict (65)
Diego (65)
Booker (65)
Derrell (64)
Douglass (64)
Darry (64)
Cruz (64)
Mel (63)
Linda (63)
Patricia (63)
Alvaro (63)
Daryle (63)
Genaro (63)
Sal (62)
Dario (62)
Butch (62)
Bertram (62)
Mauricio (61)
Lacy (61)
Rogers (61)
Renard (61)
Deric (61)
Chadwick (61)
Emmitt (61)
Darell (61)
Maury (60)
Paris (60)
Parker (60)
Leigh (60)
Lupe (60)
Cynthia (60)
Darcy (60)
Chauncey (60)
Haywood (60)
Reese (59)
Terance (59)
Vinson (59)
Raymon (59)
Thor (59)
Brain (59)
Cyril (59)
Michale (59)
Ezra (59)
Kieth (59)
Marcellus (59)
Jacky (58)
Wilford (58)
Reginal (58)
Mckinley (58)
Fitzgerald (58)
Dennie (58)
Darryll (58)
Dorian (58)
Lennie (57)
Irwin (57)
Marcelino (57)
Rodrigo (57)
Hershel (57)
Montgomery (57)
Quint (57)
Carol (57)
Giovanni (57)
Darold (57)
Clair (57)
Elroy (57)
Edgardo (57)
Rayford (56)
Woody (56)
Milo (56)
Stephon (56)
Elizabeth (56)
Brenda (56)
Broderick (56)
Darvin (56)
Isidro (55)
Franz (55)
Arnulfo (55)
Cletus (54)
Boris (54)
Boyce (54)

FEMALE_NAMES
Lisa (60233)
Mary (34288)
Karen (32875)
Kimberly (28828)
Susan (26321)
Patricia (23546)
Donna (19700)
Linda (19346)
Cynthia (19250)
Angela (18734)
Tammy (18006)
Pamela (17471)
Deborah (17070)
Julie (16979)
Sandra (16377)
Elizabeth (16314)
Laura (16222)
Michelle (16205)
Lori (15697)
Jennifer (15201)
Christine (15184)
Sharon (15138)
Brenda (15059)
Teresa (14568)
Barbara (14043)
Dawn (13331)
Debra (13094)
Denise (13023)
Tina (12808)
Kelly (12753)
Cheryl (12551)
Nancy (12254)
Robin (11991)
Kathleen (11790)
Amy (11528)
Tracy (11168)
Rhonda (10944)
Melissa (10564)
Wendy (10478)
Diane (10359)
Rebecca (10213)
Carol (10104)
Kim (10077)
Stephanie (9766)
Theresa (9680)
Maria (9450)
Jacqueline (9426)
Michele (8664)
Paula (7904)
Sheila (7903)
Jill (7818)
Margaret (7796)
Cindy (7775)
Kathy (7763)
Janet (7658)
Sherry (7381)
Catherine (7250)
Carolyn (6971)
Ann (6783)
Laurie (6533)
Connie (6157)
Debbie (6087)
Andrea (6080)
Diana (5929)
Suzanne (5910)
Beth (5773)
Valerie (5651)
Renee (5497)
Terri (5385)
Gina (5270)
Annette (5262)
Monica (5097)
Janice (5034)
Christina (4969)
Leslie (4872)
Carla (4777)
Dana (4727)
Anne (4718)
Katherine (4707)
Lynn (4664)
Wanda (4655)
Regina (4548)
Darlene (4360)
Stacey (4266)
Tracey (4254)
Judy (4248)
Kathryn (4158)
Cathy (4150)
Joyce (4128)
Bonnie (4126)
Sherri (4065)
Sarah (4055)
Colleen (4027)
Anita (4012)
Jane (3935)
Anna (3915)
Penny (3897)
Shelly (3850)
Martha (3802)
Melinda (3798)
Beverly (3776)
Judith (3587)
Betty (3570)
Tamara (3556)
Heidi (3556)
Marie (3488)
Maureen (3482)
Victoria (3476)
Gloria (3435)
Deanna (3402)
Shirley (3379)
Jean (3364)
Holly (3353)
Vicki (3333)
Virginia (3302)
Joan (3286)
Ellen (3252)
Ruth (3237)
Stacy (3203)
Kristine (3160)
Joanne (3150)
Melanie (3117)
Peggy (3109)
Julia (3098)
Veronica (3075)
Rita (3062)
Jodi (3055)
Allison (3043)
Tonya (2964)
Dorothy (2963)
Sylvia (2957)
Yvonne (2953)
Rose (2867)
Gail (2856)
Kristin (2830)
April (2810)
Helen (2802)
Vickie (2787)
Yolanda (2763)
Jamie (2746)
Sheri (2627)
Charlene (2591)
Becky (2589)
Alice (2569)
Shannon (2566)
Sheryl (2536)
Crystal (2536)
Shelley (2528)
Elaine (2496)
Heather (2493)
Marilyn (2491)
Carrie (2481)
Charlotte (2474)
Phyllis (2471)
Yvette (2462)
Jackie (2442)
Eileen (2417)
Joann (2395)
Alicia (2353)
Sonya (2352)
Toni (2329)
Sally (2320)
Frances (2267)
Felicia (2190)
Jeanne (2152)
Carmen (2142)
Roberta (2131)
Traci (2078)
Jeanette (2066)
Lorraine (2059)
Norma (2048)
Belinda (2028)
Tanya (2019)
Terry (2019)
Rachel (2014)
Evelyn (2006)
Joy (1975)
Sara (1973)
Samantha (1955)
Natalie (1942)
Ronda (1915)
Shelia (1889)
Jo (1852)
Shari (1842)
Loretta (1836)
Constance (1833)
Caroline (1815)
Karla (1803)
Kimberley (1800)
Sue (1775)
Kristen (1727)
Tami (1720)
Sandy (1707)
Gwendolyn (1701)
Angie (1694)
Tammie (1693)
Patty (1668)
Melody (1665)
Juanita (1665)
Audrey (1656)
Amanda (1650)
Dianne (1634)
Vanessa (1629)
Doris (1627)
Monique (1623)
Kristi (1608)
Lynda (1605)
Darla (1583)
Jody (1569)
Cassandra (1562)
Marcia (1551)
Irene (1550)
Kelli (1543)
Bridget (1539)
Marsha (1537)
Jessica (1528)
Glenda (1516)
Doreen (1511)
Lynne (1503)
Erin (1464)
Kelley (1463)
Teri (1461)
Alison (1457)
Shawn (1449)
Lynette (1408)
Rosa (1403)
Robyn (1393)
Leah (1390)
Lora (1388)
Kristina (1373)
Vicky (1364)
Janine (1357)
Eva (1332)
Patti (1314)
Lauren (1305)
Danielle (1304)
Lee (1298)
Katrina (1287)
Dianna (1286)
Emily (1285)
Rosemary (1279)
Roxanne (1272)
Ramona (1270)
Vivian (1254)
Sonia (1253)
Pam (1217)
Marlene (1214)
Lois (1207)
Sherrie (1206)
Tara (1169)
Kellie (1169)
Hope (1155)
Marianne (1146)
Christy (1128)
Sonja (1120)
Grace (1112)
Ginger (1109)
Jacquelyn (1103)
Tracie (1095)
Jana (1093)
Vonda (1092)
Chris (1088)
Lydia (1077)
Nina (1061)
Molly (1059)
Mia (1052)
Arlene (1052)
June (1052)
Lorie (1030)
Lillian (1022)
Kay (1021)
Trina (1019)
Antoinette (996)
Ruby (988)
Cara (988)
Jenny (983)
Kris (977)
Marjorie (971)
Candace (971)
Esther (963)
Gayle (953)
Kari (949)
Priscilla (943)
Louise (942)
Jan (938)
Angelia (937)
Betsy (932)
Alisa (923)
Kerry (921)
Karin (918)
Geraldine (914)
Bernadette (909)
Joanna (906)
Bobbie (905)
Gretchen (902)
Rochelle (901)
Marla (901)
Carole (898)
Nicole (895)
Edith (890)
Cathleen (885)
Debora (879)
Nora (878)
Cheri (874)
Mona (872)
Pauline (871)
Lucy (869)
Leigh (865)
Josephine (862)
Desiree (856)
Dolores (831)
Dina (828)
Cherie (824)
Maryann (819)
Annie (817)
Claudia (816)
Ana (811)
Dena (811)
Kara (800)
Sabrina (789)
Leticia (786)
Lana (780)
Kristy (779)
Nadine (774)
Lorrie (759)
Trisha (757)
Miriam (755)
Krista (751)
Mildred (749)
Lesley (743)
Leanne (738)
Delores (734)
Cecilia (733)
Dora (728)
Irma (726)
Billie (724)
Georgia (719)
Therese (719)
Joni (717)
Edna (711)
Candy (711)
Paulette (708)
Faith (705)
Patrice (704)
Lesa (699)
Jeannette (695)
Margie (694)
Adrienne (694)
Rosemarie (693)
Francine (690)
Jodie (669)
Jeanine (666)
Celeste (660)
Suzette (658)
Rosalind (648)
Kirsten (641)
Myra (639)
Emma (637)
Daphne (637)
Beatrice (636)
Rene (634)
Tammi (625)
Bonita (624)
Terrie (622)
Stacie (622)
Janie (620)
Leann (618)
Lucinda (614)
Elisa (613)
Megan (610)
Eleanor (607)
Kerri (606)
Susie (600)
Marian (600)
Marcella (589)
Susanne (588)
Trudy (583)
Gladys (581)
Naomi (580)
Sondra (577)
Thelma (577)
Bobbi (577)
Jeannine (577)
Patsy (576)
Deanne (575)
Marion (568)
Clara (568)
Erica (566)
Tiffany (564)
Jennie (561)
Bernice (561)
Kecia (559)
Ladonna (556)
Tricia (554)
Bertha (554)
Jeannie (551)
Shawna (549)
Alma (548)
Darcy (544)
Deirdre (543)
Deana (539)
Vera (535)
Laurel (524)
Claire (524)
Amber (524)
Nanette (523)
Paige (521)
Ida (519)
Lena (518)
Staci (515)
Karyn (511)
Stella (509)
Lorna (508)
Lauri (508)
Iris (507)
Sophia (506)
Valarie (497)
Bridgette (496)
Margarita (496)
Ingrid (494)
Caryn (491)
Misty (490)
Tonia (490)
Jayne (489)
Tamera (486)
Janette (486)
Gwen (484)
Kimberlee (482)
Ella (479)
Marcy (478)
Amelia (475)
Dee (474)
Jessie (473)
Janelle (472)
Jeri (472)
Christie (469)
Rosie (467)
Madeline (465)
Julianne (463)
Wendi (458)
Elisabeth (457)
Danette (457)
Elena (457)
Kendra (453)
Allyson (453)
Mindy (452)
Jerri (452)
Jolene (451)
Katie (450)
Florence (449)
Polly (447)
Colette (447)
Camille (447)
Rena (446)
Lea (442)
Ethel (441)
Olivia (439)
Noreen (434)
Kristie (433)
Lynnette (428)
Janis (426)
Lorri (425)
Maxine (424)
Deann (423)
Deneen (422)
Erika (417)
Angelina (417)
Guadalupe (416)
Lucille (413)
Alesia (408)
Marybeth (407)
Annmarie (405)
Corinne (405)
Angel (403)
Marci (398)
Diann (398)
Wilma (396)
Tamra (394)
Olga (393)
Shellie (391)
Deidre (390)
Della (388)
Bethany (386)
Maura (385)
Benita (378)
Hilda (378)
Rachelle (378)
Christi (378)
Lillie (377)
Johanna (375)
Elise (375)
Melisa (373)
Malinda (371)
Cora (370)
Marina (368)
Felecia (367)
Velma (364)
Zina (363)
Delia (363)
Marguerite (361)
Eugenia (360)
Tonja (358)
Celia (356)
Ursula (355)
Michael (355)
Marta (352)
Mitzi (351)
Leona (349)
Luann (346)
Liza (345)
Meredith (345)
Deena (345)
Robbin (344)
Lorena (344)
Hazel (343)
Jenifer (340)
Coleen (339)
Lenora (339)
Margo (338)
Faye (338)
Daisy (337)
Willie (336)
Gayla (332)
Bridgett (332)
Stefanie (331)
Cecelia (331)
Jami (327)
Isabel (326)
Cristina (325)
Gena (325)
Keri (324)
Latonya (319)
Justine (319)
Randi (317)
Rosalie (317)
Shauna (316)
Rae (315)
Renae (313)
Shelli (311)
Roseann (308)
Luz (306)
Alexandra (305)
Rosalyn (304)
Gay (303)
Mari (302)
James (302)
Janna (302)
Marisa (301)
Leeann (301)
Geneva (298)
Whitney (296)
Robbie (296)
Silvia (295)
Aimee (294)
Judi (293)
Annemarie (293)
Jocelyn (292)
Laverne (291)
Jacquline (290)
Harriet (290)
Katharine (288)
Lourdes (287)
Janell (287)
Brigitte (287)
Helene (287)
Tamela (285)
Chandra (284)
Saundra (282)
Martina (282)
Letitia (282)
Roslyn (281)
Frankie (281)
Lola (278)
Antonia (278)
Lesia (278)
Verna (274)
Shelby (273)
Selena (273)
Elsa (273)
Marcie (272)
Bessie (272)
Christa (270)
Ada (269)
Marnie (267)
Shana (265)
Debby (265)
Maryellen (265)
Tania (264)
Penelope (264)
Denice (263)
Maryanne (262)
Freda (262)
Sandi (261)
Rosalinda (260)
Pearl (260)
Agnes (260)
Helena (260)
Mattie (259)
Deloris (259)
Corina (259)
Dale (259)
Caren (258)
Candice (258)
Greta (257)
Danita (257)
Ernestine (257)
Robert (256)
Kathi (255)
Eve (255)
Leisa (253)
John (253)
Aileen (253)
Courtney (253)
Elsie (252)
Viola (250)
Lucia (250)
Patrica (249)
Maritza (248)
Hilary (248)
Angelica (247)
Deidra (247)
Raquel (246)
Eunice (245)
Selina (244)
Blanca (244)
Charmaine (243)
Juli (242)
Dionne (242)
Dayna (241)
Cindi (241)
Rosanne (240)
Rebekah (240)
Johnnie (239)
Carlene (239)
Genevieve (239)
Liz (237)
Adriana (236)
Marisol (235)
Tena (234)
Myrna (234)
Gale (234)
Barbra (234)
Sharron (233)
Nikki (233)
Dawna (233)
Maribel (230)
Valencia (229)
Claudette (229)
Latanya (227)
Noelle (227)
Ava (227)
Lolita (226)
Lawanda (226)
Cari (226)
Simone (225)
Johnna (225)
Lou (224)
Rhoda (224)
Tana (223)
Alberta (223)
Sharlene (222)
Laureen (222)
Abby (222)
Jeanie (222)
Missy (220)
Gabrielle (220)
David (218)
Teressa (217)
Ashley (217)
Lorene (217)
Rachael (216)
Roxann (214)
Juliana (214)
Althea (214)
Dorothea (213)
Violet (212)
Valeria (211)
Melodie (211)
Dixie (209)
Abigail (209)
Geri (208)
Pennie (208)
Kathie (208)
Minnie (207)
Gracie (206)
Flora (206)
Juliet (205)
Susanna (204)
Maribeth (203)
Rosetta (202)
Pamala (202)
Kate (202)
Graciela (202)
Tresa (201)
Renita (201)
Nellie (201)
Reba (201)
Clarissa (200)
Sallie (199)
Ivy (198)
Dona (197)
Sharla (197)
Danna (197)
Suzan (194)
Mara (194)
Alecia (194)
Starla (193)
Shanna (193)
Sheree (193)
Edwina (193)
Carolina (193)
Kandy (193)
Vikki (192)
Dorene (192)
Alana (191)
Leanna (190)
Arleen (190)
Carmela (190)
Mae (189)
Luanne (189)
Estella (188)
Roxane (188)
Kathrine (188)
Rosanna (187)
Nannette (187)
Trena (186)
Susana (186)
Letha (186)
Adele (186)
Bettina (186)
Mandy (185)
Jewel (185)
Ivette (185)
Ilene (185)
Teena (184)
Alyson (184)
Kerrie (184)
Lupe (183)
Karrie (183)
Lavonne (182)
Libby (182)
Jeanna (182)
Clare (182)
Karol (181)
Serena (181)
Josie (181)
Roxanna (180)
Terese (179)
Corrine (179)
Krystal (178)
Aida (178)
Aurora (178)
Evette (177)
Milagros (175)
Maggie (174)
Georgette (173)
Bernadine (172)
Kaye (171)
Antionette (171)
Elvira (171)
Sydney (170)
Valorie (169)
Lorinda (168)
Inez (168)
Cathryn (168)
Venus (167)
Magdalena (167)
Sarita (166)
Mercedes (166)
Lila (166)
Hattie (166)
Charla (165)
Maryjo (165)
Alexis (165)
Twila (164)
Loriann (164)
Glenna (164)
Merry (163)
Georgina (163)
Jonna (163)
Tia (162)
Pat (162)
Lyn (161)
Cassie (159)
Camilla (159)
Nita (158)
Alisha (157)
Vickey (156)
Esmeralda (156)
Lenore (156)
William (155)
Machelle (155)
Fay (155)
Dolly (155)
Petra (154)
Minerva (154)
Angelita (154)
Dedra (154)
Sybil (153)
Glynis (153)
Annamarie (153)
Corinna (153)
Henrietta (152)
Gerri (152)
Elva (152)
Portia (151)
Lucretia (151)
Hollie (151)
Erma (151)
Kandi (150)
Ruthie (150)
Carey (150)
Alfreda (150)
Dara (149)
Anastasia (149)
Barb (149)
Lela (147)
Kayla (147)
Aretha (147)
Chrystal (147)
Melba (146)
Louisa (146)
Candi (146)
Beatriz (146)
Brooke (146)
Shiela (145)
Renea (145)
Juliann (145)
Delisa (145)
Mollie (144)
Pattie (144)
Moira (144)
Carmella (143)
Millie (142)
Zena (142)
Juana (142)
Dinah (142)
Mamie (142)
Rosario (141)
Dori (141)
Mimi (140)
Jeana (140)
Katy (140)
Gigi (140)
Cherry (140)
Stacia (139)
Trudi (139)
Miranda (139)
Aleta (139)
Michell (139)
Lanette (139)
Migdalia (138)
Brigette (138)
Jacquelin (138)
Roseanne (137)
Maricela (137)
Eloise (137)
Sherie (136)
Joellen (136)
Richard (135)
Leila (135)
Mariann (134)
Madonna (134)
Kimberlie (134)
Darleen (133)
Kimberli (133)
Consuelo (133)
Fannie (133)
Angelique (133)
Charles (133)
Collette (133)
Tonda (132)
Loraine (132)
Janeen (131)
Suzy (130)
Tori (130)
Nanci (130)
Gaye (130)
Dorinda (130)
Ina (130)
Karon (130)
Penni (129)
Nelda (129)
Fonda (128)
Etta (128)
Marylou (127)
Noemi (127)
Gabriela (127)
Tamie (127)
Edie (127)
Athena (127)
Donita (127)
Marni (126)
Sharyn (126)
Richelle (126)
Malissa (126)
Lesli (126)
Casandra (126)
Deedee (126)
Kitty (126)
Monika (125)
Josefina (125)
Marva (125)
Tangela (124)
Hannah (124)
Elaina (124)
Darci (124)
Dawne (124)
Kimberely (123)
Ronna (123)
Amie (123)
Cherri (123)
Lilly (122)
Francis (122)
Lizabeth (121)
Kendall (121)
Treva (121)
Leesa (121)
Esperanza (121)
Cyndi (121)
Carleen (121)
Barbie (121)
Meg (120)
Shanda (120)
Twyla (120)
Mark (120)
Lavonda (120)
Linette (120)
Charleen (120)
Charisse (120)
Demetria (120)
Suzanna (119)
Risa (119)
Tawana (119)
Thea (119)
Natasha (119)
Alyssa (119)
Avis (119)
Ester (119)
Mechelle (118)
Shirlene (118)
Joseph (118)
Debbra (118)
Shonda (117)
Yolonda (117)
Roxie (117)
Kevin (117)
Cornelia (117)
Myrtle (116)
Loren (116)
Jewell (116)
Adrian (116)
Brigid (116)
Marissa (115)
Madelyn (115)
Geralyn (115)
Julianna (115)
Annetta (115)
Keli (114)
Cathrine (114)
Jerry (114)
Latricia (113)
Lorelei (113)
Timothy (113)
Tommie (113)
Randy (113)
Lizette (113)
Ginny (113)
Deeann (113)
Adrianne (113)
Gia (113)
Delinda (113)
Justina (112)
Katheryn (112)
Margot (112)
Jaime (112)
Romona (112)
Evangeline (112)
Carin (112)
Gertrude (112)
Cristine (112)
Fawn (112)
Thomas (111)
Hillary (111)
Angeline (111)
Melva (110)
Sadie (110)
Karolyn (110)
Zelda (110)
Juliette (110)
Bethann (110)
Jena (110)
Shaun (109)
Winifred (109)
Millicent (109)
Lula (109)
Louann (109)
Kenneth (109)
Babette (108)
Anthony (108)

LAST_NAMES
SMITH    2,501,922  1.006   1
JOHNSON  2,014,470  0.81    2
WILLIAMS     1,738,413  0.699   3
JONES    1,544,427  0.621   4
BROWN    1,544,427  0.621   5
DAVIS    1,193,760  0.48    6
MILLER   1,054,488  0.424   7
WILSON   843,093    0.339   8
MOORE    775,944    0.312   9
TAYLOR   773,457    0.311   10
ANDERSON     773,457    0.311   11
THOMAS   773,457    0.311   12
JACKSON  770,970    0.31    13
WHITE    693,873    0.279   14
HARRIS   683,925    0.275   15
MARTIN   678,951    0.273   16
THOMPSON     669,003    0.269   17
GARCIA   631,698    0.254   18
MARTINEZ     581,958    0.234   19
ROBINSON     579,471    0.233   20
CLARK    574,497    0.231   21
RODRIGUEZ    569,523    0.229   22
LEWIS    562,062    0.226   23
LEE  547,140    0.22    24
WALKER   544,653    0.219   25
HALL     497,400    0.2 26
ALLEN    494,913    0.199   27
YOUNG    479,991    0.193   28
HERNANDEZ    477,504    0.192   29
KING     472,530    0.19    30
WRIGHT   470,043    0.189   31
LOPEZ    465,069    0.187   32
HILL     465,069    0.187   33
SCOTT    460,095    0.185   34
GREEN    455,121    0.183   35
ADAMS    432,738    0.174   36
BAKER    425,277    0.171   37
GONZALEZ     412,842    0.166   38
NELSON   402,894    0.162   39
CARTER   402,894    0.162   40
MITCHELL     397,920    0.16    41
PEREZ    385,485    0.155   42
ROBERTS  380,511    0.153   43
TURNER   378,024    0.152   44
PHILLIPS     370,563    0.149   45
CAMPBELL     370,563    0.149   46
PARKER   363,102    0.146   47
EVANS    350,667    0.141   48
EDWARDS  340,719    0.137   49
COLLINS  333,258    0.134   50
STEWART  330,771    0.133   51
SANCHEZ  323,310    0.13    52
MORRIS   310,875    0.125   53
ROGERS   305,901    0.123   54
REED     303,414    0.122   55
COOK     298,440    0.12    56
MORGAN   293,466    0.118   57
BELL     290,979    0.117   58
MURPHY   290,979    0.117   59
BAILEY   286,005    0.115   60
RIVERA   281,031    0.113   61
COOPER   281,031    0.113   62
RICHARDSON   278,544    0.112   63
COX  273,570    0.11    64
HOWARD   273,570    0.11    65
WARD     268,596    0.108   66
TORRES   268,596    0.108   67
PETERSON     266,109    0.107   68
GRAY     263,622    0.106   69
RAMIREZ  261,135    0.105   70
JAMES    261,135    0.105   71
WATSON   256,161    0.103   72
BROOKS   256,161    0.103   73
KELLY    253,674    0.102   74
SANDERS  248,700    0.1 75
PRICE    246,213    0.099   76
BENNETT  246,213    0.099   77
WOOD     243,726    0.098   78
BARNES   241,239    0.097   79
ROSS     238,752    0.096   80
HENDERSON    236,265    0.095   81
COLEMAN  236,265    0.095   82
JENKINS  236,265    0.095   83
PERRY    233,778    0.094   84
POWELL   231,291    0.093   85
LONG     228,804    0.092   86
PATTERSON    228,804    0.092   87
HUGHES   228,804    0.092   88
FLORES   228,804    0.092   89
WASHINGTON   228,804    0.092   90
BUTLER   226,317    0.091   91
SIMMONS  226,317    0.091   92
FOSTER   226,317    0.091   93
GONZALES     216,369    0.087   94
BRYANT   216,369    0.087   95
ALEXANDER    211,395    0.085   96
RUSSELL  211,395    0.085   97
GRIFFIN  208,908    0.084   98
DIAZ     208,908    0.084   99
HAYES    206,421    0.083   100
MYERS    206,421    0.083   101
FORD     203,934    0.082   102
HAMILTON     203,934    0.082   103
GRAHAM   203,934    0.082   104
SULLIVAN     201,447    0.081   105
WALLACE  201,447    0.081   106
WOODS    198,960    0.08    107
COLE     198,960    0.08    108
WEST     198,960    0.08    109
JORDAN   193,986    0.078   110
OWENS    193,986    0.078   111
REYNOLDS     193,986    0.078   112
FISHER   191,499    0.077   113
ELLIS    191,499    0.077   114
HARRISON     189,012    0.076   115
GIBSON   186,525    0.075   116
MCDONALD     186,525    0.075   117
CRUZ     186,525    0.075   118
MARSHALL     186,525    0.075   119
ORTIZ    186,525    0.075   120
GOMEZ    186,525    0.075   121
MURRAY   184,038    0.074   122
FREEMAN  184,038    0.074   123
WELLS    181,551    0.073   124
WEBB     179,064    0.072   125
SIMPSON  174,090    0.07    126
STEVENS  174,090    0.07    127
TUCKER   174,090    0.07    128
PORTER   171,603    0.069   129
HUNTER   171,603    0.069   130
HICKS    171,603    0.069   131
CRAWFORD     169,116    0.068   132
HENRY    169,116    0.068   133
BOYD     169,116    0.068   134
MASON    169,116    0.068   135
MORALES  166,629    0.067   136
KENNEDY  166,629    0.067   137
WARREN   166,629    0.067   138
DIXON    164,142    0.066   139
RAMOS    164,142    0.066   140
REYES    164,142    0.066   141
BURNS    161,655    0.065   142
GORDON   161,655    0.065   143
SHAW     161,655    0.065   144
HOLMES   161,655    0.065   145
RICE     159,168    0.064   146
ROBERTSON    159,168    0.064   147
HUNT     156,681    0.063   148
BLACK    156,681    0.063   149
DANIELS  154,194    0.062   150
PALMER   154,194    0.062   151
MILLS    151,707    0.061   152
NICHOLS  149,220    0.06    153
GRANT    149,220    0.06    154
KNIGHT   149,220    0.06    155
FERGUSON     146,733    0.059   156
ROSE     146,733    0.059   157
STONE    146,733    0.059   158
HAWKINS  146,733    0.059   159
DUNN     144,246    0.058   160
PERKINS  144,246    0.058   161
HUDSON   144,246    0.058   162
SPENCER  141,759    0.057   163
GARDNER  141,759    0.057   164
STEPHENS     141,759    0.057   165
PAYNE    141,759    0.057   166
PIERCE   139,272    0.056   167
BERRY    139,272    0.056   168
MATTHEWS     139,272    0.056   169
ARNOLD   139,272    0.056   170
WAGNER   136,785    0.055   171
WILLIS   136,785    0.055   172
RAY  136,785    0.055   173
WATKINS  136,785    0.055   174
OLSON    136,785    0.055   175
CARROLL  136,785    0.055   176
DUNCAN   136,785    0.055   177
SNYDER   136,785    0.055   178
HART     134,298    0.054   179
CUNNINGHAM   134,298    0.054   180
BRADLEY  134,298    0.054   181
LANE     134,298    0.054   182
ANDREWS  134,298    0.054   183
RUIZ     134,298    0.054   184
HARPER   134,298    0.054   185
FOX  131,811    0.053   186
RILEY    131,811    0.053   187
ARMSTRONG    131,811    0.053   188
CARPENTER    131,811    0.053   189
WEAVER   131,811    0.053   190
GREENE   131,811    0.053   191
LAWRENCE     129,324    0.052   192
ELLIOTT  129,324    0.052   193
CHAVEZ   129,324    0.052   194
SIMS     129,324    0.052   195
AUSTIN   129,324    0.052   196
PETERS   129,324    0.052   197
KELLEY   129,324    0.052   198
FRANKLIN     126,837    0.051   199
LAWSON   126,837    0.051   200
FIELDS   126,837    0.051   201
GUTIERREZ    126,837    0.051   202
RYAN     126,837    0.051   203
SCHMIDT  126,837    0.051   204
CARR     126,837    0.051   205
VASQUEZ  126,837    0.051   206
CASTILLO     126,837    0.051   207
WHEELER  126,837    0.051   208
CHAPMAN  124,350    0.05    209
OLIVER   124,350    0.05    210
MONTGOMERY   121,863    0.049   211
RICHARDS     121,863    0.049   212
WILLIAMSON   121,863    0.049   213
JOHNSTON     121,863    0.049   214
BANKS    119,376    0.048   215
MEYER    119,376    0.048   216
BISHOP   119,376    0.048   217
MCCOY    119,376    0.048   218
HOWELL   119,376    0.048   219
ALVAREZ  119,376    0.048   220
MORRISON     119,376    0.048   221
HANSEN   116,889    0.047   222
FERNANDEZ    116,889    0.047   223
GARZA    116,889    0.047   224
HARVEY   116,889    0.047   225
LITTLE   114,402    0.046   226
BURTON   114,402    0.046   227
STANLEY  114,402    0.046   228
NGUYEN   114,402    0.046   229
GEORGE   114,402    0.046   230
JACOBS   114,402    0.046   231
REID     114,402    0.046   232
KIM  111,915    0.045   233
FULLER   111,915    0.045   234
LYNCH    111,915    0.045   235
DEAN     111,915    0.045   236
GILBERT  111,915    0.045   237
GARRETT  111,915    0.045   238
ROMERO   111,915    0.045   239
WELCH    109,428    0.044   240
LARSON   109,428    0.044   241
FRAZIER  109,428    0.044   242
BURKE    109,428    0.044   243
HANSON   106,941    0.043   244
DAY  106,941    0.043   245
MENDOZA  106,941    0.043   246
MORENO   106,941    0.043   247
BOWMAN   106,941    0.043   248
MEDINA   104,454    0.042   249
FOWLER   104,454    0.042   250
BREWER   104,454    0.042   251
HOFFMAN  104,454    0.042   252
CARLSON  104,454    0.042   253
SILVA    104,454    0.042   254
PEARSON  104,454    0.042   255
HOLLAND  104,454    0.042   256
DOUGLAS  101,967    0.041   257
FLEMING  101,967    0.041   258
JENSEN   101,967    0.041   259
VARGAS   101,967    0.041   260
BYRD     101,967    0.041   261
DAVIDSON     101,967    0.041   262
HOPKINS  101,967    0.041   263
MAY  99,480 0.04    264
TERRY    99,480 0.04    265
HERRERA  99,480 0.04    266
WADE     99,480 0.04    267
SOTO     99,480 0.04    268
WALTERS  99,480 0.04    269
CURTIS   99,480 0.04    270
NEAL     96,993 0.039   271
CALDWELL     96,993 0.039   272
LOWE     96,993 0.039   273
JENNINGS     96,993 0.039   274
BARNETT  96,993 0.039   275
GRAVES   96,993 0.039   276
JIMENEZ  96,993 0.039   277
HORTON   96,993 0.039   278
SHELTON  96,993 0.039   279
BARRETT  96,993 0.039   280
OBRIEN   96,993 0.039   281
CASTRO   96,993 0.039   282
SUTTON   94,506 0.038   283
GREGORY  94,506 0.038   284
MCKINNEY     94,506 0.038   285
LUCAS    94,506 0.038   286
MILES    94,506 0.038   287
CRAIG    94,506 0.038   288
RODRIQUEZ    92,019 0.037   289
CHAMBERS     92,019 0.037   290
HOLT     92,019 0.037   291
LAMBERT  92,019 0.037   292
FLETCHER     92,019 0.037   293
WATTS    92,019 0.037   294
BATES    92,019 0.037   295
HALE     92,019 0.037   296
RHODES   92,019 0.037   297
PENA     92,019 0.037   298
BECK     92,019 0.037   299
NEWMAN   89,532 0.036   300
HAYNES   89,532 0.036   301
MCDANIEL     89,532 0.036   302
MENDEZ   89,532 0.036   303
BUSH     89,532 0.036   304
VAUGHN   89,532 0.036   305
PARKS    87,045 0.035   306
DAWSON   87,045 0.035   307
SANTIAGO     87,045 0.035   308
NORRIS   87,045 0.035   309
HARDY    87,045 0.035   310
LOVE     87,045 0.035   311
STEELE   87,045 0.035   312
CURRY    87,045 0.035   313
POWERS   87,045 0.035   314
SCHULTZ  87,045 0.035   315
BARKER   87,045 0.035   316
GUZMAN   84,558 0.034   317
PAGE     84,558 0.034   318
MUNOZ    84,558 0.034   319
BALL     84,558 0.034   320
KELLER   84,558 0.034   321
CHANDLER     84,558 0.034   322
WEBER    84,558 0.034   323
LEONARD  84,558 0.034   324
WALSH    82,071 0.033   325
LYONS    82,071 0.033   326
RAMSEY   82,071 0.033   327
WOLFE    82,071 0.033   328
SCHNEIDER    82,071 0.033   329
MULLINS  82,071 0.033   330
BENSON   82,071 0.033   331
SHARP    82,071 0.033   332
BOWEN    82,071 0.033   333
DANIEL   82,071 0.033   334
BARBER   79,584 0.032   335
CUMMINGS     79,584 0.032   336
HINES    79,584 0.032   337
BALDWIN  79,584 0.032   338
GRIFFITH     79,584 0.032   339
VALDEZ   79,584 0.032   340
HUBBARD  79,584 0.032   341
SALAZAR  79,584 0.032   342
REEVES   79,584 0.032   343
WARNER   77,097 0.031   344
STEVENSON    77,097 0.031   345
BURGESS  77,097 0.031   346
SANTOS   77,097 0.031   347
TATE     77,097 0.031   348
CROSS    77,097 0.031   349
GARNER   77,097 0.031   350
MANN     77,097 0.031   351
MACK     77,097 0.031   352
MOSS     77,097 0.031   353
THORNTON     77,097 0.031   354
DENNIS   77,097 0.031   355
MCGEE    77,097 0.031   356
FARMER   74,610 0.03    357
DELGADO  74,610 0.03    358
AGUILAR  74,610 0.03    359
VEGA     74,610 0.03    360
GLOVER   74,610 0.03    361
MANNING  74,610 0.03    362
COHEN    74,610 0.03    363
HARMON   74,610 0.03    364
RODGERS  74,610 0.03    365
ROBBINS  74,610 0.03    366
NEWTON   74,610 0.03    367
TODD     74,610 0.03    368
BLAIR    74,610 0.03    369
HIGGINS  74,610 0.03    370
INGRAM   74,610 0.03    371
REESE    74,610 0.03    372
CANNON   74,610 0.03    373
STRICKLAND   74,610 0.03    374
TOWNSEND     74,610 0.03    375
POTTER   74,610 0.03    376
GOODWIN  74,610 0.03    377
WALTON   74,610 0.03    378
ROWE     72,123 0.029   379
HAMPTON  72,123 0.029   380
ORTEGA   72,123 0.029   381
PATTON   72,123 0.029   382
SWANSON  72,123 0.029   383
JOSEPH   72,123 0.029   384
FRANCIS  72,123 0.029   385
GOODMAN  72,123 0.029   386
MALDONADO    72,123 0.029   387
YATES    72,123 0.029   388
BECKER   72,123 0.029   389
ERICKSON     72,123 0.029   390
HODGES   72,123 0.029   391
RIOS     72,123 0.029   392
CONNER   72,123 0.029   393
ADKINS   72,123 0.029   394
WEBSTER  69,636 0.028   395
NORMAN   69,636 0.028   396
MALONE   69,636 0.028   397
HAMMOND  69,636 0.028   398
FLOWERS  69,636 0.028   399
COBB     69,636 0.028   400
MOODY    69,636 0.028   401
QUINN    69,636 0.028   402
BLAKE    69,636 0.028   403
MAXWELL  69,636 0.028   404
POPE     69,636 0.028   405
FLOYD    67,149 0.027   406
OSBORNE  67,149 0.027   407
PAUL     67,149 0.027   408
MCCARTHY     67,149 0.027   409
GUERRERO     67,149 0.027   410
LINDSEY  67,149 0.027   411
ESTRADA  67,149 0.027   412
SANDOVAL     67,149 0.027   413
GIBBS    67,149 0.027   414
TYLER    67,149 0.027   415
GROSS    67,149 0.027   416
FITZGERALD   67,149 0.027   417
STOKES   67,149 0.027   418
DOYLE    67,149 0.027   419
SHERMAN  67,149 0.027   420
SAUNDERS     67,149 0.027   421
WISE     67,149 0.027   422
COLON    67,149 0.027   423
GILL     67,149 0.027   424
ALVARADO     67,149 0.027   425
GREER    64,662 0.026   426
PADILLA  64,662 0.026   427
SIMON    64,662 0.026   428
WATERS   64,662 0.026   429
NUNEZ    64,662 0.026   430
BALLARD  64,662 0.026   431
SCHWARTZ     64,662 0.026   432
MCBRIDE  64,662 0.026   433
HOUSTON  64,662 0.026   434
CHRISTENSEN  64,662 0.026   435
KLEIN    64,662 0.026   436
PRATT    64,662 0.026   437
BRIGGS   64,662 0.026   438
PARSONS  64,662 0.026   439
MCLAUGHLIN   64,662 0.026   440
ZIMMERMAN    64,662 0.026   441
FRENCH   64,662 0.026   442
BUCHANAN     64,662 0.026   443
MORAN    64,662 0.026   444
COPELAND     62,175 0.025   445
ROY  62,175 0.025   446
PITTMAN  62,175 0.025   447
BRADY    62,175 0.025   448
MCCORMICK    62,175 0.025   449
HOLLOWAY     62,175 0.025   450
BROCK    62,175 0.025   451
POOLE    62,175 0.025   452
FRANK    62,175 0.025   453
LOGAN    62,175 0.025   454
OWEN     62,175 0.025   455
BASS     62,175 0.025   456
MARSH    62,175 0.025   457
DRAKE    62,175 0.025   458
WONG     62,175 0.025   459
JEFFERSON    62,175 0.025   460
PARK     62,175 0.025   461
MORTON   62,175 0.025   462
ABBOTT   62,175 0.025   463
SPARKS   62,175 0.025   464
PATRICK  59,688 0.024   465
NORTON   59,688 0.024   466
HUFF     59,688 0.024   467
CLAYTON  59,688 0.024   468
MASSEY   59,688 0.024   469
LLOYD    59,688 0.024   470
FIGUEROA     59,688 0.024   471
CARSON   59,688 0.024   472
BOWERS   59,688 0.024   473
ROBERSON     59,688 0.024   474
BARTON   59,688 0.024   475
TRAN     59,688 0.024   476
LAMB     59,688 0.024   477
HARRINGTON   59,688 0.024   478
CASEY    59,688 0.024   479
BOONE    59,688 0.024   480
CORTEZ   59,688 0.024   481
CLARKE   59,688 0.024   482
MATHIS   59,688 0.024   483
SINGLETON    59,688 0.024   484
WILKINS  59,688 0.024   485
CAIN     59,688 0.024   486
BRYAN    59,688 0.024   487
UNDERWOOD    59,688 0.024   488
HOGAN    59,688 0.024   489
MCKENZIE     57,201 0.023   490
COLLIER  57,201 0.023   491
LUNA     57,201 0.023   492
PHELPS   57,201 0.023   493
MCGUIRE  57,201 0.023   494
ALLISON  57,201 0.023   495
BRIDGES  57,201 0.023   496
WILKERSON    57,201 0.023   497
NASH     57,201 0.023   498
SUMMERS  57,201 0.023   499
ATKINS   57,201 0.023   500
WILCOX   57,201 0.023   501
PITTS    57,201 0.023   502
CONLEY   57,201 0.023   503
MARQUEZ  57,201 0.023   504
BURNETT  57,201 0.023   505
RICHARD  57,201 0.023   506
COCHRAN  57,201 0.023   507
CHASE    57,201 0.023   508
DAVENPORT    57,201 0.023   509
HOOD     57,201 0.023   510
GATES    57,201 0.023   511
CLAY     57,201 0.023   512
AYALA    57,201 0.023   513
SAWYER   57,201 0.023   514
ROMAN    57,201 0.023   515
VAZQUEZ  57,201 0.023   516
DICKERSON    57,201 0.023   517
HODGE    54,714 0.022   518
ACOSTA   54,714 0.022   519
FLYNN    54,714 0.022   520
ESPINOZA     54,714 0.022   521
NICHOLSON    54,714 0.022   522
MONROE   54,714 0.022   523
WOLF     54,714 0.022   524
MORROW   54,714 0.022   525
KIRK     54,714 0.022   526
RANDALL  54,714 0.022   527
ANTHONY  54,714 0.022   528
WHITAKER     54,714 0.022   529
OCONNOR  54,714 0.022   530
SKINNER  54,714 0.022   531
WARE     54,714 0.022   532
MOLINA   54,714 0.022   533
KIRBY    54,714 0.022   534
HUFFMAN  54,714 0.022   535
BRADFORD     54,714 0.022   536
CHARLES  54,714 0.022   537
GILMORE  54,714 0.022   538
DOMINGUEZ    54,714 0.022   539
ONEAL    54,714 0.022   540
BRUCE    54,714 0.022   541
LANG     52,227 0.021   542
COMBS    52,227 0.021   543
KRAMER   52,227 0.021   544
HEATH    52,227 0.021   545
HANCOCK  52,227 0.021   546
GALLAGHER    52,227 0.021   547
GAINES   52,227 0.021   548
SHAFFER  52,227 0.021   549
SHORT    52,227 0.021   550
WIGGINS  52,227 0.021   551
MATHEWS  52,227 0.021   552
MCCLAIN  52,227 0.021   553
FISCHER  52,227 0.021   554
WALL     52,227 0.021   555
SMALL    52,227 0.021   556
MELTON   52,227 0.021   557
HENSLEY  52,227 0.021   558
BOND     52,227 0.021   559
DYER     52,227 0.021   560
CAMERON  52,227 0.021   561
GRIMES   52,227 0.021   562
CONTRERAS    52,227 0.021   563
CHRISTIAN    52,227 0.021   564
WYATT    52,227 0.021   565
BAXTER   52,227 0.021   566
SNOW     52,227 0.021   567
MOSLEY   52,227 0.021   568
SHEPHERD     52,227 0.021   569
LARSEN   52,227 0.021   570
HOOVER   52,227 0.021   571
BEASLEY  49,740 0.02    572
GLENN    49,740 0.02    573
PETERSEN     49,740 0.02    574
WHITEHEAD    49,740 0.02    575
MEYERS   49,740 0.02    576
KEITH    49,740 0.02    577
GARRISON     49,740 0.02    578
VINCENT  49,740 0.02    579
SHIELDS  49,740 0.02    580
HORN     49,740 0.02    581
SAVAGE   49,740 0.02    582
OLSEN    49,740 0.02    583
SCHROEDER    49,740 0.02    584
HARTMAN  49,740 0.02    585
WOODARD  49,740 0.02    586
MUELLER  49,740 0.02    587
KEMP     49,740 0.02    588
DELEON   49,740 0.02    589
BOOTH    49,740 0.02    590
PATEL    49,740 0.02    591
CALHOUN  49,740 0.02    592
WILEY    49,740 0.02    593
EATON    49,740 0.02    594
CLINE    49,740 0.02    595
NAVARRO  49,740 0.02    596
HARRELL  49,740 0.02    597
LESTER   49,740 0.02    598
HUMPHREY     49,740 0.02    599
PARRISH  49,740 0.02    600
DURAN    49,740 0.02    601
HUTCHINSON   49,740 0.02    602
HESS     49,740 0.02    603
DORSEY   49,740 0.02    604
BULLOCK  49,740 0.02    605
ROBLES   49,740 0.02    606
BEARD    47,253 0.019   607
DALTON   47,253 0.019   608
AVILA    47,253 0.019   609
VANCE    47,253 0.019   610
RICH     47,253 0.019   611
BLACKWELL    47,253 0.019   612
YORK     47,253 0.019   613
JOHNS    47,253 0.019   614
BLANKENSHIP  47,253 0.019   615
TREVINO  47,253 0.019   616
SALINAS  47,253 0.019   617
CAMPOS   47,253 0.019   618
PRUITT   47,253 0.019   619
MOSES    47,253 0.019   620
CALLAHAN     47,253 0.019   621
GOLDEN   47,253 0.019   622
MONTOYA  47,253 0.019   623
HARDIN   47,253 0.019   624
GUERRA   47,253 0.019   625
MCDOWELL     47,253 0.019   626
CAREY    47,253 0.019   627
STAFFORD     47,253 0.019   628
GALLEGOS     47,253 0.019   629
HENSON   47,253 0.019   630
WILKINSON    47,253 0.019   631
BOOKER   47,253 0.019   632
MERRITT  47,253 0.019   633
MIRANDA  47,253 0.019   634
ATKINSON     47,253 0.019   635
ORR  47,253 0.019   636
DECKER   47,253 0.019   637
HOBBS    47,253 0.019   638
PRESTON  47,253 0.019   639
TANNER   47,253 0.019   640
KNOX     47,253 0.019   641
PACHECO  47,253 0.019   642
STEPHENSON   44,766 0.018   643
GLASS    44,766 0.018   644
ROJAS    44,766 0.018   645
SERRANO  44,766 0.018   646
MARKS    44,766 0.018   647
HICKMAN  44,766 0.018   648
ENGLISH  44,766 0.018   649
SWEENEY  44,766 0.018   650
STRONG   44,766 0.018   651
PRINCE   44,766 0.018   652
MCCLURE  44,766 0.018   653
CONWAY   44,766 0.018   654
WALTER   44,766 0.018   655
ROTH     44,766 0.018   656
MAYNARD  44,766 0.018   657
FARRELL  44,766 0.018   658
LOWERY   44,766 0.018   659
HURST    44,766 0.018   660
NIXON    44,766 0.018   661
WEISS    44,766 0.018   662
TRUJILLO     44,766 0.018   663
ELLISON  44,766 0.018   664
SLOAN    44,766 0.018   665
JUAREZ   44,766 0.018   666
WINTERS  44,766 0.018   667
MCLEAN   44,766 0.018   668
RANDOLPH     44,766 0.018   669
LEON     44,766 0.018   670
BOYER    44,766 0.018   671
VILLARREAL   44,766 0.018   672
MCCALL   44,766 0.018   673
GENTRY   44,766 0.018   674
CARRILLO     42,279 0.017   675
KENT     42,279 0.017   676
AYERS    42,279 0.017   677
LARA     42,279 0.017   678
SHANNON  42,279 0.017   679
SEXTON   42,279 0.017   680
PACE     42,279 0.017   681
HULL     42,279 0.017   682
LEBLANC  42,279 0.017   683
BROWNING     42,279 0.017   684
VELASQUEZ    42,279 0.017   685
LEACH    42,279 0.017   686
CHANG    42,279 0.017   687
HOUSE    42,279 0.017   688
SELLERS  42,279 0.017   689
HERRING  42,279 0.017   690
NOBLE    42,279 0.017   691
FOLEY    42,279 0.017   692
BARTLETT     42,279 0.017   693
MERCADO  42,279 0.017   694
LANDRY   42,279 0.017   695
DURHAM   42,279 0.017   696
WALLS    42,279 0.017   697
BARR     42,279 0.017   698
MCKEE    42,279 0.017   699
BAUER    42,279 0.017   700
RIVERS   42,279 0.017   701
EVERETT  42,279 0.017   702
BRADSHAW     42,279 0.017   703
PUGH     42,279 0.017   704
VELEZ    42,279 0.017   705
RUSH     42,279 0.017   706
ESTES    42,279 0.017   707
DODSON   42,279 0.017   708
MORSE    42,279 0.017   709
SHEPPARD     42,279 0.017   710
WEEKS    42,279 0.017   711
CAMACHO  42,279 0.017   712
BEAN     42,279 0.017   713
BARRON   42,279 0.017   714
LIVINGSTON   42,279 0.017   715
MIDDLETON    39,792 0.016   716
SPEARS   39,792 0.016   717
BRANCH   39,792 0.016   718
BLEVINS  39,792 0.016   719
CHEN     39,792 0.016   720
KERR     39,792 0.016   721
MCCONNELL    39,792 0.016   722
HATFIELD     39,792 0.016   723
HARDING  39,792 0.016   724
ASHLEY   39,792 0.016   725
SOLIS    39,792 0.016   726
HERMAN   39,792 0.016   727
FROST    39,792 0.016   728
GILES    39,792 0.016   729
BLACKBURN    39,792 0.016   730
WILLIAM  39,792 0.016   731
PENNINGTON   39,792 0.016   732
WOODWARD     39,792 0.016   733
FINLEY   39,792 0.016   734
MCINTOSH     39,792 0.016   735
KOCH     39,792 0.016   736
BEST     39,792 0.016   737
SOLOMON  39,792 0.016   738
MCCULLOUGH   39,792 0.016   739
DUDLEY   39,792 0.016   740
NOLAN    39,792 0.016   741
BLANCHARD    39,792 0.016   742
RIVAS    39,792 0.016   743
BRENNAN  39,792 0.016   744
MEJIA    39,792 0.016   745
KANE     39,792 0.016   746
BENTON   39,792 0.016   747
JOYCE    39,792 0.016   748
BUCKLEY  39,792 0.016   749
HALEY    39,792 0.016   750
VALENTINE    39,792 0.016   751
MADDOX   39,792 0.016   752
RUSSO    39,792 0.016   753
MCKNIGHT     39,792 0.016   754
BUCK     39,792 0.016   755
MOON     39,792 0.016   756
MCMILLAN     39,792 0.016   757
CROSBY   39,792 0.016   758
BERG     39,792 0.016   759
DOTSON   39,792 0.016   760
MAYS     39,792 0.016   761
ROACH    39,792 0.016   762
CHURCH   39,792 0.016   763
CHAN     39,792 0.016   764
RICHMOND     39,792 0.016   765
MEADOWS  39,792 0.016   766
FAULKNER     39,792 0.016   767
ONEILL   39,792 0.016   768
KNAPP    39,792 0.016   769
KLINE    37,305 0.015   770
BARRY    37,305 0.015   771
OCHOA    37,305 0.015   772
JACOBSON     37,305 0.015   773
GAY  37,305 0.015   774
AVERY    37,305 0.015   775
HENDRICKS    37,305 0.015   776
HORNE    37,305 0.015   777
SHEPARD  37,305 0.015   778
HEBERT   37,305 0.015   779
CHERRY   37,305 0.015   780
CARDENAS     37,305 0.015   781
MCINTYRE     37,305 0.015   782
WHITNEY  37,305 0.015   783
WALLER   37,305 0.015   784
HOLMAN   37,305 0.015   785
DONALDSON    37,305 0.015   786
CANTU    37,305 0.015   787
TERRELL  37,305 0.015   788
MORIN    37,305 0.015   789
GILLESPIE    37,305 0.015   790
FUENTES  37,305 0.015   791
TILLMAN  37,305 0.015   792
SANFORD  37,305 0.015   793
BENTLEY  37,305 0.015   794
PECK     37,305 0.015   795
KEY  37,305 0.015   796
SALAS    37,305 0.015   797
ROLLINS  37,305 0.015   798
GAMBLE   37,305 0.015   799
DICKSON  37,305 0.015   800
BATTLE   37,305 0.015   801
SANTANA  37,305 0.015   802
CABRERA  37,305 0.015   803
CERVANTES    37,305 0.015   804
HOWE     37,305 0.015   805
HINTON   37,305 0.015   806
HURLEY   37,305 0.015   807
SPENCE   37,305 0.015   808
ZAMORA   37,305 0.015   809
YANG     37,305 0.015   810
MCNEIL   37,305 0.015   811
SUAREZ   37,305
WYNN     32,331 0.013   909
NIELSEN  32,331 0.013   910
BAIRD    32,331 0.013   911
STANTON  32,331 0.013   912
SNIDER   32,331 0.013   913
ROSALES  32,331 0.013   914
BRIGHT   32,331 0.013   915
WITT     32,331 0.013   916
STUART   32,331 0.013   917
HAYS     32,331 0.013   918
HOLDEN   32,331 0.013   919
RUTLEDGE     32,331 0.013   920
KINNEY   32,331 0.013   921
CLEMENTS     32,331 0.013   922
CASTANEDA    32,331 0.013   923
SLATER   32,331 0.013   924
HAHN     32,331 0.013   925
EMERSON  32,331 0.013   926
CONRAD   32,331 0.013   927
BURKS    32,331 0.013   928
DELANEY  32,331 0.013   929
PATE     32,331 0.013   930
LANCASTER    32,331 0.013   931
SWEET    32,331 0.013   932
JUSTICE  32,331 0.013   933
TYSON    32,331 0.013   934
SHARPE   32,331 0.013   935
WHITFIELD    32,331 0.013   936
TALLEY   32,331 0.013   937
MACIAS   32,331 0.013   938
IRWIN    32,331 0.013   939
BURRIS   32,331 0.013   940
RATLIFF  32,331 0.013   941
MCCRAY   32,331 0.013   942
MADDEN   32,331 0.013   943
KAUFMAN  32,331 0.013   944
BEACH    32,331 0.013   945
GOFF     32,331 0.013   946
CASH     32,331 0.013   947
BOLTON   32,331 0.013   948
MCFADDEN     32,331 0.013   949
LEVINE   32,331 0.013   950
GOOD     32,331 0.013   951
BYERS    32,331 0.013   952
KIRKLAND     32,331 0.013   953
KIDD     32,331 0.013   954
WORKMAN  32,331 0.013   955
CARNEY   32,331 0.013   956
DALE     32,331 0.013   957
MCLEOD   32,331 0.013   958
HOLCOMB  32,331 0.013   959
ENGLAND  32,331 0.013   960
FINCH    32,331 0.013   961
HEAD     29,844 0.012   962
BURT     29,844 0.012   963
HENDRIX  29,844 0.012   964
SOSA     29,844 0.012   965
HANEY    29,844 0.012   966
FRANKS   29,844 0.012   967
SARGENT  29,844 0.012   968
NIEVES   29,844 0.012   969
DOWNS    29,844 0.012   970
RASMUSSEN    29,844 0.012   971
BIRD     29,844 0.012   972
HEWITT   29,844 0.012   973
LINDSAY  29,844 0.012   974
LE   29,844 0.012   975
FOREMAN  29,844 0.012   976
VALENCIA     29,844 0.012   977
ONEIL    29,844 0.012   978
DELACRUZ     29,844 0.012   979
VINSON   29,844 0.012   980
DEJESUS  29,844 0.012   981
HYDE     29,844 0.012   982
FORBES   29,844 0.012   983
GILLIAM  29,844 0.012   984
GUTHRIE  29,844 0.012   985
WOOTEN   29,844 0.012   986
HUBER    29,844 0.012   987
BARLOW   29,844 0.012   988
BOYLE    29,844 0.012   989
MCMAHON  29,844 0.012   990
BUCKNER  29,844 0.012   991
ROCHA    29,844 0.012   992
PUCKETT  29,844 0.012   993
LANGLEY  29,844 0.012   994
KNOWLES  29,844 0.012   995
COOKE    29,844 0.012   996
VELAZQUEZ    29,844 0.012   997
WHITLEY  29,844 0.012   998
NOEL     29,844 0.012   999
VANG     29,844 0.012   1000

DEP_INITIAL
Long-Distance
Quasi
Independent
Central
Regional
Fundamental
Peripheral
Community
Online
Quantitative
Qualitative

DEP_MEDIAL
Technical
Debt
Asset
Human
Financial
Fiscal
Engineering
Automotive
Health
Safety
Information
Warehouse
Development
Engagement
Valuation

DEP_FINAL
Monitoring
Dynamics
Publishing
Delivery
Authority
Board
Group
Accounting
Resources
Logistics
Analysis
Research
Planning
Strategy
Technology
Sales
Archives


