package Webservice::InterMine::ResultIterator::Role::HTMLTableRow;

use Moose::Role;

requires qw(next);

sub html_row {
    my $self = shift;
    my $row  = $self->next;
    return unless ( defined $row );
    my $output = "<tr>";
    for (@$row) {
        $output .= (defined $_) ? "<td>$_</td>" : "<td>[NONE]</td>";
    }
    $output .= "</tr>";
    return $output;
}

1;
