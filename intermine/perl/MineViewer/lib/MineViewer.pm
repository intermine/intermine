package MineViewer;
use strict;
use warnings;
use Dancer ':syntax';
use Dancer::Config;
use Dancer::Plugin::DBIC;
use Dancer::Plugin::ProxyPath;
use Scalar::Util qw/blessed/;
use Try::Tiny;

# For caching results we do not expect to change
use Attribute::Memoize;

use Lingua::EN::Inflect qw(PL_N);

=head1 VERSION

Software release version: 0.01

Changes:

=over

=item * 0.01 - Initial release

Support for lists in a specific mine, and user comments 
on a per gene basis.

=back

=cut

our $VERSION = '0.1';

# InterMine client library code
use Webservice::InterMine 0.9700;
use Webservice::InterMine::Bio qw/GFF3 FASTA/;

# The connection to the main intermine webservice

my %last_update;
my $update_interval = setting('update_interval');

use constant RESULT_OPTIONS => ( as => 'jsonobjects' );

sub service {
    my $service_args = setting('service_args');
    my $service  = Webservice::InterMine->get_service(@$service_args);
    return $service;
}

before sub {
    try {
        service->refresh_lists;
    } catch {
        send_error("Could not connect to service: ($_) " . to_dumper(setting('service_args')), 500);
    }
};

before_template sub {
    my $tokens = shift;
    $tokens->{pluraliser} = \&pluraliser;
    $tokens->{service} = service();
    $tokens->{decamelise} = \&decamelise;
};

get '/' => sub {
    return redirect "/lists";
    #return template index => { lists => [get_lists()] };
};

get '/about' => sub {
    return template "about" => { lists => [get_lists()] };
};

get '/templates' => sub {
    return template 'templates' => { all_lists => [ get_lists() ], };
};

sub decamelise : Memoize {
    my $term = shift;
    my @chars = split(//, $term);
    my $new_str = '';
    my $last_char_was_lowercase = 0;
    for (@chars) {
        unless (/[[:alpha:]]/) {
            $new_str .= $_;
            next;
        }
        my $this_is_upper = (/[[:upper:]]/) ? 1 : 0;
        $new_str .= ' ' if ($last_char_was_lowercase && $this_is_upper);
        $new_str .= $_;
        $last_char_was_lowercase = ! $this_is_upper;
    }
    return $new_str;
}

sub pluraliser : Memoize {
    my $term            = shift;
    return '' unless $term;
    my $lc_term         = lc($term);
    my $lc_plural       = PL_N($lc_term);
    my @term_chars      = split( //, $lc_term );
    my @lc_plural_chars = split( //, $lc_plural );
    my $last_same;
    for my $i ( 0 .. $#term_chars ) {
        if ( $term_chars[$i] eq $lc_plural_chars[$i] ) {
            $last_same = $i + 1;
        }
        else {
            last;
        }
    }

    return substr( $term, 0, $last_same ) . substr( $lc_plural, $last_same );
}

sub get_lists {
    my $list_names      = setting('gene_list_names');
    my %lists =
      map { $_->name => $_ }
      grep { defined } map { service()->list($_) } @$list_names;
    my $tag = setting('list_tag');
    $lists{ $_->name } = $_ for ( grep { $_->has_tag($tag) } service()->lists );
    my @lists = sort { $a->name cmp $b->name } values %lists;
    return @lists;
}

get '/lists' => sub {

    my @lists = get_lists();

    return template 'no_lists' unless @lists;

    my $query = get_list_query( $lists[0] );

    template lists => {
        use_data_tables => 1,
        class_keys => get_class_keys_for( $lists[0]->type ),
        items      => [],
        lists      => [@lists],
        list_query => $query,
        tsv_uri    => proxy->uri_for( '/list/' . $lists[0]->name . '.tsv' ),
        json_uri   => proxy->uri_for( '/list/' . $lists[0]->name . '.json' ),
        xml_uri    => proxy->uri_for( '/list/' . $lists[0]->name . '.xml' ),
    };
};

get '/lists.options' => sub {
    my @lists = get_lists();
    template list_options => {
        lists => [@lists],
    }, {layout => undef};
};

any '/lists.export' => sub {
    my @lists = (params->{list}) ? ( 
        service->list(params->{list}) || get_lists()) : get_lists();
    template export => {lists => [@lists]}, {layout => undef};
};

my $list_item_controller = sub {
    my @lists = (params->{list}) ? ( 
        service->list(params->{list}) || get_lists()) : get_lists();
    my $list_query = get_list_query( $lists[0] );
    my $items = $list_query->results(as => 'jsonobjects');
    template list_items => {
        class_keys => get_class_keys_for( $lists[0]->type ),
        items      => $items,
        lists      => [@lists],
    }, {layout => undef};
};
get '/lists.items' => $list_item_controller;
post '/lists.items' => $list_item_controller;

sub get_items_in_list {
    my $list  = shift;
    my $query = get_list_query($list);
    my $items = $query->results(as => 'jsonobjects');
    return $items;
}

sub get_list_query {
    my $list       = shift;
    my $main_field = get_class_keys_for( $list->type )->[0];
    my $query      = $list->build_query;
    if (service->model
                ->get_classdescriptor_by_name($list->type)
                ->get_field_by_name($main_field)) {
        $query->set_sort_order( $list->type . '.' . $main_field => 'asc' );
    }
    add_extra_views_to_query( $list->type, $query );
    return $query;
}

sub get_class_keys_for : Memoize {
    my $class      = shift;
    my $class_keys = setting('class_keys');
    if ( my $keys = $class_keys->{$class} ) {
        return $keys;
    }
    else {
        return $class_keys->{Default};
    }
}

get '/list/:list.gff3' => sub {
    content_type 'text/plain';
    header 'Content-Disposition' => 'attachment: filename='
      . params->{list} . '.gff3';
    return get_list_gff3( params->{list} );
};

sub get_list_gff3 : Memoize {
    my $list_name = shift;
    my $list = service->list($list_name) or die "Cannot find list $list_name";
    my $query = service->new_query( class => $list->type, with => GFF3 );
    if ($list->type eq 'Gene') {
        $query->add_sequence_features(qw/Gene Gene.exons Gene.transcripts/);
        $query->add_outer_join('Gene.exons');
        $query->add_outer_join('Gene.transcripts');
    } else {
        $query->add_constraint(qw/locatedFeatures.feature SequenceFeature/);
        $query->add_view('locatedFeatures.feature.primaryIdentifier');
    }

    $query->add_constraint( $list->type, 'IN', $list );
    return $query->get_gff3;
}

get '/list/:list.fasta' => sub {
    my $list = service->list( params->{list} );
    my $query = service->new_query( class => 'Gene', with => FASTA );
    $query->add_constraint( 'Gene', 'IN', $list );
    content_type 'text/plain';
    header 'Content-Disposition' => 'attachment: filename='
      . $list->name . '.fa';
    return $query->get_fasta;
};

get '/list/:list.tsv' => sub {
    my $list  = service->list( params->{list} );
    my $query = get_list_query($list);
    content_type 'text/plain';
    header 'Content-Disposition' => 'attachment: filename='
      . $list->name . '.tsv';
    my $result =
      join( "\n", $query->results( as => 'tsv', columnheaders => 1 ) );
    return $result;
};

get '/list/:list.json' => sub {
    my $list  = service->list( params->{list} );
    my $query = get_list_query($list);
    content_type 'application/json';
    header 'Content-Disposition' => 'attachment: filename='
      . $list->name . '.json';
    my $result = "["
      . join( "\n", $query->results( as => 'jsonobjects', json => 'raw' ) )
      . "]";
    return $result;
};

get '/list/:list.xml' => sub {
    my $list  = service->list( params->{list} );
    my $query = get_list_query($list);
    content_type 'text/xml';
    header 'Content-Disposition' => 'attachment: filename='
      . $list->name . '.xml';
    my $result = join( "\n", $query->results( as => 'xml' ) );
    return $result;
};

# An ugly hack to fix json round-tripping
sub JSON::Boolean::TO_JSON {
    my $self = shift;
    if ($$self) {
        return 'true';
    } else {
        return 'false';
    }
}

get '/list/:list.table' => sub {
    my $list = service->list(params->{list})
        or return to_json({problem => "This list is not available"});
    my $query = get_list_query($list);
    my $rows = $query->results( as => 'jsonrows' );
    no warnings;
    my $table_data = [
        map {
            [map {
                my $value = (blessed($_->{value}) and $_->{value}->can('TO_JSON')) 
                    ? $_->{value}->TO_JSON
                    : (defined($_->{value})) ? $_->{value} : '[NULL]';
                sprintf("<a href=\"%s\">%s</a>", proxy->uri_for('/' . $_->{class} . '/id/' . $_->{id}), $value)
            } @$_]
        } @$rows
    ];
    my @views = map {{sTitle => $_}} map { 
        my @parts = split(/\./);
        (@parts == 2 or $parts[-2] eq $parts[-1])
            ? ucfirst(decamelise($parts[-1]))
            : ($parts[-1] eq setting('class_keys')->{Default}[0])
                ? ucfirst(decamelise($parts[-2]))
                : ucfirst(decamelise(join(' ', @parts[-2, -1])));
    } $query->views;
    return to_json({aoColumns => [@views], aaData => $table_data});
};

any '/list/:list.items' => sub {

    my @lists = ( service->list( params->{list} ) );

    return send_error( "No gene lists found", 500 ) unless @lists;

    my $query     = get_list_query( $lists[0] );
    my $start     = params->{start} || 0;
    my $items     = $query->results(as => 'jsonobjects', start => $start, size => 100);
    my $list_name = $lists[0]->name;

    template list_items => {
        class_keys => get_class_keys_for( $lists[0]->type ),
        items      => $items,
        lists      => [@lists],
        start      => $start,
        page_size  => 100,
    }, {layout => undef};

};


get '/list/:list' => sub {

    my @lists = ( service->list( params->{list} ) );

    return send_error( "No gene lists found", 500 ) unless @lists;

    my $query     = get_list_query( $lists[0] );
    my $start     = params->{start} || 0;
    my $items     = $query->results(as => 'jsonobjects', start => $start, size => 100);
    my $list_name = $lists[0]->name;
    

    template lists => {
        class_keys => get_class_keys_for( $lists[0]->type ),
        items      => $items,
        lists      => [@lists],
        list_query => $query,
        start      => $start,
        page_size  => 100,
        tsv_uri    => proxy->uri_for( '/list/' . $list_name . '.tsv' ),
        json_uri   => proxy->uri_for( '/list/' . $list_name . '.json' ),
        xml_uri    => proxy->uri_for( '/list/' . $list_name . '.xml' ),
        gff_uri    => proxy->uri_for( '/list/' . $list_name . '.gff3' ),
        fasta_uri  => proxy->uri_for( '/list/' . $list_name . '.fasta' ),
    };

};

sub get_gff_url : Memoize {
    my $identifier = shift;
    my $gff_query = service->new_query( class => 'Gene', with => GFF3 );
    $gff_query->add_sequence_features(qw/Gene Gene.exons Gene.transcripts/);
    $gff_query->add_outer_join('Gene.exons');
    $gff_query->add_outer_join('Gene.transcripts');
    $gff_query->add_constraint( 'id', '=', $identifier );
    return $gff_query->get_gff3_uri;
}

sub get_fasta_url : Memoize {
    my $identifier = shift;
    my $fasta_query = service->new_query( class => 'Gene', with => FASTA );
    $fasta_query->add_constraint( 'id', '=', $identifier );
    return $fasta_query->get_fasta_uri;
}

sub get_summary_fields : Memoize {
    my $type           = shift;
    my $summary_fields = setting('additional_summary_fields');
    my $cd             = service->model->get_classdescriptor_by_name($type);
    my @returners;
    for my $key ( keys %$summary_fields ) {
        if ( $cd->sub_class_of($key) ) {
            push @returners, @{ $summary_fields->{$key} };
        }
    }
    return [@returners];
}

sub add_extra_views_to_query {
    my ( $type, $query, $not_outer_joins ) = ( @_, [] );
    my %dont_outer_join = map { $_ => 1 } @$not_outer_joins;
    my $extra_views = get_summary_fields($type);
    if (@$extra_views) {
        $query->add_views(@$extra_views);
        for (@$extra_views) {
            next if $dont_outer_join{$_};
            my @parts     = split(/\./);
            my $join_path = shift @parts;
            do {
                $query->add_outer_join($join_path);
                $join_path .= '.' . shift @parts;
            } while (@parts);
        }
    }
}

sub get_item_query : Memoize {
    my $type       = ucfirst(shift);
    my $identifier = shift;
    my @ids        = split( /;/, $identifier );

    my $query      = service->new_query( class => $type );
    $query->add_views('*');
    if ( @ids == 1 ) {
        add_extra_views_to_query( $type, $query );
        $query->add_constraint( $type, 'LOOKUP', $ids[0] );
    }
    else {
        my $class_keys = get_class_keys_for($type);
        add_extra_views_to_query( $type, $query, $class_keys );
        for ( my $i = 0 ; $i < @$class_keys ; $i++ ) {
            my $path  = $class_keys->[$i];
            my $value = $ids[$i];
            debug("Adding constraint: $path = $value");
            $query->add_constraint( $path, '=', $value );
        }
    }
    return $query;
}

sub get_item_details : Memoize {
    my $query = get_item_query(@_);
    my ($item) = $query->results(RESULT_OPTIONS);
    return $item;
}

sub get_homologues : Memoize {
    my $identifier = shift;
    my $homologue_query = service->new_query( class => 'Gene' );
    $homologue_query->add_views(qw/* organism.name/);
    $homologue_query->add_constraint( 'organism.name', '!=', 'Homo sapiens' );
    $homologue_query->add_constraint( 'homologues.homologue', 'LOOKUP',
        $identifier );
    my $homologues = $homologue_query->results(RESULT_OPTIONS);
    return $homologues;
}

get qr{/gene/id/(\w+)}i => sub {
    my ($id) = splat;
    return template item_error => { params } unless ($id =~ /^\d+$/);
    my $query = service->new_query(class => 'Gene');
    $query->add_views('*');
    add_extra_views_to_query(Gene => $query);
    $query->add_constraint( 'id', '=', $id );
    return do_gene_report($query);
};

get qr{/gene/(\w+)}i => sub {
    my ($id) = splat;
    my $query = get_item_query( Gene => $id );
    return do_gene_report($query);
};

sub do_gene_report {
    my $query = shift;

    debug("Getting gene item");
    my ($item) = $query->results( as => 'hashrefs' )
      or return template item_error => { query => $query, params };
    debug("Getting gene obj");
    my ($obj) = $query->results(RESULT_OPTIONS)
      or return template item_error => { query => $query, params };

    my $display_name = $obj->{symbol} || $obj->{primaryIdentifier};

    my $type       = "Gene";
    my $keys       = get_class_keys_for($type);

    my $identifier = get_identifier($type, $item, $keys);
    my @comments = get_user_comments($identifier);

    my $cd = service->model->get_classdescriptor_by_name( $obj->{class} );
    debug("Getting lists");
    my @all_lists = get_lists();
    my @lists = grep { $cd->sub_class_of( $_->type ) } @all_lists;
    debug("Getting contained in");
    my %contained_in = eval {
      map { $_->name, $_ } 
      service->lists_with_object( $obj->{objectId} )
    };

    # Generate Links for Sequence exports
    debug("Getting export uris");
    my $gff_uri   = get_gff_url($obj->{objectId});
    my $fasta_uri = get_fasta_url($obj->{objectId});

    # Get homologues in rat and mouse
    debug("Getting homologues");
    my $homologues = get_homologues($identifier);
    my @table_keys = grep {$_ !~ /chromosomeLocation/} $query->views;

    return template gene => {
        item        => $item,
        obj        => $obj,
        displayname => $display_name,
        table_keys => [@table_keys],
        comments    => [@comments],
        tsv_uri => get_export_url($query, 'tab'),
        json_uri => get_export_url($query, 'jsonrows'),
        xml_uri => get_export_url($query, 'xml'),
        gff3_uri     => $gff_uri,
        fasta_uri   => $fasta_uri,
        homologues  => $homologues,
        lists        => [@lists],
        all_lists    => [@all_lists],
        contained_in => {%contained_in},
        identifier   => $identifier,
        templates    => get_templates('Gene'),
    };
};

sub get_export_url {
    my ($query, $format) = @_;
    my $uri = URI->new($query->url);
    $uri->query_form($query->get_request_parameters, format => $format);
    return "$uri";
}

get '/:type/id/:id' => sub {
    my $type = ucfirst( params->{'type'} );
    return template item_error => {params} unless (params->{id} =~ /^\d+$/);
    my $query = service->new_query( class => $type );
    $query->add_views('*');
    add_extra_views_to_query( $type, $query );
    $query->add_constraint( 'id', '=', params->{id} );
    return do_item_report($query);
};

get '/:type/:id' => sub {
    my $type = ucfirst( params->{'type'} );
    my $query = get_item_query( $type, params->{'id'} );
    return do_item_report($query);
};

sub do_item_report {
    my $query = shift;
    debug("Getting " . params->{type} . " item");
    my ($item) = $query->results( as => 'hashrefs' )
      or return template item_error => { query => $query, params };
    debug("Getting " . params->{type} . " obj");
    my ($obj) = $query->results(RESULT_OPTIONS)
      or return template item_error => { query => $query, params };

    my $cd = service->model->get_classdescriptor_by_name( $obj->{class} );
    my @all_lists = get_lists();
    my @lists = grep { $cd->sub_class_of( $_->type ) } @all_lists;
    my %contained_in = eval {
      map { $_->name, $_ } 
      service->lists_with_object( $obj->{objectId} )
    };

    my $type       = ucfirst( params->{'type'} );
    my $keys       = get_class_keys_for($type);

    my $identifier = get_identifier($type, $item, $keys);

    my $displayname;
    for my $k (@$keys) {
        $displayname = $item->{"$type.$k"};
        last if $displayname;
    }

    my @comments = get_user_comments($identifier);

    debug( "Rendering report for " . to_dumper($item) );

    return template item => {
        item         => $item,
        lists        => [@lists],
        all_lists    => [@all_lists],
        contained_in => {%contained_in},
        templates    => get_templates($type),
        obj          => $obj,
        identifier   => $identifier,
        displayname  => $displayname,
        comments     => [@comments],
    };
}

sub get_identifier {
    my ($type, $item, $keys) = @_;
    my $identifier = join( ';',
        map { defined( $item->{"$type.$_"} ) ? $type . $_ . '=' . $item->{"$type.$_"} : '' }
          @$keys );
    return $identifier;
}

sub get_templates : Memoize {
    my $type = shift;
    opendir( my $dir, 'views' );
    my $cd = service->model->get_classdescriptor_by_name( ucfirst($type) );
    my @templates;
    for ( readdir($dir) ) {
        next unless /_templates\.tt/;
        my ($file_type) = map { ucfirst } split(/_/);
        if ( $cd->sub_class_of($file_type) ) {
            push @templates, $_;
        }
    }
    return [@templates];
}

sub get_user_comments {

    # TODO - change DB schema so it refers to items
    my $identifier = shift;
    my $gene_rs = schema('usercomments')->resultset('Item')
                                        ->find_or_create( { identifer => $identifier } );

    debug("Looking for " . $identifier);
    debug("Found " . $gene_rs->comments->count);
    my @comments = $gene_rs->comments->get_column('value')->all;
    return @comments;
}

post '/addcomment' => sub {
    my $id      = params->{id};
    my $comment = params->{comment};
    my $gene_rs = schema('usercomments')->resultset('Item')
      ->find_or_create( { identifer => $id } );
    $gene_rs->add_to_comments( { value => $comment } );
    return to_json( { id => $id, comment => $comment } );
};

post '/removecomment' => sub {
    my $id      = params->{id};
    my $comment = params->{commenttext};
    my $gene_rs =
      schema('usercomments')->resultset('Item')
      ->find_or_create( { identifer => $id } );
    $gene_rs->delete_related( 'comments', { value => $comment } );
    return to_json( { id => $id, comment => $comment } );
};

post '/add_ids_to_list' => sub {
    my @ids       = split( ',', params->{ids} );
    my $list_name = params->{list};
    my $list      = service->list($list_name)
      or return to_json(
        { problem => "$list_name does not exist at this service" } );
    my $prior_count = $list->size;
    my $query = service->new_query( class => $list->type );
    $query->add_constraint( 'id', 'ONE OF', [@ids] );
    $list += $query;
    my $new_item_count = $list->size - $prior_count;
    return to_json(
        { info => "Added $new_item_count new items to $list_name"} );
};

post '/create_new_list' => sub {
    my @ids       = split( ',', params->{"ids"} );
    my $list_name = params->{"list"};
    my $type      = params->{"type"};
    my $desc      = params->{"description"};
    my $query     = service->new_query( class => $type );
    $query->add_constraint( 'id', 'ONE OF', [@ids] );
    my $list = service->new_list(
        content     => $query,
        name        => $list_name,
        description => $desc,
        tags        => [ setting('list_tag') ],
    );
    return to_json( { info => "New list created: " . $list->name } );
};

post '/remove_list_item' => sub {
    my @ids       = split( ',', params->{"ids"} );
    my $list_name = params->{"list"};
    my $list = service->list($list_name)
        or return to_json({problem => "$list_name is not available"});
    my $query = service->new_query( class => $list->type );
    $query->add_constraint( 'id', 'ONE OF', [@ids] );
    my $prior_count = $list->size;
    $list -= $query;
    my $removed_count = $prior_count - $list->size;
    service->delete_temp_lists();
    return to_json({info => "Removed $removed_count " .
            pluraliser($list->type) . " from $list_name"});
};

post '/deletelist' => sub {
    my $list_name = params->{"list"};
    my $list = service->list($list_name)
        or return to_json({problem => "$list_name is not available"});
    $list->delete();
    return to_json({info => "Deleted " . $list->name});
};

post '/deletelists' => sub {
    my @list_names = split(/;/, params->{"lists"});
    my @lists;
    for my $list_name (@list_names) {
        my $list = service->list($list_name)
            or return to_json({problem => "$list_name is not available"});
        push @lists, $list;
    }
    map {$_->delete()} @lists;
    return to_json({info => "Deleted " . scalar(@lists) . " lists"});
};

my %method_name_for_option = (
    merge => 'join_lists',
    intersect => 'intersect_lists', 
    diff => 'diff_lists',
    subtract => 'subtract_lists',
);

post '/performlistop' => sub {
    my @list_names = split(/;/, params->{"lists"});
    my $name = params->{newname};
    my $desc = params->{newdesc};
    my $op = params->{op};
    my $method = $method_name_for_option{$op} 
        or return to_json({problem => "$op is not a list operation"});
    my @lists;
    for my $list_name (@list_names) {
        my $list = service->list($list_name)
            or return to_json({problem => "$list_name is not available"});
        push @lists, $list;
    }
    my @rhs;
    if (my $rhs_names = params->{rhs}) {
        for my $name (split(/;/, $rhs_names)) {
            my $list = service->list($name)
                or return to_json({problem => "$name is not available"});
            push @rhs, $list;
        }
    }

    my @args = (params->{rhs}) ? ([@lists], [@rhs]) : ([@lists]);
    my $try = eval {service->$method(@args)};
    if (my $e = $@) {
        return to_json({problem => $e});
    }
    my $new_list = service->new_list(
        content => $try,
        name => $name, 
        description => $desc, 
        tags => [ setting('list_tag') ],
    );
    service->delete_temp_lists();
    return to_json({info => "Created " . $new_list->name});
};

true;
