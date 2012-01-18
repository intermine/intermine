package InterMine::Model;

use strict;
use warnings;

use Carp qw/confess/;
use Moose::Util::TypeConstraints;
use XML::Parser::PerlSAX;
use InterMine::Model::Handler;
use Time::HiRes qw/gettimeofday/;

use constant TYPE_PREFIX => "InterMine";

our $VERSION = '0.9900';

=head1 NAME

InterMine::Model - the representation of an InterMine model

=head1 SYNOPSIS

  use InterMine::Model;

  my $model_file = 'flymine/dbmodel/build/model/genomic_model.xml';
  my $model = InterMine::Model->new(file => $model_file);
  my $gene = $model->make_new(
    Gene => {
        primaryIdentifier => "FBgn0004053",
        secondaryIdentifier => "CG1046",
        symbol              => "zen",
        name                => "zerknullt",
        length              => "3474",
        organism            => {
            shortName => "D. melanogaster",
        }
        ncbiGeneNumber      => 40828,
    });

  $gene->getName(); # "zerknullt"

  ...

=head1 DESCRIPTION

The class is the Perl representation of an InterMine data model.  The
C<new()> method can parse the model file.  The
C<get_classdescriptor_by_name()> method will return an
InterMine::Model::ClassDescriptor object for the class with the given
name, and the C<make_new()> method will return an instantiated object
of the given class.

For an example model see:
L<http://trac.flymine.org/browser/trunk/intermine/objectstore/model/testmodel/testmodel_model.xml>

=head1 CLASS METHODS

=cut

=head2 new( %options )

Standard constructor - accepts key/value pairs. Possible options are:

=over 4

=item * source: the source of the xml 

can be a ScalarRef, filehandle, filename, or string (or anything that overloads "")
(tested in that order)

=item * file: The file to load the model from 

[deprecated - use source instead] 

=item * string: A string containing the xml to load the model from 

[deprecated - use source instead]

=item * origin: Where this model comes from 

usually a mine name - optional

=back

=cut

sub new {
    my $class = shift;
    my %opts  = @_;

    print join('=>', @_) if $ENV{DEBUG};

    my $source = $opts{source} || $opts{file} || $opts{string}
        or confess "No source passed to $class constructor";

    my $self  = {%opts};

    $self->{class_hash}   = {};
    $self->{object_cache} = {};

    bless $self, $class;

    { 
        no warnings 'newline';

        if      (ref $source eq 'SCALAR') {
            $self->_process_string($$source);
        } elsif (ref $source eq 'GLOB') {
            $self->_process_string(join('', <$source>));
        } elsif (-r $source || $opts{file}) {
            $self->_process_file($source);
        } else {
            $self->_process_string("$source");
        }
    }

    $self->_fix_class_descriptors();

    return $self;
}

sub _process_string {
    my ($self, $string) =  @_;
    return $self->_process($string, 1);
}

sub _process_file {
    my ($self, $filename) = @_;
    -r $filename || confess "Cannot read model source file $filename. Aborting";
    return $self->_process($filename, 0);
}

sub _process {
    my $self             = shift;
    my $source_arg       = shift;
    my $source_is_string = shift;

    warn "PARSING MODEL " . gettimeofday() if $ENV{DEBUG};
    my $handler = new InterMine::Model::Handler( model => $self );
    my $parser = XML::Parser::PerlSAX->new( Handler => $handler );

    my $source;

    if ($source_is_string) {
        $source = { String => $source_arg };
    }
    else {
        $source = { SystemId => $source_arg };
    }

    $parser->parse( Source => $source );
    warn "FINISHED PARSING MODEL " . gettimeofday() if $ENV{DEBUG};
}

sub _add_type_constraint_and_coercion {
    my $self = shift;
    my $class_name = shift;

    subtype $class_name, as "Object", where {$_->isa($self->{perl_package} . $class_name)};
    subtype "ArrayOf" . $class_name, as "ArrayRef[$class_name]";
    coerce $class_name, from 'HashRef', via {
        $self->make_new(($_->{class} || $class_name), $_);
    };
    subtype "ArrayOfHashes", as "ArrayRef[HashRef]";

    coerce "ArrayOf$class_name", from "ArrayOfHashes", 
        via { [map {$self->make_new(($_->{class} || $class_name), $_)} @$_] };
}

use Moose::Meta::Class;

# add fields from base classes to sub-classes so that $class_descriptor->fields()
# returns fields from base classes too
sub _fix_class_descriptors {
    my $self = shift;
#
#    warn "BUILDING MODEL " . gettimeofday() if $ENV{DEBUG};
#    for my $class_name (keys %{ $self->{class_hash} } ) {
#        $self->_add_type_constraint_and_coercion($class_name);
#    }
#
#    while ( my ( $class_name, $cd ) = each %{ $self->{class_hash} } ) {
#        my @fields = $self->_get_fields($cd);
#        for my $field (@fields) {
#            $cd->add_field($field);
#        }
#        $cd->_make_fields_into_attributes();
#        $cd->make_immutable;
#    }
#    warn "FINISHED BUILDING MODEL " . gettimeofday() if $ENV{DEBUG};
}

sub _fix_cd {
    my ($self, $name, $class) = @_;
    $self->_add_type_constraint_and_coercion($name);
    my @fields = $self->_get_fields($class);
    for my $field (@fields) {
        $class->add_field($field);
    }
    $class->_make_fields_into_attributes();
    #$class->make_immutable;
    $class->_set_fixed(1);
}

sub _get_fields {
    my $self = shift;
    my $cd   = shift;

    my @fields = ();

    for my $field ( $cd->fields() ) {
        my $field_name = $field->name();
        push @fields, $field;
    }

    for my $parent ( $cd->parental_class_descriptors ) {
        push @fields, $self->_get_fields($parent);
    }

    return @fields;
}

=head2 get_classdescriptor_by_name

Get the L<InterMine::Model::ClassDescriptor> (meta-class) with the given name.
 
 my $cd = $model->get_classdescriptor_by_name("Gene");

=cut

sub get_classdescriptor_by_name {
    my $self      = shift;
    my $classname = shift;

    if ( !defined $classname ) {
        confess "no classname passed to get_classdescriptor_by_name()\n";
    }

    $classname =~ s/.*:://;

    # These are always valid
    if ( $classname eq 'Integer' ) {
        return InterMine::Model::ClassDescriptor->new(
            model   => $self,
            name    => $classname,
            extends => ['id'],
        );
    }

    my $class = $self->{class_hash}{$classname}
      || $self->{class_hash}{ $self->{package_name} . $classname };
    confess "$classname not in the model" unless $class;
    unless ($class->_is_ready()) {
        $self->_fix_cd($classname, $class);
    }
    return $class;
}

=head2 make_new($class_name, [%attributes|$attributes])

Return an object of the desired class, with the attributes 
given

 my $gene = $model->make_new(Gene => {symbol => "zen", organism => {name => 'D. melanogaster}});

 say $gene->getSymbol             # "zen"
 say $gene->getOrganism->getName # "D. melanogaster"

=cut

sub make_new {
    my $self = shift;
    my $name = (ref $_[0] eq 'HASH') ? $_[0]->{class} : shift;
    my $params = (@_ == 1) ? $_[0] : {@_};

    my $obj = $self->get_classdescriptor_by_name($name)->new_object($params);

    if ($obj->hasObjectId) {
        if (my $existing = $self->{object_cache}{$obj->getObjectId}) {
            $existing->merge($obj);
            return $existing;
        } else {
            $self->{object_cache}{$obj->getObjectId} = $obj;
        }
    } else {
        return $obj;
    }
}

=head2 get_all_classdescriptors

Return all the L<InterMine::Model::ClassDescriptor>s for this model

 my @cds = $model->get_all_classdescriptors();

=cut

sub get_all_classdescriptors {
    my $self = shift;
    return values %{ $self->{class_hash} };
}

=head2 get_referenced_classdescriptor

Get the class descriptor at the other end of a reference. The main use for this 
method is internal, during the construction of a model

 my $cd = $model->get_referenced_classdescriptor($ref);

=cut

sub get_referenced_classdescriptor {
    my $self      = shift;
    my $reference = shift;
    for my $cd ( $self->get_all_classdescriptors ) {
        for my $ref ( $cd->references ) {
            if ( $ref->has_reverse_reference ) {
                if ( $ref->reverse_reference->name eq $reference ) {
                    return $cd;
                }
            }
        }
    }
    return undef;
}

=head2 find_classes_declaring_field( $name )

Get the class descriptors that declare fields of a certain name  

 my @classes = $model->find_classes_declaring_field($str);

=cut

sub find_classes_declaring_field {
    my $self       = shift;
    my $field_name = shift;
    my @returners;
    for my $cd ( $self->get_all_classdescriptors ) {
        for my $field ( $cd->get_own_fields ) {
            push @returners, $cd if ( $field->name eq $field_name );
        }
    }
    return @returners;
}

=head2 package_name

Return the package name derived from the original java name space, eg. org.intermine.model

  my $java_package = $model->package_name;

=cut

sub package_name {
    my $self = shift;
    return $self->{package_name};
}

=head2 model_name

Return the name of this model. Conceptually, this maps to the enclosing package for the 
generated classes.

 my $model_name = $model->model_name();

=cut

sub model_name {
    my $self = shift;
    return $self->{model_name};
}

=head2 to_xml

Returns a string containing an XML representation of the model.

=cut

sub to_xml {
    my $self = shift;
    my $xml = sprintf(qq{<model name="%s" package="%s">\n}, 
        $self->model_name, $self->package_name);

    for my $cd (sort($self->get_all_classdescriptors())) {
        $xml .= q[ ] x 2 . $cd->to_xml . "\n";
    }

    $xml .= "</model>";
    return $xml;
}

=head2 lazy_fetch

Always returns undef. This can be overriden by subclasses to provide lazy fetching
capabilities for items, from a web-service or directly from a database.

=cut

sub lazy_fetch { undef };

1;

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Model

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2006,2007,2008,2009, 2010, 2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

