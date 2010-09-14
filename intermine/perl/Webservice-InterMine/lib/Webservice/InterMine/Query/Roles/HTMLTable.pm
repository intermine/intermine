package Webservice::InterMine::Query::Roles::HTMLTable;

use Moose::Role;

requires qw(results_iterator views);

my $role = 'Webservice::InterMine::ResultIterator::Role::HTMLTableRow';

sub results_as_html_table {
    my $self = shift;
    my $ri = $self->results_iterator( with => [$role] );
    die $ri->status_line unless $ri->is_success;
    my $table_string = '<table>';
    $table_string .=
      "<tr>" . join( '', map { "<td>$_</td>" } $self->views ) . "</tr>";
    while ( my $row = $ri->html_row ) {
        $table_string .= $row;
    }
    $table_string .= '</table>';
    return $table_string;
}

1;
