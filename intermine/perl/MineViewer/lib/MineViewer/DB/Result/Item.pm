package MineViewer::DB::Result::Item;

# Created by DBIx::Class::Schema::Loader
# DO NOT MODIFY THE FIRST PART OF THIS FILE

use strict;
use warnings;

use Moose;
use MooseX::NonMoose;
use namespace::autoclean;
extends 'DBIx::Class::Core';


=head1 NAME

MineViewer::DB::Result::Item

=cut

__PACKAGE__->table("item");

=head1 ACCESSORS

=head2 identifer

  data_type: 'text'
  is_nullable: 0

=cut

__PACKAGE__->add_columns("identifer", { data_type => "text", is_nullable => 0 });
__PACKAGE__->set_primary_key("identifer");

=head1 RELATIONS

=head2 comments

Type: has_many

Related object: L<MineViewer::DB::Result::Comment>

=cut

__PACKAGE__->has_many(
  "comments",
  "MineViewer::DB::Result::Comment",
  { "foreign.item" => "self.identifer" },
  { cascade_copy => 0, cascade_delete => 0 },
);


# Created by DBIx::Class::Schema::Loader v0.07010 @ 2011-10-10 18:16:03
# DO NOT MODIFY THIS OR ANYTHING ABOVE! md5sum:nxffAAPKfZwWNPek8sbk8w


# You can replace this text with custom code or comments, and it will be preserved on regeneration
__PACKAGE__->meta->make_immutable;
1;
