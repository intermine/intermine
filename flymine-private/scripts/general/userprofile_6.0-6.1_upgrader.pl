#!/usr/bin/perl -w

use strict;

BEGIN {
  push @INC, "$ENV{HOME}/svn/dev/intermine/perl/lib";
}

use Apache::DBI;

my @types;

BEGIN {
  # hard coded path at the moment - needs fixing
  $InterMine::model_file = '/tmp/genomic_model.xml';
  $InterMine::properties_file = "$ENV{HOME}/flymine.properties";
  $InterMine::db_prefix = 'db.production';
  $InterMine::class_keys = "$ENV{HOME}/svn/dev/bio/core/props/resources/class_keys.properties";
  @types = qw(Gene Protein Exon GOTerm Transcript Organism);
}


# specify which types/classes will be used
use InterMine @types;
use InterMine qw(Synonym);

use XML::Simple qw(:strict);

$Rose::DB::Object::QueryBuilder::Debug = 0;

my $xs = new XML::Simple(ForceArray => 1, KeepRoot => 1, KeyAttr => []);

my $in_file_name = shift;

my $parsed = $xs->XMLin($in_file_name);

my @userprofiles = @{$parsed->{userprofiles}[0]{userprofile}};

my %user_bags = ();

for my $userprofile (@userprofiles) {
  my $username = $userprofile->{username};
  print "$username\n";

  if ($username =~ /flymine|drslyne.ntlworld.com/) {
    warn "  - skipping\n";
    next;
  }

  if (defined $userprofile->{bags} && defined $userprofile->{bags}[0]{bag}) {
    my @bags = @{$userprofile->{bags}[0]{bag}};
    my @new_bags = ();

    for my $bag (@bags) {
      my $bag_name = $bag->{name};
      my $bag_size = scalar @{$bag->{element}};
      print "  $bag_name - $bag_size elements\n";

      my %objects_by_type = find_objects_by_type($bag);

      print '    unknown in this bag: ', scalar(@{$objects_by_type{UNKNOWN}}), "\n";

      $user_bags{$username}{$bag_name} = \%objects_by_type;
    }
  }
}

use Data::Dumper;

open DUMP, '>userprofile_perl_dump' or die;
my $dumper = new Data::Dumper([\%user_bags]);
$dumper->Indent(1);
print DUMP $dumper->Dump();

# open F, '>processed_userprofile.xml';
# print F $xs->XMLout($parsed);


my %object_by_identifier_cache = ();

sub find_objects_by_type
{
  my $bag = shift;

  my %objects_by_type = (UNKNOWN => []);

  my @identifiers = ();

  for my $element (@{$bag->{element}}) {
    my $value = $element->{value};
    my $java_type = $element->{type};
    if ($java_type eq 'java.lang.String') {
      push @identifiers, $value;
    }
  }

  my %keys_for_type = ();

  for my $type (@types) {
    my $lc_type = lc $type;

    my @class_keys = InterMine::get_class_keys($type);

    $keys_for_type{$type} = [@class_keys];
  }

 IDENTIFIER:
  for my $identifier (@identifiers) {
#    warn " identifier: $identifier\n";

    if (exists $object_by_identifier_cache{$identifier}) {
      if (defined $object_by_identifier_cache{$identifier}) {
#        warn "  found in CACHE\n";
        my ($type, $obj_id) = @{$object_by_identifier_cache{$identifier}};
        push @{$objects_by_type{$type}}, $obj_id;
      } else {
        push @{$objects_by_type{UNKNOWN}}, $identifier;
#        warn "  found in CACHE - UNKNOWN\n";
      }
      next IDENTIFIER;
    }

    for my $type (@types) {
      my $lc_type = lc $type;
#      warn "  type: $type\n";

      my @class_keys = @{$keys_for_type{$type}};

      for my $class_key_field (@class_keys) {
#        warn "  class_key_field: $class_key_field\n";

        if ($class_key_field eq 'taxonid') {
          # doesn't work
          next;
        }

        my $quoted_identifier = quotemeta $identifier;

        my $eval_string = <<"EOF";
          InterMine::${type}::Manager->get_${lc_type}s_iterator(
                 query =>
                      [
                        '$class_key_field', { ilike => "$quoted_identifier" },
                      ],
                   );
EOF

        my $iter = eval $eval_string;
        die $@ if $@;

        while (my $obj = $iter->next) {
#          warn "   found - cache now ", scalar(keys %object_by_identifier_cache) ,"\n";
          push @{$objects_by_type{$type}}, $obj->id();
          add_to_cache($obj, $type, \%keys_for_type);
          next IDENTIFIER;
        }
      }
    }

#    warn "  NOT found\n";

    push @{$objects_by_type{UNKNOWN}}, $identifier;
    $object_by_identifier_cache{$identifier} = undef;
  }

  return %objects_by_type;
}

sub add_to_cache
{
  my $obj = shift;
  my $type = shift;
  my $keys_for_type_ref = shift;
  my @class_keys = @{$keys_for_type_ref->{$type}};

  for my $class_key_field (@class_keys) {
    if ($class_key_field eq 'taxonid') {
      # doesn't work
      next;
    }

    my $field_value = eval "\$obj->$class_key_field()";

    die $@ if $@;

    if (!defined $field_value) {
      next;
    }

    $object_by_identifier_cache{$field_value} = [$type, $obj->id()];
  }
}
