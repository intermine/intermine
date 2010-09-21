#!/usr/bin/perl
# could parse properties file instead
my $DBHOST = $ARGV[0];
my $CHADODB = $ARGV[1];
my $DBUSER = $ARGV[2];
my $report = $ARGV[3];

my $chado_feats = `psql -H -h $DBHOST -d $CHADODB -U $DBUSER -c 'select c.name, c.cvterm_id, count(*) from feature f, cvterm c where c.cvterm_id = f.type_id group by c.name, c.cvterm_id order by c.name;'`;

my $chado_header = "<h2>Chado features</h2>Please check figures and if there is any new one we are missing.<p>";
my $chado_footer = "<p><hr><p>";

open (FILE, $report)
     or die "can't open $report $!\n";
while (<FILE>) {
#chomp;

my $whole = $_;

if ($whole =~ m:test0: ){
print "$chado_header\n";
print "$chado_feats\n";
print "$chado_footer\n";
print $whole;
}
else {
print $whole;
}

}
close (FILE);
exit;
