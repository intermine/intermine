use Test::More;

my @modules;

sub find_modules {
    my $thing = shift;
    if (-d $thing && $thing !~ /^\./) {
        opendir(my $dh, $thing) or die $!;
        while (my $subthing = readdir($dh)) {
            find_modules($thing . '/' . $subthing) unless ($subthing =~ /^\./);
        }
        closedir $dh or die $!
    } else {
        if ($thing =~ /\.pm/) {
            push @modules, $thing;
        }
    }
}

find_modules('lib');

BAIL_OUT "No modules found" unless @modules;

my @dirties;

for my $module (@modules) {
    my $is_using_moose;
    my $has_cleaned_up;
    open(my $mod_h, '<', $module) or die $!;
    while (<$mod_h>) {
        $is_using_moose++ if /^use Moose;/;
        $has_cleaned_up++ if /->meta->make_immutable/;
    }
    close $mod_h or die $!;
    if($is_using_moose and not $has_cleaned_up) {
        push @dirties, $module;
    }
}

is(scalar(@dirties), 0, "There are no dirty modules")
    or diag(sprintf("%d of %d modules are dirty. Dirty modules are:\n\t", $#dirties + 1, $#modules + 1),
            join("\n\t", @dirties));

done_testing;



