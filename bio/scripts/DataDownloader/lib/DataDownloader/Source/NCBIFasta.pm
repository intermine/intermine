package DataDownloader::Source::NCBIFasta;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE       => "NCBI Sequence data",
    DESCRIPTION => "Fasta Files from NCBI",
    SOURCE_LINK => 'http://ncbi.nlm.nih.gov',
    HOST        => "ftp.ncbi.nlm.nih.gov",
    SOURCE_DIR  => "human/fasta",
};

use constant FILE_TYPES =>
  qr/hs_ref_GRCh38.p2_chr/;

sub BUILD {
    my $self = shift;
        for my $file ($self->ls_remote_dir("genomes/H_sapiens/Assembled_chromosomes/seq")) {
            $self->add_source(
                HOST       => 'ftp.ncbi.nlm.nih.gov',
                REMOTE_DIR => "genomes/H_sapiens/Assembled_chromosomes/seq",
                FILE       => $file,
                EXTRACT    => 1,
            ) if ( $file =~ FILE_TYPES );

    }
}

1;
