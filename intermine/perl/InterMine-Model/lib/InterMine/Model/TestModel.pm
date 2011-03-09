package InterMine::Model::TestModel;

use strict;
use warnings;

use File::Basename;

use InterMine::Model;

my $model_file = dirname(__FILE__) . '/testmodel_model.xml';

my $instance = __PACKAGE__->new;

sub new {
    return InterMine::Model->new(source => $model_file, origin => "TestModel");
}

sub instance {
    return $instance;
}

1;

=head1 NAME

InterMine::Model::TestModel - A model to make testing easy for other modules.

=head1 SYNOPSIS

 use InterMine::Model::TestModel;

 # For a shared instance:
 my $singleton = InterMine::Model::TestModel->instance;

 # For a freshly minted model:
 my $model = InterMine::Model::TestModel->new;

=head1 DESCRIPTION

This module produces a predefined version of an L<InterMine::Model> for 
testing purposes. The model itself has 19 classes based around
a hierarchical company structure. 

To view the model as an xml file, see:
L<http://trac.flymine.org/browser/trunk/intermine/objectstore/model/testmodel/testmodel_model.xml>

Useful classes in the model are 

=over 4

=item Employee (analogous to Gene in genomic models)

 name (Str)
 age  (Int)
 fullTime (Bool)
 department (Department)
 address (Address)

=item Department (analogous to Chromosome in genomic models)

 employees (ArrayRef[Employee])
 company (Company)
 manager (Manager)
 name (Str)
 address (Address)

=item Company (

 departments (ArrayRef[Department])
 name (Str)
 vatNumber (Int)
 contractors (ArrayRef[Contractor])
 secretarys (ArrayRef[Secretary])
 address (Address)

=back

=head1 CLASS METHODS

=head2 new

returns a new instance of the test model

=head2 instance

returns a shared instance of the testmodel

In most cases you will want to use C<instance>, as 
the model is for the most part read-only.

You only need to use C<new> if you are testing the 
object cache.

=head1 SEE ALSO

=over 4

=item L<InterMine::Model>

=back

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Model::TestModel

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2006,2007,2008,2009, 2010, 2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

