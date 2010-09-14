package Webservice::InterMine::ResultIterator::Role::HTMLTableRow;

use Moose::Role;

requires qw(arrayref);

sub html_row {
    my $self = shift;
    my $row  = $self->arrayref;
    return unless (defined $row);
    my $output = "<tr>";
    for (@$row) {
	$output .= "<td>$_</td>";
    }
    $output .= "</tr>";
    return $output;
}

1;
