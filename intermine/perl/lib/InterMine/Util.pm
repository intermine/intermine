package InterMine::Util;

=head1 NAME

InterMine::Util - Utility functions for InterMine

=cut

use strict;

use Exporter;

our @ISA = qw(Exporter);
our @EXPORT_OK = qw(get_property_value);

=head2 get_property_value
 Title   : get_property_value
 Usage   : $property_value = 
              InterMine::Util::get_property_value('db.production.datasource.serverName',
                                                  '/home/user/flymine.properties');
 Function: gets a value from a properties file
 Args    : $property_name, $property_file_name
=cut

sub get_property_value
{
  my $key = shift;
  my $file = shift;

  open F, "$file" or die "cannot open $file: $!\n";

  my $ret_val;

  while (my $line = <F>) {
    if ($line =~ /^\s*#/) {
      next;
    }
    if ($line =~ /$key=(.*)/) {
      $ret_val = $1;
    }
  }

  close F;

  return $ret_val;
}

1;
