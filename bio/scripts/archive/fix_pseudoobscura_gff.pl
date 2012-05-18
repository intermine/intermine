#!/usr/bin/perl -w

# script to read a FlyBase pseudoobscura and writes a fixed version
# fixes:
#   - only use SO types (change match and match_part to the appropriate SO
#     type - mRNA or exon)
#   - remove records with source == 'part_of' (which removes two exons with an
#     exon as parent)

use strict;

my %id_map = ();

my %start_pos_map = ();

my $re = qr/^(\S+)\t(\S*)\t(\S+)\t(\d+)\t(\d+)\t(\S*)\t(\S*)\t(\S*)\t(\S*)/;

my @genes = ();

sub parse
{
  my $gff = shift;

  if (my @bits = $gff =~ /$re/) {
    my $source = $bits[1];
    my $type = $bits[2];
    my $start = $bits[3];
    my $end = $bits[4];
    my $attributes = $bits[8];

    if ($source =~/^blast/) {
      return undef;
    }

    if ($source eq "part_of") {
      return undef;
    }

    my @att_bits = split /;/, $attributes;

    my %att_map = ();

    for my $att_bit (@att_bits) {
      my ($name, $value) = $att_bit =~ /(\S+)=(.*)/;

      my @values = split /,/, $value;

      if ($name eq "Parent") {
        my %seen = ();
        @values = grep { ! $seen{$_} ++ } @values;
      }

      $att_map{$name} = [@values];
    }

    if ($type eq 'match' || $type eq 'match_part') {
      if (!exists $att_map{Parent}) {
        # ignore matches with no Parent
        return undef;
      }
    }

    my $id = $att_map{ID}[0];

    my $parsed_gff = {
                      id => $id,
                      source => $source,
                      type => $type,
                      start => $start,
                      end => $end,
                      attributes => \%att_map,
                      orig => $gff,
                     };

    return $parsed_gff;
 }

  return undef;
}

# parse
while (<>) {
  chomp $_;
  my $parsed = parse $_;
  if (defined $parsed) {
    my $id = $parsed->{id};
    if ($parsed->{type} eq "gene") {
      push @genes, $parsed;
    }

    $id_map{$id} = $parsed;

    push @{$start_pos_map{$parsed->{start}}}, $parsed;
  }
}

# find all children and set $parsed_gff->{children}
while (my ($id, $parsed_gff) = each (%id_map)) {
  my $parent_ref = $parsed_gff->{attributes}{Parent};

  if (defined $parent_ref) {
    my @parents = @{$parent_ref};

    for my $parent_id (@parents) {
      my $parent = $id_map{$parent_id};

      if (!grep /^\Q$id$/, @{$parent->{children}}) {
        push @{$parent->{children}}, $id;
      }

#       if ($id eq 'gene00046635:1') {
#         print "id: $id  -  parent: $parent_id\n";

#         print @{$parent->{children}}, "\n";
#       }
    }
  }
}

# show features that have the same start,end and Parent as another
sub check_for_duplicate
{
  my $type = shift;

  my %features_to_merge = ();

  while (my ($id, $parsed_gff) = each (%id_map)) {
    my @same_start_features = @{$start_pos_map{$parsed_gff->{start}}};

    for my $test_feature (@same_start_features) {
      if ($id eq $test_feature->{id}) {
        next;
      }
      if ($parsed_gff->{type} eq $type && $test_feature->{type} eq $type) {
        if ($parsed_gff->{end} == $test_feature->{end}) {
#          print "$id == ", $test_feature->{id}, "  start: ", 
#                $parsed_gff->{start}, "  end: ", $parsed_gff->{end}, "\n";
          push @{$features_to_merge{$id}}, $test_feature->{id};
        }
      }
    }
  }

  while (my ($id, $others) = each (%features_to_merge)) {
    my @all = ($id, @{$others});

    print "all: @all\n";
    for my $feature_id (@all) {
      my $feature = $id_map{$feature_id};
#      print $feature_id, " ", $feature->{source}, "   @{$feature->{attributes}{Parent}}\n";
    }
  }
}

#check_for_duplicate "exon";
#check_for_duplicate "mRNA";


# use Data::Dumper;

# print Dumper $id_map{'gene00046635:1'}, "\n";
# print Dumper $id_map{'gene00046635-RA'}, "\n";


# fix match and match_part types to be mRNA and exon
sub match_fix
{
  my $id = shift;
  if (defined $id) {
    my $current_depth = shift;
    my $obj = $id_map{$id};

#     print "before: ", $obj->{type}, "\n";

    if ($current_depth == 1) {
      if ($obj->{type} eq 'match' || $obj->{type} eq 'match_part') {
        $obj->{type} = 'mRNA';
      }
    } else {
      if ($current_depth == 2) {
        if ($obj->{type} eq 'match' || $obj->{type} eq 'match_part') {
          $obj->{type} = 'exon';
        }
      }
    }

#     print "after:  $id  $current_depth ", $obj->{type}, "\n";

#     print $obj->{orig},"\n";

    $current_depth++;

    for my $child (@{$obj->{children}}) {
      match_fix($child, $current_depth);
    }
  }
}
for my $obj (@genes) {
  match_fix($obj->{id}, 0);
}

# print the path to each record (via parent links)
my %path_map = ();
sub path
{
  my $id = shift;
  if (defined $id) {
    my $current_path = shift;
    my $obj = $id_map{$id};

    $current_path .= $obj->{type} . " > ";

    $path_map{$current_path} = $id;

    for my $child (@{$obj->{children}}) {
      path($child, $current_path);
    }
  }
}
#while (my ($id, $obj) = each (%id_map)) {
for my $obj (@genes) {
  path($obj->{id}, "");
}
for my $path (keys %path_map) {
#  print STDERR "path: $path\n";
}

# write
sub output_record
{
  my $obj = shift;
  my $type = $obj->{type};
#   print "type: $type\n";
#   print "before: ", $obj->{orig}, "\n";
  $obj->{orig} =~ s/^(\S*\t\S*\t)\S*/$1$type/;
  print $obj->{orig}, "\n";
}
for my $obj (values %id_map) {
  output_record $obj;
}
