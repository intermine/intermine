#!/usr/bin/perl

# returns a list of classes in a model, for use in obo_to_model

use InterMine::Model;

if (@ARGV != 1) {
  die "$0: need name of a mine, eg. flymine";
}

my $mine = $ARGV[0];
my $svnpath = "model_update";

my $build_dir = "../../$mine/dbmodel/build/model";
my $f = "$build_dir/genomic_model.xml";

my $model = new InterMine::Model(file => $f);

read_model($model);

sub read_model
{
  my (@class_names);
  my ($model) = @_;
  my @names = sort map {$_->name} $model->get_all_classdescriptors();
   
  open my $outfh, ">", "$build_dir/so_terms.txt" or die "$!";

  foreach (@names) {
      my $cd = $_;
      print $outfh $cd . "\n";
  }  
  close $outfh;
}

