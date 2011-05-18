package MineViewer;
use Dancer ':syntax';
use Dancer::Plugin::DBIC;
use Dancer::Plugin::ProxyPath;

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
my $service_args = setting('service_args');
my $service      = Webservice::InterMine->get_service(@$service_args);

my $last_update     = 0;
my $update_interval = setting('update_interval');
my $list_names      = setting('gene_list_names');

use constant RESULT_OPTIONS => ( as => 'jsonobjects' );

before sub {
    if ( $last_update < ( time - $update_interval ) ) {
        $service->refresh_lists;
        $last_update = time;
    }
};

before_template sub {
    my $tokens = shift;
    $tokens->{pluraliser} = \&pluraliser;
};

get '/' => sub {

    my @lists = map { $service->list($_) } @$list_names;

    return template index => { lists => [@lists] };
};

get '/templates' => sub {
    return template 'templates' => { all_lists => [ get_lists() ], };
};

sub pluraliser : Memoize {
    my $term            = shift;
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
    my %lists =
      map { $_->name => $_ }
      grep { defined } map { $service->list($_) } @$list_names;
    my $tag = setting('list_tag');
    $lists{ $_->name } = $_ for ( grep { $_->has_tag($tag) } $service->lists );
    my @lists = sort { $a->name cmp $b->name } values %lists;
    return @lists;
}

get '/lists' => sub {

    my @lists = get_lists();

    return send_error( "No gene lists found", 500 ) unless @lists;

    my $items = get_items_in_list( $lists[0] );

    template lists => {
        class_keys => get_class_keys_for( $lists[0]->type ),
        items      => $items,
        lists      => [@lists],
        tsv_uri    => proxy->uri_for( '/list/' . $lists[0]->name . '.tsv' ),
        json_uri   => proxy->uri_for( '/list/' . $lists[0]->name . '.json' ),
        xml_uri    => proxy->uri_for( '/list/' . $lists[0]->name . '.xml' ),
    };
};

sub get_items_in_list {
    my $list  = shift;
    my $query = get_list_query($list);
    my $items = $query->results( as => 'jsonobjects' );
    return $items;
}

sub get_list_query {
    my $list       = shift;
    my $main_field = get_class_keys_for( $list->type )->[0];
    my $query      = $list->build_query;
    $query->set_sort_order( $list->type . '.' . $main_field => 'asc' );
    add_extra_views_to_query( $list->type, $query );
    return $query;
}

sub get_class_keys_for : Memoize {
    my $class      = shift;
    my $class_keys = setting('class_keys');
    if ( my $keys = $class_keys->{$class} ) {
        debug( "Returning " . to_dumper($keys) );
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
    my $list = $service->list($list_name) or die "Cannot find list $list_name";
    my $query = $service->new_query( class => 'Gene', with => GFF3 );
    $query->add_sequence_features(qw/Gene Gene.exons Gene.transcripts/);
    $query->add_constraint( 'Gene', 'IN', $list );
    return $query->get_gff3;
}

get '/list/:list.fasta' => sub {
    my $list = $service->list( params->{list} );
    my $query = $service->new_query( class => 'Gene', with => FASTA );
    $query->add_constraint( 'Gene', 'IN', $list );
    content_type 'text/plain';
    header 'Content-Disposition' => 'attachment: filename='
      . $list->name . '.fa';
    return $query->get_fasta;
};

get '/list/:list.tsv' => sub {
    my $list  = $service->list( params->{list} );
    my $query = get_list_query($list);
    content_type 'text/plain';
    header 'Content-Disposition' => 'attachment: filename='
      . $list->name . '.tsv';
    my $result =
      join( "\n", $query->results( as => 'tsv', columnheaders => 1 ) );
    return $result;
};

get '/list/:list.json' => sub {
    my $list  = $service->list( params->{list} );
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
    my $list  = $service->list( params->{list} );
    my $query = get_list_query($list);
    content_type 'text/xml';
    header 'Content-Disposition' => 'attachment: filename='
      . $list->name . '.xml';
    my $result = join( "\n", $query->results( as => 'xml' ) );
    return $result;
};

get '/list/:list' => sub {

    my @lists = ( $service->list( params->{list} ) );

    return send_error( "No gene lists found", 500 ) unless @lists;

    my $items     = get_items_in_list( $lists[0] );
    my $list_name = $lists[0]->name;

    template lists => {
        class_keys => get_class_keys_for( $lists[0]->type ),
        items      => $items,
        lists      => [@lists],
        tsv_uri    => proxy->uri_for( '/list/' . $list_name . '.tsv' ),
        json_uri   => proxy->uri_for( '/list/' . $list_name . '.json' ),
        xml_uri    => proxy->uri_for( '/list/' . $list_name . '.xml' ),
        gff_uri    => proxy->uri_for( '/list/' . $list_name . '.gff3' ),
        fasta_uri  => proxy->uri_for( '/list/' . $list_name . '.fasta' ),
    };

};

sub get_gff_url : Memoize {
    my $identifier = shift;
    my $gff_query = $service->new_query( class => 'Gene', with => GFF3 );
    $gff_query->add_sequence_features(qw/Gene Gene.exons Gene.transcripts/);
    $gff_query->add_constraint( 'Gene', 'LOOKUP', $identifier );
    return $gff_query->get_gff3_uri;
}

sub get_fasta_url : Memoize {
    my $identifier = shift;
    my $fasta_query = $service->new_query( class => 'Gene', with => FASTA );
    $fasta_query->add_constraint( 'Gene', 'LOOKUP', $identifier );
    return $fasta_query->get_fasta_uri;
}

sub get_summary_fields : Memoize {
    my $type           = shift;
    my $summary_fields = setting('additional_summary_fields');
    my $cd             = $service->model->get_classdescriptor_by_name($type);
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
    my $query      = $service->new_query( class => $type );
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
    my $homologue_query = $service->new_query( class => 'Gene' );
    $homologue_query->add_views(qw/primaryIdentifier symbol organism.name/);
    $homologue_query->add_constraint( 'organism.name', '!=', 'Homo sapiens' );
    $homologue_query->add_constraint( 'homologues.homologue', 'LOOKUP',
        $identifier );
    my $homologues = $homologue_query->results(RESULT_OPTIONS);
    return $homologues;
}

get '/gene/:id' => sub {
    my $gene = get_item_details( gene => params->{id} )
      or return template gene_error => params;

    my $display_name = $gene->{symbol}            || $gene->{primaryIdentifier};
    my $identifier   = $gene->{primaryIdentifier} || $gene->{symbol};
    my @comments = get_user_comments($identifier);

    # Generate Links for Sequence export
    my $gff_uri   = get_gff_url($identifier);
    my $fasta_uri = get_fasta_url($identifier);

    # Get homologues in rat and mouse
    my $homologues = get_homologues($identifier);

    return template gene => {
        gene        => $gene,
        displayname => $display_name,
        comments    => [@comments],
        gff_uri     => $gff_uri,
        fasta_uri   => $fasta_uri,
        homologues  => $homologues,
    };
};

get '/:type/id/:id' => sub {
    my $type = ucfirst( params->{'type'} );
    my $query = $service->new_query( class => $type );
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
    my ($item) = $query->results( as => 'hashrefs' )
      or return template item_error => { query => $query, params };
    my ($obj) = $query->results(RESULT_OPTIONS)
      or return template item_error => { query => $query, params };

    my $cd = $service->model->get_classdescriptor_by_name( $obj->{class} );
    my @all_lists = get_lists();
    my @lists = grep { $cd->sub_class_of( $_->type ) } @all_lists;
    my %contained_in =
      map { $_->name, $_ } $service->lists_with_object( $obj->{objectId} );

    my $type       = ucfirst( params->{'type'} );
    my $keys       = get_class_keys_for($type);
    my $identifier = join( ';',
        map { defined( $item->{"$type.$_"} ) ? $item->{"$type.$_"} : '' }
          @$keys );

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

sub get_templates : Memoize {
    my $type = shift;
    opendir( my $dir, 'views' );
    my $cd = $service->model->get_classdescriptor_by_name( ucfirst($type) );
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
    my $gene_rs =
      schema('usercomments')->resultset('Gene')
      ->find_or_create( { identifer => $identifier } );
    my @comments = $gene_rs->comments->get_column('value')->all;
    return @comments;
}

post '/addcomment' => sub {
    my $id      = params->{id};
    my $comment = params->{comment};
    my $gene_rs =
      schema('usercomments')->resultset('Gene')
      ->find_or_create( { identifer => $id } );
    $gene_rs->add_to_comments( { value => $comment } );
    return to_json( { id => $id, comment => $comment } );
};

post '/removecomment' => sub {
    my $id      = params->{geneid};
    my $comment = params->{commenttext};
    my $gene_rs =
      schema('usercomments')->resultset('Gene')
      ->find_or_create( { identifer => $id } );
    $gene_rs->delete_related( 'comments', { value => $comment } );
    return to_json( { id => $id, comment => $comment } );
};

post '/add_ids_to_list' => sub {
    my @ids       = split( ',', params->{ids} );
    my $list_name = params->{list};
    my $list      = $service->list($list_name)
      or return to_json(
        { problem => "$list_name does not exist at this service" } );
    my $query = $service->new_query( class => $list->type );
    $query->add_constraint( 'id', 'ONE OF', [@ids] );
    $list += $query;
    return to_json(
        { info => "Added " . scalar(@ids) . " items to $list_name" } );
};

post '/create_new_list' => sub {
    my @ids       = split( ',', params->{"ids"} );
    my $list_name = params->{"list"};
    my $type      = params->{"type"};
    my $desc      = params->{"description"};
    my $query     = $service->new_query( class => $type );
    $query->add_constraint( 'id', 'ONE OF', [@ids] );
    my $list = $service->new_list(
        content     => $query,
        name        => $list_name,
        description => $desc,
        tags        => [ setting('list_tag') ],
    );
    return to_json( { info => "New list created: " . $list->name } );
};

true;
