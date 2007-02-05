#!/usr/bin/perl -w

use strict;

BEGIN {
  push @INC, "$ENV{HOME}/svn/dev/intermine/perl/lib";
}

use Apache::DBI;
use FreezeThaw qw(freeze thaw);

my @types;

BEGIN {
  # hard coded path at the moment - needs fixing
  $InterMine::model_file = '/tmp/genomic_model.xml';
  $InterMine::properties_file = "$ENV{HOME}/flymine.properties";
  $InterMine::db_prefix = 'db.production';
  $InterMine::class_keys = "$ENV{HOME}/svn/dev/bio/core/props/resources/class_keys.properties";
  @types = qw(Gene Protein Exon GOTerm Transcript Organism);
}

use GDBM_File;

my %object_by_identifier_cache = ();

tie %object_by_identifier_cache, 'GDBM_File', 'object_by_identifier_cache', &GDBM_WRCREAT, 0640;;

use Data::Dumper;
# print Data::Dumper->Dump([\%object_by_identifier_cache]);

# specify which types/classes will be used
use InterMine @types;
use InterMine qw(Synonym);

use XML::Simple qw(:strict);

$Rose::DB::Object::QueryBuilder::Debug = 0;

my $xs = new XML::Simple(ForceArray => 1, KeepRoot => 1, KeyAttr => []);

my $in_file_name = shift;

my $parsed = $xs->XMLin($in_file_name);

# open DUMP, '>parsed_userprofile' or die;
# my $dumper = new Data::Dumper([$parsed]);
# $dumper->Indent(1);
# print DUMP $dumper->Dump();
# close DUMP;

my @userprofiles = @{$parsed->{userprofiles}[0]{userprofile}};

my %user_bags = ();
my %unknown_map = ();

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

      open UNK, '>>unknowns' or die;
      for my $unknown (@{$objects_by_type{UNKNOWN}}) {
        print UNK "$unknown\n";
      }
      close UNK;

#      print Data::Dumper->Dump ([\%objects_by_type]);


      $user_bags{$username}{$bag_name} = \%objects_by_type;

      for my $type_and_org (keys %objects_by_type) {
        # %objects_by_type will always have at least 1 key: "UNKNOWN"
        if ($type_and_org eq 'UNKNOWN') {
          if (@{$objects_by_type{$type_and_org}}) {
            push @{$unknown_map{$username}{$bag_name}}, @{$objects_by_type{$type_and_org}};
          }
        } else {
          my @obj_ids = @{$objects_by_type{$type_and_org}};
          my $new_bag_name;
          if (keys %objects_by_type == 2) {
            # bag has only one type - just convert
            $new_bag_name = $bag_name;
          } else {
            # bag needs splitting
            $new_bag_name = "$bag_name ($type_and_org)";
          }

          my $type = ($type_and_org =~ /^(\w+)/)[0];

          push @new_bags, {
                           name => $new_bag_name,
                           type => $type,
                           bagElement => [ map { { id => $_, type => $type } } @obj_ids ],
                          };
        }
      }
    }

    @{$userprofile->{bags}[0]{bag}} = @new_bags;
  }
}

open DUMP, '>userprofile_perl_dump' or die;
my $dumper = new Data::Dumper([\%user_bags]);
$dumper->Indent(1);
print DUMP $dumper->Dump();

open DUMP, '>unknowns_perl_dump' or die;
my $unknown_dumper = new Data::Dumper([\%unknown_map]);
$unknown_dumper->Indent(1);
print DUMP $unknown_dumper->Dump();

open F, '>processed_userprofile.xml';
print F $xs->XMLout($parsed);



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
    } else {
      warn "  found $java_type: $value - ignoring\n";
      next;
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

    if (find_in_cache(\%objects_by_type, $identifier)) {
      next IDENTIFIER;
    }

    if (find_in_cache(\%objects_by_type, lc $identifier)) {
      next IDENTIFIER;
    }

    for my $type (@types) {
#      warn "  type: $type\n";

      my @class_keys = @{$keys_for_type{$type}};

      for my $class_key_field (@class_keys) {
#        warn "  class_key_field: $class_key_field\n";

        if ($class_key_field eq 'taxonid') {
          # doesn't work
          next;
        }

        if (find_identifier($identifier, $type, $class_key_field,
                            \%objects_by_type, \%keys_for_type, 0)) {
          next IDENTIFIER;
        }
        if (find_identifier($identifier, $type, $class_key_field,
                            \%objects_by_type, \%keys_for_type, 1)) {
          next IDENTIFIER;
        }
      }
    }

    warn "  NOT found\n";

    push @{$objects_by_type{UNKNOWN}}, $identifier;
    $object_by_identifier_cache{$identifier} = "";
    $object_by_identifier_cache{lc $identifier} = "";
  }

  return %objects_by_type;
}

sub find_identifier
{
  my $identifier = shift;
  my $type = shift;
  my $class_key_field = shift;
  my $objects_by_type_ref = shift;
  my $keys_for_type_ref = shift;
  my $ilike_flag = shift;

  my $lc_type = lc $type;
  my $quoted_identifier = quotemeta $identifier;

  my $op;

  if ($ilike_flag) {
    $op = "ilike";
  } else {
    $op = "eq";
  }

  my $eval_string = <<"EOF";
          InterMine::${type}::Manager->get_${lc_type}s(
                 query =>
                      [
                        '$class_key_field', { $op => "$quoted_identifier" },
                      ],
                   );
EOF

  my $eval_res = eval $eval_string;
  my @objs = @$eval_res;
  die $@ if $@;

#  warn "   matches: ", scalar(@objs), "\n";

  if (scalar (@objs) == 0) {
    return 0;
  }

  for my $obj (@objs) {
    my $organism_name;

    eval {
      $organism_name = $obj->organism()->name();
    };

#    warn "   found - cache now ", scalar(keys %object_by_identifier_cache) ,"\n";
    if (defined $organism_name) {
      push @{$objects_by_type_ref->{"$type objects from $organism_name"}}, $obj->id();
    } else {
      push @{$objects_by_type_ref->{"$type objects"}}, $obj->id();
    }
    add_to_cache($obj, $type, $keys_for_type_ref);
  }

  return scalar(@objs);
}

sub find_in_cache
{
  my $ref = shift;
  my $identifier = shift;

  if (exists $object_by_identifier_cache{$identifier}) {
    if (defined $object_by_identifier_cache{$identifier} &&
        $object_by_identifier_cache{$identifier} !~ /^\s*$/) {
#      warn "  found in CACHE: ", $object_by_identifier_cache{$identifier}, "\n";
      my @objs = thaw $object_by_identifier_cache{$identifier};

      for my $stored (@objs) {
        my $id = $stored->{id};
        my $type = $stored->{type};
        my $organism = $stored->{organism};

        if (defined $organism) {
          push @{$ref->{"$type objects from $organism"}}, $id;
        } else {
          push @{$ref->{"$type objects"}}, $id;
        }
      }

    } else {
      push @{$ref->{UNKNOWN}}, $identifier;
#      warn "  found in CACHE - UNKNOWN $identifier\n";
    }
    return 1;
  }

#  warn "      $identifier not in cache\n";

  return 0;
}

sub add_to_cache
{
  my $obj = shift;
  my $type = shift;

  my $organism_name;

  eval {
    $organism_name = $obj->organism()->name();
  };

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

    my @objs_to_store = ();

    if (exists $object_by_identifier_cache{$field_value}) {
      @objs_to_store = thaw $object_by_identifier_cache{$field_value};
    }

    if (!grep {$_->{id} == $obj->id()} @objs_to_store) {
      push @objs_to_store, {
        id => $obj->id(),
        type => $type,
        organism => $organism_name,
      };
    }

    $object_by_identifier_cache{$field_value} = freeze @objs_to_store;

    if (exists $object_by_identifier_cache{lc $field_value}) {
      @objs_to_store = thaw $object_by_identifier_cache{lc $field_value};
    }

    if (!grep {$_->{id} == $obj->id()} @objs_to_store) {
      push @objs_to_store, {
        id => $obj->id(),
        type => $type,
        organism => $organism_name,
      };
    }

    $object_by_identifier_cache{lc $field_value} = freeze @objs_to_store;
  }
}

untie %object_by_identifier_cache;
