package Webservice::InterMine::Query::Roles::WriteExcel;

use Moose::Role;
use Spreadsheet::WriteExcel;

requires qw(views results_iterator);

sub results_as_xls {
    my $self = shift;
    my $file = shift;

    my $workbook = Spreadsheet::WriteExcel->new($file);
    my $sheet    = $workbook->add_worksheet;

    my $bold_format = $workbook->add_format;
    $bold_format->set_bold;

    my $row = 0;
    my @longest = map { 0 } $self->views;

    my $ri = $self->results_iterator;
    confess $ri->status_line unless $ri->is_success;

    while ( my $a = $ri->arrayref ) {
        for my $col ( 0 .. $#{$a} ) {
            if ( $row == 0 ) {
                $sheet->write( $row, $col, $a->[$col], $bold_format );
            } else {
                $sheet->write( $row, $col, $a->[$col] );
            }
            if ( $longest[$col] < length( $a->[$col] ) ) {
                $longest[$col] = length( $a->[$col] );
            }
        }
        $row++;
    }

    for ( 0 .. $#longest ) {
        $sheet->set_column( $_, $_, $longest[$_] );
    }

    $workbook->close;
}

1;
