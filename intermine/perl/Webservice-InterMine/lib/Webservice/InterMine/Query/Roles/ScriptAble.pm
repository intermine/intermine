package Webservice::InterMine::Query::Roles::ScriptAble;

use Moose::Role;
use Perl::Tidy;

requires qw(constraints joins service joined_view);

my $NEWLINE      = "\n";
my $SPACE        = q{ };
my $ENDLINE      = q{;};
my $SINGLE_QUOTE = q{'};
my $DOUBLE_QUOTE = q{"};
my $TAB          = $SPACE x 4;
my $COMMA        = q{,};

sub single_quoted {
    my $string = shift;
    return $SINGLE_QUOTE . $string . $SINGLE_QUOTE;
}

sub double_quoted {
    my $string = shift;
    return $DOUBLE_QUOTE . $string . $DOUBLE_QUOTE;
}

sub tidy {
    my $string = shift;
    my $tidy_string;
    perltidy( source => \$string, destination => \$tidy_string );
    return $tidy_string;
}

sub to_script {
    my $self          = shift;
    my $script_buffer = '#!/usr/bin/perl';
    $script_buffer .= $NEWLINE x 2
      . join( $SPACE,
        qw(use Webservice::InterMine),
        single_quoted( $self->service->root ) )
      . $ENDLINE
      . $NEWLINE x 1;
    $script_buffer .=
        'my $query = Webservice::InterMine->new_query' 
      . $ENDLINE
      . $NEWLINE x 2;
    $script_buffer .=
        '$query->add_view('
      . join( $COMMA, map( { single_quoted($_) } $self->views ) ) . ')'
      . $ENDLINE
      . $NEWLINE x 2;
    for my $elems (qw/joins constraints/) {
        for my $elem ( $self->$elems ) {
            $script_buffer .=
              '$query->add_' . substr( $elems, 0, -1 ) . '(';
            while ( my ( $key, $value ) = each %$elem ) {
                next if ($key =~ /^_/);
                $script_buffer .=
                  $TAB . $key . ' => ' . single_quoted($value) . $COMMA;
            }
            $script_buffer .= ')' . $ENDLINE;
        }
        $script_buffer .= $NEWLINE;
    }
    $script_buffer .=
      $NEWLINE . 'my $results = $query->results' . $ENDLINE;
    return tidy($script_buffer);
}

1;

# use Webservice::InterMine 'www.flymine.org';
#
# my $query = Webservice::InterMine->new_query;
#
# $query->add_view(@view);
#
# $query->add_constraint(
#     pathkey => 'pathvalue',
# );
#
# my $results = $query->run;
