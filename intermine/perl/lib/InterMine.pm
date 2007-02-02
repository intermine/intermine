package InterMine;

use warnings;
use strict;

=head1 NAME

InterMine - perl code for accessing an InterMine data warehouse

=head1 VERSION

Version 0.01

=cut

our $VERSION = '0.01';

=head1 SYNOPSIS

=head1 EXPORT

=head1 AUTHOR

FlyMine, C<< <perl@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<< <perl@flymine.org> >>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=item * AnnoCPAN: Annotated CPAN documentation

L<http://annocpan.org/dist/InterMine>

=item * CPAN Ratings

L<http://cpanratings.perl.org/d/InterMine>

=item * RT: CPAN's request tracker

L<http://rt.cpan.org/NoAuth/Bugs.html?Dist=InterMine>

=item * Search CPAN

L<http://search.cpan.org/dist/InterMine>

=back

=head1 ACKNOWLEDGEMENTS

=head1 COPYRIGHT & LICENSE

Copyright 2006 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=cut

use InterMine::Model;
use Config::Properties;

our $model_file;
our $properties_file;
our $db_prefix;
our $class_keys;
local %InterMine::class_keys_map = ();

our $model;

sub get_model {
  die "model_file not set\n" unless defined $model_file;

  if (defined $model) {
    return $model;
  } else {
    $model = new InterMine::Model(file => $model_file);
  }
}

sub get_class_keys
{
  my $class_name = shift;

  die "class_keys not set\n" unless defined $class_keys;

  if (scalar(keys %InterMine::class_keys_map) == 0) {
    open PROPS, '<', $class_keys
      or die "unable to open class keys file: $InterMine::class_keys";

    my $properties = new Config::Properties();
    $properties->load(*PROPS);

    for my $class_name ($properties->propertyNames()) {
      $InterMine::class_keys_map{$class_name} =
        [split /[,\s]+/, $properties->getProperty($class_name)];
    }
  }

  my @class_keys = ();
  if (exists $InterMine::class_keys_map{$class_name}) {
    @class_keys = @{$InterMine::class_keys_map{$class_name}};
  }
  my $class_desc = get_model()->get_classdescriptor_by_name($class_name);
  for my $extendee_class_desc ($class_desc->extends_class_descriptors()) {
    push @class_keys, get_class_keys($extendee_class_desc->unqualified_name());
  }

  return @class_keys;
}

sub import {
  my $pkg = shift;

  my @classes = @_;

  if (grep /__ALL__/, @classes) {
    @classes = map {($_->name() =~ m/.*\.(.*)/)} get_model()->get_all_classdescriptors();
  }

  my @classes_to_import = ();

  for my $class (@classes) {
    my $cd = get_model()->get_classdescriptor_by_name($class);

    my %setup_args = (primary_key_columns => [ 'id' ]);
    my @columns = (id => { type => 'int', primary_key => 1, not_null => 1 });
    my @relationships = ();
    my @foreign_keys = ();

    for my $field ($cd->fields()) {

      if ($field->field_type() eq 'attribute') {
        push @columns, _pg_identifier_from_intermine($field->field_name()), {type => $field->attribute_type()}
      } else {
        my $referenced_type_name = $field->referenced_type_name();
        $referenced_type_name =~ s/.*\.(.*)/InterMine::$1/;
        if ($field->field_type() eq 'reference') {
          push @columns, $field->field_name() . 'id', {type => 'int'};
          my $foreign_key_settings =
            {
             class => $referenced_type_name,
             key_columns => {
                             $field->field_name() . 'id',
                             'id'
                            }
            };
          push @foreign_keys, $field->field_name(), $foreign_key_settings;
        } else {
           if ($field->is_one_to_many()) {
             my $reverse_reference_field_name =
               $field->reverse_reference()->field_name();
             my $relationship_settings =
             {
               type       => 'one to many',
               class      => $referenced_type_name,
               column_map => { id => $reverse_reference_field_name . 'id' },
             };
             push @relationships, $field->field_name(), $relationship_settings;
           } else {
             if ($field->is_many_to_many()) {
               my $reverse_reference_field_name =
                 $field->reverse_reference()->field_name();
               my $map_class = _make_mapping_table($field);
               my $relationship_settings =
               {
                type      => 'many to many',
                map_class => $map_class,
                map_from  => lc $field->field_name() . "_key",
                map_to    => lc $reverse_reference_field_name . "_key",
               };
               push @relationships, $field->field_name(), $relationship_settings;
             } else {
               warn "unimplemented: ", $field->field_name(), "\n";
             }
           }
        }
      }
    }

    $setup_args{table} = $class;
    $setup_args{columns} = \@columns;
    $setup_args{foreign_keys} = \@foreign_keys;
    $setup_args{relationships} = \@relationships;
    my @unique_keys = get_class_keys($class);
    $setup_args{unique_keys} = [@unique_keys];

    my $lc_class = lc $class;

    $setup_args{table} = $class;

    eval <<"EOF";
package InterMine::$class;
use base 'InterMine::DB::Object';
__PACKAGE__->meta->setup(%setup_args);
1;

package InterMine::$class\::Manager;
use base 'Rose::DB::Object::Manager';
sub object_class { 'InterMine::$class' }
__PACKAGE__->make_manager_methods('${lc_class}s');
1;

EOF

    die $@ if $@;
  }
}



my %postgres_keywords;

my @keyword_list =
  qw(abs absolute action add admin after aggregate alias all allocate alter
     analyse analyze and any are array as asc asensitive assertion asymmetric
     at atomic authorization avg before begin between bigint binary bit
     bit_length blob boolean both breadth by call called cardinality cascade
     cascaded case cast catalog ceil ceiling char character character_length
     char_length check class clob close coalesce collate collation collect
     column commit completion condition connect connection constraint
     constraints constructor continue convert corr corresponding count
     covar_pop covar_samp create cross cube cume_dist current current_date
     current_default_tran current_path current_role current_time
     current_timestamp current_transform_gr current_user cursor cycle data
     database date day deallocate dec decimal declare default deferrable
     deferred delete dense_rank depth deref desc describe descriptor destroy
     destructor deterministic diagnostics dictionary disconnect distinct do
     domain double drop dynamic each element else end end-exec equals escape
     every except exception exec execute exists exp external extract false
     fetch filter first float floor for foreign found free freeze from full
     function fusion general get global go goto grant group grouping having
     hold host hour identity ignore ilike immediate in indicator initialize
     initially inner inout input insensitive insert int integer intersect
     intersection interval into is isnull isolation iterate join key language
     large last lateral leading left less level like limit ln local localtime
     localtimestamp locator lower map match max member merge method min minute
     mod modifies modify module month multiset names national natural nchar
     nclob new next no none normalize not notnull null nullif numeric object
     objectclass octet_length of off offset old on only open operation option
     or order ordinality out outer output over overlaps overlay pad parameter
     parameters partial partition path percentile_cont percentile_disc
     percent_rank placing position postfix power precision prefix preorder
     prepare preserve primary prior privileges procedure public range read
     reads real recursive ref references referencing regr_avgx regr_avgy
     regr_count regr_intercept regr_r2 regr_slope regr_sxx regr_sxy regr_syy
     relative release restrict result return returns revoke right role
     rollback rollup routine row rows row_number savepoint schema scope scroll
     search second section select sensitive sequence session session_user set
     setof sets similar size smallint some space specific specifictype sql
     sqlcode sqlerror sqlexception sqlstate sqlwarning sqrt start state
     statement static stddev_pop stddev_samp structure submultiset substring
     sum symmetric system system_user table tablesample temporary terminate
     than then time timestamp timezone_hour timezone_minute to trailing
     transaction translate translation treat trigger trim true uescape under
     union unique unknown unnest update upper usage user using value values
     varchar variable varying var_pop var_samp verbose view when whenever
     where width_bucket window with within without work write year zone);


@postgres_keywords{@keyword_list} = ();

sub _pg_identifier_from_intermine
{
  my $name = shift;

  if (exists $postgres_keywords{lc $name}) {
    return "intermine_$name";
  } else {
    return $name;
  }
}


sub _make_mapping_table
{
  my $field = shift;
  my $reverse_field = $field->reverse_reference();
  my $field_name = $field->field_name();
  my $reverse_field_name = $reverse_field->field_name();

  my $lc_field_name = lc $field_name;
  my $lc_reverse_field_name = lc $reverse_field_name;

  my $field_class_name = $reverse_field->field_class()->unqualified_name();
  my $reverse_field_class_name = $field->field_class()->unqualified_name();

  my $map_class = 'InterMine::' . ucfirst $field_name . ucfirst $reverse_field_name . 'Map';

  my $eval_string = <<"EOF";
package $map_class;

use base 'InterMine::DB::Object';

__PACKAGE__->meta->setup
(
 table   => '${lc_reverse_field_name}${lc_field_name}',
 columns =>
 [
  ${lc_field_name} => { type => 'int', not_null => 1 },
  ${lc_reverse_field_name} => { type => 'int', not_null => 1 },
 ],

 primary_key_columns => [ '${lc_field_name}', '${lc_reverse_field_name}' ],

 foreign_keys =>
 [
  ${lc_field_name}_key =>
  {
   class       => 'InterMine::$reverse_field_class_name',
   key_columns => { $lc_field_name => 'id' },
  },

  ${lc_reverse_field_name}_key =>
  {
   class       => 'InterMine::$field_class_name',
   key_columns => { $lc_reverse_field_name => 'id' },
  },
 ],
);
EOF

  eval $eval_string;

  return $map_class;
}

1;
