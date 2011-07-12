package DataDownloader::Source::Interpro;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use XML::Parser::PerlSAX;
use PerlIO::gzip;
use Number::Format qw(format_bytes);

use autodie qw(close open);

use constant {
    TITLE => 'InterPro protein family and domain data',
    DESCRIPTION =>
"Protein Family and Domain data for D. melanogaster, A. gambiae and C. elegans from Interpro",
    SOURCE_LINK => 'http://www.ebi.ac.uk/interpro',
    SOURCE_DIR  => 'interpro',
    HOST        => "ftp.ebi.ac.uk",
    REMOTE_DIR  => "pub/databases/interpro",
    REF_FILE    => 'interpro.xml.gz',
};

use constant ORGANISMS => qw(
  DROME CAEEL SACCE ANOGA HOMSA MUSMU RATNO 9MUSC DROPS
);

my $match_complete_post_processor = sub {
    my $self = shift;
    my ( $temp, $destination ) = @_;
    open( my ($fh), '<:gzip', $temp );
    my $out_file = substr( "$destination", 0, -3 );
    my $handler =
      MyMatchXMLHandler->new( writer => $out_file, organisms => [ORGANISMS] );
    my $parser = XML::Parser::PerlSAX->new( Handler => $handler );
    $parser->parse( Source => { ByteStream => $fh } );
    close $fh;
    $self->info("Extracted proteins from matches XML - size of $out_file: ", 
        format_bytes(-s $out_file));
};

override generate_version => sub {
    my $self       = shift;
    my $date_stamp = super;
    my $ftp        = $self->connect;
    my $notes;

    open( my ($string_buffer), '>', \$notes );
    $ftp->get( "release_notes.txt", $string_buffer )
      or die "Failed to retrieve release notes";
    close $string_buffer;
    $ftp->quit;

    if ( $notes =~ /^Release \s (\d+\.\d+) ,/mx ) {
        return $1 . '-' . $date_stamp;
    }
};

sub BUILD {
    my $self = shift;
    $self->set_sources(
        [
            {
                SUBTITLE   => "Protein domain data",
                HOST       => "ftp.ebi.ac.uk",
                REMOTE_DIR => "pub/databases/interpro",
                FILE       => 'interpro.xml.gz',
                EXTRACT    => 1,
            },
            {
                SUBTITLE       => "Domain annotation",
                HOST           => "ftp.ebi.ac.uk",
                REMOTE_DIR     => "pub/databases/interpro",
                FILE           => 'match_complete.xml.gz',
                POST_PROCESSOR => $match_complete_post_processor,
            }
        ]
    );
}

1;

package MyMatchXMLHandler;

use Moose;
use XML::Writer;
use Moose::Util::TypeConstraints;

use autodie qw(open close);
use feature 'switch';

class_type( 'XMLWriter', { class => 'XML::Writer' } );
coerce 'XMLWriter', from 'Str', via {
    open( my $fh, '>:utf8', $_ );
    my $writer = XML::Writer->new(
        OUTPUT      => $fh,
        DATA_MODE   => 1,
        DATA_INDENT => 3,
    );
    $writer->xmlDecl("UTF-8");
    $writer->doctype( "interpromatch", undef, "match_complete.dtd" );
    return $writer;
};

has organisms => (
    is       => 'ro',
    isa      => 'ArrayRef',
    traits   => ['Array'],
    handles  => { search_organisms => 'grep', },
    required => 1,
);

has protein_info => (
    is         => 'rw',
    isa        => 'HashRef',
    clearer    => 'clear_protein_info',
    predicate  => 'has_protein_info',
    auto_deref => 1,
);

sub determine_if_wanted {
    my $self       = shift;
    my $attr       = shift;
    my $is_desired = 0;
    my $name       = $attr->{name};
    if ( $name =~ /_/ ) {
        my ( undef, $species ) = split( /_/, $name, 2 );
        if ( $self->search_organisms( sub { $species eq $_ } ) ) {
            $is_desired = 1;
        }
    }
    elsif ( $name =~ /-/ ) {

        # Include all isoforms
        $is_desired = 1;
    }
    $self->is_in_desired_protein($is_desired);
    $self->protein_info($attr) if $is_desired;
}

has writer => (
    is       => 'ro',
    isa      => 'XMLWriter',
    coerce   => 1,
    required => 1,
);

has is_in_desired_protein => (
    isa     => 'Bool',
    is      => 'rw',
    default => 0,
);

sub start_element {
    my $self      = shift;
    my $args      = shift;
    my $elem_name = $args->{Name};
    my $elem_attr = $args->{Attributes};
    given ($elem_name) {
        when ('interpromatch') { $self->writer->startTag($elem_name); }
        when ('protein')       { $self->determine_if_wanted($elem_attr); }
        default {
            if ( $self->is_in_desired_protein ) {
                if ( $self->has_protein_info ) {
                    $self->writer->startTag( 'protein', $self->protein_info );
                    $self->clear_protein_info;
                }
                $self->writer->startTag( $elem_name, %{$elem_attr} );
            }
        }
    };
}

sub end_element {
    my $self      = shift;
    my $args      = shift;
    my $elem_name = $args->{Name};
    if ( $self->writer->in_element($elem_name) ) {
        $self->writer->endTag($elem_name);
    }
    if ( $elem_name eq 'protein' ) {
        $self->is_in_desired_protein(0);
        $self->clear_protein_info;
    }
}

sub end_document {
    my $self = shift;
    $self->writer->end;
}

1;
