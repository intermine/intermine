#!/usr/bin/perl -w

use strict;

# script to check GFF files for duplicate IDs, missing Parents and to check
# that no record has a parent of the same type

my $file = shift;

open F, $file or die;

my %parent_id_to_type = ();

my $re = qr/^\S+\t\S*\t(\S+)\t\d+\t\d+\t\S*\t\S*\t\S*\t(\S*)/;
my $bits_re = qr/ID=([^;]+)(?:.*Parent=([^;]+))?/;

while (<F>) {
  if (my ($type, $bits) = /$re/) {
    if (my ($id, $parent_bits) = $bits =~ /$bits_re/) {
      my %parents = ();
      if ($parent_bits) {
        @parents{split /,/, $parent_bits} = split /,/, $parent_bits;
      }
      if (exists $parent_id_to_type{$id}) {
        warn "duplicate ID: $id  in $bits\n";
      } else {
        $parent_id_to_type{$id} = {type=>$type, parents=>[keys %parents]};
      }
    }
  }
}

open F, $file or die;

while (<F>) {
  if (my ($type, $bits) = /$re/) {
    if (my ($id, $parent_bits) = $bits =~ /$bits_re/) {
      if ($parent_bits) {
        my @parents = split /,/, $parent_bits;
        for my $parent_id (@parents) {
          if (!exists $parent_id_to_type{$parent_id}) {
            warn "ID $id has missing Parent: $parent_id\n";
          }
          
          if ($parent_id_to_type{$parent_id} eq $type) {
            warn "parent has same type: $_"
          }
        }
      }
    }
  }
}
