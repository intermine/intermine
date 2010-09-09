package InterMine::Query::Roles::ReadInAble;

use Moose::Role;
use InterMine::TypeLibrary qw(File QueryHandler);
use MooseX::Types::Moose qw(Str);

use XML::Parser::PerlSAX;

requires (
    qw/name view sort_order logic
       _build_handler add_pathdescription
       add_join add_constraint/
);

has handler => (
    is         => 'ro',
    isa        => QueryHandler,
    lazy_build => 1,
);

has source_string => (
    is => 'rw',
    isa => Str,
    trigger => \&process_xml,
);

has source_file => (
    is => 'ro',
    isa => File,
    trigger => \&set_xml,
);

sub set_xml {
    my $self = shift;
    my $file = shift;
    open (my $XMLFH, '<', $file)
	or confess "Cannot read from xml file, $!";
    my $xml = join('', <$XMLFH>);
    close $XMLFH
	or confess "EEK, what happened there? $!";
    $self->source_string($xml);
}

sub process_xml {
    my $self = shift;
    my $handler = $self->handler;
    $handler->query($self);
    my $parser  = XML::Parser::PerlSAX->new(Handler => $handler);
    $parser->parse(Source => {String => $self->source_string});
}

1;
