package InterMine::Model::Handler;

=head1 NAME

InterMine::Model::Handler - The SAX handler for reading in a model

=head1 SYNOPSIS

    use InterMine::Model;
    use InterMine::Model::Handler;
    use XML::Parser::PerlSAX;

    my $handler = InterMine::Model::Handler->new( model   => $self );
    my $parser  = XML::Parser::PerlSAX->new(      Handler => $handler );

    my $source;

    if ($source_is_string) {
        $source = { String => $source_arg };
    }
    else {
        $source = { SystemId => $source_arg };
    }

    $parser->parse( Source => $source );

=head1 DESCRIPTION

This in a class used internally for processing the xml representation of the model
that is returned from webservices, and stored as a the model's serialised 
representation. You will not need to use this class directly.

=head1 SEE ALSO

=over 4

=item * L<XML::Parser::PerlSAX>

=back

=cut

use strict;
use warnings;

use Carp qw/confess/;

use InterMine::Model::Attribute;
use InterMine::Model::Reference;
use InterMine::Model::Collection;
use InterMine::Model::ClassDescriptor;

use constant ORIGIN => "InterMine";

=head1 CLASS METHODS

=head2 new(model => $model)

Standard constructor. Takes options as key value pairs, and expects
just one option (model).

=cut

sub new {
    my $class = shift;
    @_ == 2 || confess "Not enough arguments to ${class}::new\n", 
                       "Expected: 'model => \$model'\n",
                       "got: ", join('=>', @_);
    my $self = {@_};

    return bless $self, $class;
}

my $serial_no = 00;

=head2 start_element

implementation of method required by XML::Parser::PerlSAX

=cut

sub start_element {
    my $self = shift;
    my $args = shift;

    $self->{current_element} = $args->{Name};

    my $nameattr = $args->{Attributes}{name};

    if ( $args->{Name} eq "model" ) {
        $self->{model}{model_name} = $nameattr;
        $self->{model}{package_name} = $args->{Attributes}{package};
    }
    else {
        my $model = $self->{model};
        if ( $args->{Name} eq "class" ) {
            my $origin = ORIGIN;
            $origin .= '::' . $self->{model}{origin} if $self->{model}{origin};
            my $perl_package  = $self->{model}{perl_package} ||= join('::', 
                $origin, $self->{model}{model_name}, sprintf("%02d", ++$serial_no)) . '::';
            my @parents = ();
            if ( exists $args->{Attributes}{extends} ) {
                @parents = split /\s+/, $args->{Attributes}{extends};
                @parents = grep { $_ ne 'java.lang.Object' } @parents;

                # strip off any preceding package (eg. "org.intermine.")
                map { s/.*\.(.*)/$1/ } @parents;
            }
            my $cd = InterMine::Model::ClassDescriptor->create(
                $perl_package . $nameattr,
                model   => $model,
                parents => [ map {$perl_package . $_} @parents],
                is_interface => ($args->{Attributes}{'is-interface'} eq 'true'),
            );
            $model->{class_hash}{$nameattr} = $cd;
            $self->{current_class} = $cd;
        }
        else {
            my $field;
            if ( $args->{Name} eq "attribute" ) {
                my $type = $args->{Attributes}{type};
                $field = InterMine::Model::Attribute->new(
                    name  => $nameattr,
                    type  => $type,
                    model => $model
                );
            }
            else {
                my $referenced_type 
                    = $args->{Attributes}{'referenced-type'};
                my $reverse_reference 
                    = $args->{Attributes}{'reverse-reference'};

                my %args = (
                    name                 => $nameattr,
                    referenced_type_name => $referenced_type,
                    model                => $model
                );
                $args{reverse_reference_name} = $reverse_reference
                  if $reverse_reference;

                if ( $args->{Name} eq "reference" ) {
                    $field = InterMine::Model::Reference->new(%args);
                }
                elsif ( $args->{Name} eq "collection" ) {
                    $field = InterMine::Model::Collection->new(%args);
                }
                else {
                    confess "unexpected element: ", $args->{Name}, "\n";
                }

            }
            $self->{current_class}->add_field( $field, 'own' );
        }
    }
}

=head2 end_element

implementation of method required by XML::Parser::PerlSAX

=cut

sub end_element {
    my $self = shift;
    my $args = shift;
    if ( $args->{Name} eq 'class' ) {
        $self->{current_class} = undef;
    }
}

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

