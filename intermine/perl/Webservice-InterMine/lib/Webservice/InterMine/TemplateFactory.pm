package Webservice::InterMine::TemplateFactory;

=head1 NAME

Webservice::InterMine::TemplateFactory - Service for making Webservice::InterMine
template queries using the web service

=head1 SYNOPSIS

  my $factory = Webservice::InterMine::TemplateFactory->new($xml, $model);

  my @templates = $factory->get_templates;
  my $template  = $factory->get_template($name);
  my @matching_templates = $factory->search_for($keyword);

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine::TemplateFactory

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2010 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=head1 FUNCTIONS

=cut

use Moose;
with 'Webservice::InterMine::Role::ModelOwner';
use MooseX::Types::Moose qw/Str HashRef/;
use Webservice::InterMine::Types qw/File Service Template DomNode/;
use Webservice::InterMine::Query::Template;
use XML::DOM;
use Carp qw/carp confess/;

around BUILDARGS => sub {
    my $orig  = shift;
    my $class = shift;

    if ( @_ == 1 && ref $_[0] eq 'ARRAY' ) {
        confess "Not enough elements in arrayref to new"
          if ( @{ $_[0] } < 3 );
        return $class->$orig(
            service       => $_[0]->[0],
            model         => $_[0]->[1],
            source_string => $_[0]->[2],
        );
    } else {
        return $class->$orig(@_);
    }
};

sub BUILD {
    my $self = shift;

    unless ( $self->source_string ) {
        confess
"source xml must be passed to a TemplateFactory as either a string or a file";
    }
}

has service => (
    is       => 'ro',
    isa      => Service,
    required => 1,
);

has source_file => (
    is      => 'ro',
    isa     => File,
    trigger => \&set_xml,
);

has source_string => (
    is      => 'ro',
    writer  => '_set_source_string',
    isa     => Str,
    trigger => \&process_xml,
);

has template_list => (
    traits  => ['Hash'],
    isa     => HashRef[Template],
    default => sub { {} },
    handles => {
        _set_template      => "set",
        _get_template      => "get",
        _get_templates     => "values",
        _get_parsed_count  => 'count',
    },
);

has xml_list => (
    traits => ['Hash'],
    isa => HashRef[DomNode],
    default => sub { {} },
    handles => {
        _set_template_xml   => 'set',
        _get_template_xml   => 'get',
        get_template_names  => "keys",
        get_template_count  => 'count',
    },
);

sub set_xml {
    my $self = shift;
    my $file = shift;
    open( my $XMLFH, '<', $file )
      or confess "Cannot read from xml file, $!";
    my $xml = join( '', <$XMLFH> );
    close $XMLFH
      or confess "EEK, what happened there? $!";
    $self->_set_source_string($xml);
}

sub process_xml {
    my $self             = shift;
    my $xml              = shift;
    my $parser = XML::DOM::Parser->new;
    my $doc = eval {$parser->parse($xml)};
    if (my $e = $@) {
        confess "Error parsing template XML: '$xml';", $e;
    }
    my @templates = $doc->getElementsByTagName('template');
    for my $template (@templates) {
        my $name = $template->getAttribute('name');
        if ($self->_get_template_xml($name)) {
            confess "Found two templates with the same name: $name";
        }
        warn "FOUND TEMPLATE $name" if $ENV{DEBUG};
        $self->_set_template_xml($name, $template);
    }
}

sub get_template_by_name {
    my ($self, $name) = @_;
    if (my $template = $self->_get_template($name)) {
        return $template;
    }
    if (my $xml = $self->_get_template_xml($name)) {
        warn "PARSING $name" if $ENV{DEBUG};
        my $t = eval {
            Webservice::InterMine::Query::Template->new(
                service       => $self->service,
                model         => $self->model,
                source_string => $xml->toString(),
            );
        };
        if (my $e = $@) {
            confess "Error parsing template $name: (", $xml->toString(), ") ", $e;
        } 
        $self->_set_template($name, $t);
        return $t;
    } else {
        return;
    }
}

sub get_templates {
    my $self = shift;
    if ($self->_get_parsed_count == $self->get_template_count) {
        return $self->_get_templates;
    } else {
        return grep {defined} map {eval{$self->get_template_by_name($_)}} $self->get_template_names;
    }
}

__PACKAGE__->meta->make_immutable;
no Moose;

1;

__END__

=head1 AUTHOR

Alex Kalderimis C<< <dev@intermine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine

You can also look for information at:

=over 4

=item * Webservice::InterMine

L<http://www.intermine.org>

=item * Documentation

L<http://www.intermine.org/perlapi>

=back

=head1 COPYRIGHT AND LICENSE

Copyright 2006 - 2010 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=cut
