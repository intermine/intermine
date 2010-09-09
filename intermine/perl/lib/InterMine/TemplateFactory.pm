package InterMine::TemplateFactory;

=head1 NAME

InterMine::TemplateFactory - Service for making InterMine
template queries using the web service

=head1 SYNOPSIS

  my $factory = InterMine::TemplateFactory->new($xml, $model);

  my @templates = $factory->get_templates;
  my $template  = $factory->get_template($name);
  my @matching_templates = $factory->search_for($keyword);

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::TemplateFactory

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
with 'InterMine::Roles::CommonAttributes' => {
    excludes => [qw/name description/],
};
use MooseX::Types::Moose qw/Str/;
use InterMine::TypeLibrary qw/File TemplateHash Service/;
use InterMine::Query::Template;

around BUILDARGS => sub {
    my $orig = shift;
    my $class = shift;

    if ( @_ == 1 && ref $_[0] eq 'ARRAY') {
	confess "Not enough elements in arrayref to new" if (@{$_[0]} < 3);
	return $class->$orig(
	    service => $_[0]->[0],
	    model   => $_[0]->[1],
	    source_string => $_[0]->[2],
	);
    }
    else {
	return $class->$orig(@_);
    }
};

sub BUILD {
    my $self = shift;

    unless ($self->source_string) {
	confess "source xml must be passed to a TemplateFactory as either a string or a file";
    }
}

has service => (
    is => 'ro',
    isa => Service,
    required => 1,
);

has source_file => (
    is => 'ro',
    isa => File,
    trigger => \&set_xml,
);

has source_string => (
    is => 'ro',
    writer => '_set_source_string',
    isa => Str,
    trigger => \&process_xml,
);

has template_list => (
    traits => ['Hash'],
    isa => TemplateHash,
    default => sub { {} },
    handles => {
	_set_template => "set",
	get_template_by_name => "get",
	get_templates => "values",
	get_template_names => "keys",
    },
);

sub set_xml {
    my $self = shift;
    my $file = shift;
    open (my $XMLFH, '<', $file)
	or confess "Cannot read from xml file, $!";
    my $xml = join('', <$XMLFH>);
    close $XMLFH
	or confess "EEK, what happened there? $!";
    $self->_set_source_string($xml);
}

sub process_xml {
    my $self = shift;
    my $xml  = shift;
    my @template_strings = $xml
	=~ m!(<template .*?</template>)!sg;
    confess "Can't find any template strings in the xml I was passed"
	unless @template_strings;
    for (@template_strings) {
	my $t = InterMine::Query::Template->new(
	    service       => $self->service,
	    model         => $self->model,
	    source_string => $_,
	);
	my $name = $t->name;
	confess "Made two templates with the same name - $name"
	    if $self->get_template_by_name($name);
	$self->_set_template($name, $t);
    }
}

=head1 AUTHOR

Alex Kalderimis C<< <dev@intermine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine

You can also look for information at:

=over 4

=item * InterMine

L<http://www.intermine.org>

=item * Documentation

L<http://www.intermine.org/perlapi>

=back

=head1 COPYRIGHT AND LICENSE

Copyright 2006 - 2010 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=cut


1;
