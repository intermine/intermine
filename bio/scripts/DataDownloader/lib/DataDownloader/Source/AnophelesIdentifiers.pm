package DataDownloader::Source::AnophelesIdentifiers;

use Moose;
extends 'DataDownloader::Source::ABC';
use LWP;

# ftp://ftp.vectorbase.org/public_data/organism_data/agambiae/Other/AgamP3.3-to-AgamP3.4-Identifiers.tgz

use constant {
    TITLE => 'Anopheles Identifiers',
    DESCRIPTION => "Gene Stable ID mappings from geneset AgamP3.3 to AgamP3.4",
    SOURCE_LINK => 'http://agambiae.vectorbase.org/GetData/Downloads/',
    SOURCE_DIR  => 'anopheles-identifiers',
};

sub BUILD {
    my $self = shift;
    my @files_to_extract = {"New_IDs_to_Old_IDs-Genes.tsv", "New_IDs_to_Old_IDs-Transcripts.tsv"};

    $self->set_sources([
        {
            SERVER => 'ftp://ftp.vectorbase.org/public_data/organism_data/agambiae/Other',
            FILE => 'AgamP3.3-to-AgamP3.4-Identifiers.tgz',

            CLEANER => sub {
                my $self = shift;
                my $file = $self->get_destination;
                my @args = ('unzip', $file, @files_to_extract, '-d', 
                    $self->get_destination_dir);
                $self->execute_system_command(@args);
                $self->debug("Removing original file: $file");
                unlink($file);
            },
        },
    ]);
}

