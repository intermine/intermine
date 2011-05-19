package MineViewer::DB::Result::Gene;

# Created by DBIx::Class::Schema::Loader
# DO NOT MODIFY THE FIRST PART OF THIS FILE

use strict;
use warnings;

use Moose;
use MooseX::NonMoose;
use namespace::autoclean;
extends 'DBIx::Class::Core';


=head1 NAME

MineViewer::DB::Result::Gene

=cut

__PACKAGE__->table("gene");

=head1 ACCESSORS

=head2 gene_id

  data_type: 'integer'
  is_auto_increment: 1
  is_nullable: 0

=head2 identifer

  data_type: 'text'
  is_nullable: 0

=cut

__PACKAGE__->add_columns(
  "gene_id",
  { data_type => "integer", is_auto_increment => 1, is_nullable => 0 },
  "identifer",
  { data_type => "text", is_nullable => 0 },
);
__PACKAGE__->set_primary_key("gene_id");
__PACKAGE__->add_unique_constraint("identifer_unique", ["identifer"]);

=head1 RELATIONS

=head2 comments

Type: has_many

Related object: L<MineViewer::DB::Result::Comment>

=cut

__PACKAGE__->has_many(
  "comments",
  "MineViewer::DB::Result::Comment",
  { "foreign.gene" => "self.gene_id" },
  { cascade_copy => 0, cascade_delete => 0 },
);


# Created by DBIx::Class::Schema::Loader v0.07010 @ 2011-05-12 15:10:48
# DO NOT MODIFY THIS OR ANYTHING ABOVE! md5sum:PnEgTh5vWhLrRNIecZPW0w


# You can replace this text with custom code or comments, and it will be preserved on regeneration
__PACKAGE__->meta->make_immutable;
1;
