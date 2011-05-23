package Webservice::InterMine::Role::HasQuery;

use Moose::Role;

use Webservice::InterMine::Types qw(Query);

requires 'build_query';

sub view {};
sub view_size {};
sub add_view {};
sub add_views {};
sub results_iterator {};
sub results {};
sub table_format {};

has query => (
    is => 'ro',
    isa => Query,
    lazy_build => 1,
    builder => 'build_query',
    handles => [qw/
        views view_size add_view add_views 
        results_iterator results
        table_format
    /],
);

1;
