package Test::Webservice::InterMine::TemplateConstraint;;

use base ('Test::Webservice::InterMine::Constraint', 'Test::Webservice::InterMine::Constraint::Roles::Templated');

################# START OF DUMB CUT AND PASTE SECTION

use InterMine::Model;

sub args {
    my $test          = shift;
    my @baseargs      = $test->SUPER::args;
    my $template_args = ($test->good_template_args)[0];
    return (@baseargs, @$template_args);
}

sub startup : Test(startup => 1) {
    my $test = shift;
    $test->{object} = $test->make_object($test->args);
}

sub requirements {}

sub hash {
    my $test = shift;
    return ($test->SUPER::hash, $test->template_hash_elements);
}

sub string {
    my $test = shift;
    return $test->SUPER::string . $test->template_string_suffix;
}
sub object {}

sub make_object {
    my $test = shift;
    return $test->make_template_object(@_);
}

sub default_attributes {
    my $test = shift;
    my %attr = ($test->SUPER::default_attributes, $test->template_attributes);
    return %attr;
}
################### END OF DUMB CUT AND PASTE SECTION
sub type { '' }

sub strict_construction : Test(28) {
    my $test = shift;
    $test->SUPER::strict_construction;
    $test->test_template_construction($test->SUPER::args);
}

sub inheritance : Test(3) {
    my $test = shift;
    $test->SUPER::inheritance;
    $test->test_template_inheritance;
}

sub methods : Test(3)  {
    my $test = shift;
    $test->SUPER::methods;
    $test->test_template_methods;
}

sub attributes : Test(12) {
    my $test = shift;
    $test->SUPER::attributes;
}

1;

package Test::Webservice::InterMine::TemplateConstraint::Unary;

use base ('Test::Webservice::InterMine::Constraint::Roles::Templated',
	  'Test::Webservice::InterMine::Constraint::Unary',);

################# START OF DUMB CUT AND PASTE SECTION

use InterMine::Model;

sub args {
    my $test          = shift;
    my @baseargs      = $test->SUPER::args;
    my $template_args = ($test->good_template_args)[0];
    return (@baseargs, @$template_args);
}

sub startup : Test(startup => 1) {
    my $test = shift;
    $test->{object} = $test->make_object($test->args);
}

sub requirements {}

sub hash {
    my $test = shift;
    return ($test->SUPER::hash, $test->template_hash_elements);
}

sub string {
    my $test = shift;
    return $test->SUPER::string . $test->template_string_suffix;
}
sub object {}

sub make_object {
    my $test = shift;
    return $test->make_template_object(@_);
}

sub default_attributes {
    my $test = shift;
    my %attr = ($test->SUPER::default_attributes, $test->template_attributes);
    return %attr;
}
################### END OF DUMB CUT AND PASTE SECTION

sub type { 'unary' }

sub strict_construction : Test(35) {
    my $test = shift;
    $test->SUPER::strict_construction;
    $test->test_template_construction($test->SUPER::args);
}

sub inheritance : Test(5) {
    my $test = shift;
    $test->SUPER::inheritance;
    $test->test_template_inheritance;
}

sub methods : Test(5)  {
    my $test = shift;
    $test->SUPER::methods;
    $test->test_template_methods;
}

sub attributes : Test(16) {
    my $test = shift;
    $test->SUPER::attributes;
}


1;

package Test::Webservice::InterMine::TemplateConstraint::Binary;

use base (
    'Test::Webservice::InterMine::Constraint::Binary',
    'Test::Webservice::InterMine::Constraint::Roles::Templated'
);
################# START OF DUMB CUT AND PASTE SECTION

use InterMine::Model;

sub args {
    my $test          = shift;
    my @baseargs      = $test->SUPER::args;
    my $template_args = ($test->good_template_args)[0];
    return (@baseargs, @$template_args);
}

sub startup : Test(startup => 1) {
    my $test = shift;
    $test->{object} = $test->make_object($test->args);
}

sub requirements {}

sub hash {
    my $test = shift;
    return ($test->SUPER::hash, $test->template_hash_elements);
}

sub string {
    my $test = shift;
    return $test->SUPER::string . $test->template_string_suffix;
}
sub object {}

sub make_object {
    my $test = shift;
    return $test->make_template_object(@_);
}

sub default_attributes {
    my $test = shift;
    my %attr = ($test->SUPER::default_attributes, $test->template_attributes);
    return %attr;
}
################### END OF DUMB CUT AND PASTE SECTION

sub type { 'binary' }

sub strict_construction : Test(51) {
    my $test = shift;
    $test->SUPER::strict_construction;
    $test->test_template_construction($test->SUPER::args);
}

sub inheritance : Test(5) {
    my $test = shift;
    $test->SUPER::inheritance;
    $test->test_template_inheritance;
}

sub methods : Test(6)  {
    my $test = shift;
    $test->SUPER::methods;
    $test->test_template_methods;
}

sub attributes : Test(19) {
    my $test = shift;
    $test->SUPER::attributes;
}

1;

package Test::Webservice::InterMine::TemplateConstraint::Ternary;

use base (
    'Test::Webservice::InterMine::Constraint::Ternary',
    'Test::Webservice::InterMine::Constraint::Roles::Templated'
);
################# START OF DUMB CUT AND PASTE SECTION

use InterMine::Model;

sub args {
    my $test          = shift;
    my @baseargs      = $test->SUPER::args;
    my $template_args = ($test->good_template_args)[0];
    return (@baseargs, @$template_args);
}

sub startup : Test(startup => 1) {
    my $test = shift;
    $test->{object} = $test->make_object($test->args);
}

sub requirements {}

sub hash {
    my $test = shift;
    return ($test->SUPER::hash, $test->template_hash_elements);
}

sub string {
    my $test = shift;
    return $test->SUPER::string . $test->template_string_suffix;
}
sub object {}

sub make_object {
    my $test = shift;
    return $test->make_template_object(@_);
}

sub default_attributes {
    my $test = shift;
    my %attr = ($test->SUPER::default_attributes, $test->template_attributes);
    return %attr;
}
################### END OF DUMB CUT AND PASTE SECTION

sub type { 'ternary' }

sub strict_construction : Test(36) {
    my $test = shift;
    $test->SUPER::strict_construction;
    $test->test_template_construction($test->SUPER::args);
}

sub inheritance : Test(6) {
    my $test = shift;
    $test->SUPER::inheritance;
    $test->test_template_inheritance;
}

sub methods : Test(7)  {
    my $test = shift;
    $test->SUPER::methods;
    $test->test_template_methods;
}

sub attributes : Test(21) {
    my $test = shift;
    $test->SUPER::attributes;
}

1;

package Test::Webservice::InterMine::TemplateConstraint::Multi;

use base (
    'Test::Webservice::InterMine::Constraint::Multi',
    'Test::Webservice::InterMine::Constraint::Roles::Templated'
);
################# START OF DUMB CUT AND PASTE SECTION

use InterMine::Model;

sub args {
    my $test          = shift;
    my @baseargs      = $test->SUPER::args;
    my $template_args = ($test->good_template_args)[0];
    return (@baseargs, @$template_args);
}

sub startup : Test(startup => 1) {
    my $test = shift;
    $test->{object} = $test->make_object($test->args);
}

sub requirements {}

sub hash {
    my $test = shift;
    return ($test->SUPER::hash, $test->template_hash_elements);
}

sub string {
    my $test = shift;
    return $test->SUPER::string . $test->template_string_suffix;
}
sub object {}

sub make_object {
    my $test = shift;
    return $test->make_template_object(@_);
}

sub default_attributes {
    my $test = shift;
    my %attr = ($test->SUPER::default_attributes, $test->template_attributes);
    return %attr;
}
################### END OF DUMB CUT AND PASTE SECTION

sub type { 'multi' }

sub strict_construction : Test(36) {
    my $test = shift;
    $test->SUPER::strict_construction;
    $test->test_template_construction($test->SUPER::args);
}

sub inheritance : Test(5) {
    my $test = shift;
    $test->SUPER::inheritance;
    $test->test_template_inheritance;
}

sub methods : Test(6)  {
    my $test = shift;
    $test->SUPER::methods;
    $test->test_template_methods;
}

sub attributes : Test(18) {
    my $test = shift;
    $test->SUPER::attributes;
}

1;


package Test::Webservice::InterMine::TemplateConstraint::SubClass;

use base (
    'Test::Webservice::InterMine::Constraint::SubClass',
    'Test::Webservice::InterMine::Constraint::Roles::Templated'
);
################# START OF DUMB CUT AND PASTE SECTION

use InterMine::Model;

sub args {
    my $test          = shift;
    my @baseargs      = $test->SUPER::args;
    my $template_args = ($test->good_template_args)[0];
    return (@baseargs, @$template_args);
}

sub startup : Test(startup => 1) {
    my $test = shift;
    $test->{object} = $test->make_object($test->args);
}

sub requirements {}

sub hash {
    my $test = shift;
    return ($test->SUPER::hash, $test->template_hash_elements);
}

sub string {
    my $test = shift;
    return $test->SUPER::string . $test->template_string_suffix;
}
sub object {}

sub make_object {
    my $test = shift;
    return $test->make_template_object(@_);
}

sub default_attributes {
    my $test = shift;
    my %attr = ($test->SUPER::default_attributes, $test->template_attributes);
    return %attr;
}
################### END OF DUMB CUT AND PASTE SECTION

sub type { 'subclass' }

sub strict_construction : Test(28) {
    my $test = shift;
    $test->SUPER::strict_construction;
    $test->test_template_construction($test->SUPER::args);
}

sub inheritance : Test(4) {
    my $test = shift;
    $test->SUPER::inheritance;
    $test->test_template_inheritance;
}

sub methods : Test(4)  {
    my $test = shift;
    $test->SUPER::methods;
    $test->test_template_methods;
}

sub attributes : Test(14) {
    my $test = shift;
    $test->SUPER::attributes;
}

1;
