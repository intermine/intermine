package Webservice::InterMine::Types;

=head1 NAME

Webservice::InterMine::Types - A type library for webservice modules

=head1 SYNOPSIS

  use Webservice::InterMine::Types qw(RowFormat RowParser)

=head1 DESCRIPTION

This module provides type definitions for Moose classes that use 
parameter type constraints within the Webservice::InterMine distribution.

=head1 AUTHOR

Alex Kalderimis C<< <dev@intermine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine::Types

You can also look for information at:

=over 4

=item * Webservice::InterMine

L<http://www.intermine.org>

=item * Documentation

L<http://www.intermine.org/docs/perl-docs>

=item * User guide

L<http://www.intermine.org/wiki/PerlWebServiceAPI>

=back

=head1 COPYRIGHT AND LICENSE

Copyright 2006 - 2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=cut

use DateTime::Format::ISO8601;
use Carp qw(confess);
require overload;

use MooseX::Types -declare => [
    qw(
        Constraint ConstraintList ConstraintFactory
        ConstraintCode UnaryOperator BinaryOperator FakeBinaryOperator LCBinaryOperator
        TernaryOperator MultiOperator RangeOperator LCRangeOps LCAndUnderscoredRangeOps
        LoopOperator ListOperator NotInWithUnderScores
        LCUnaryOperator LCLoopOperator LCListOperator LCTernaryOperator NotQuiteMulti 
        XmlLoopOperators NoSpaceLoopOperator

        LogicOperator LogicGroup LogicOrStr

        SortOrder SortDirection SortOrderList

        PathDescription PathDescriptionList

        Join JoinStyle JoinList

        Uri HTTPCode NetHTTP NetHTTPS

        Service
        ServiceVersion
        ServiceRootUri ServiceRoot NotServiceRoot SlashedPath

        Query QueryType QueryName QueryHandler IllegalQueryName Listable

        Template TemplateFactory TemplateHash

        SavedQuery SavedQueryFactory

        ListFactory List ListName CanTreatAsList
        ListOfLists ListOfListables

        ListOperable ListOfListOperables 

        RowParser
        RowFormat
        JsonFormat
        RequestFormat
        TSVFormat

        File
        NotAllLowerCase

        Date

        UserAgent

        ResultIterator

        SetObject

        True False TruthValue Truthy 
        
        DomNode

        Path
    )
];

use MooseX::Types::Moose qw/
   Defined  Bool Object Str ArrayRef HashRef Undef Maybe Int/;


# UTILITY

subtype NotAllLowerCase, as Str, where { $_ !~ /^[a-z]+$/ };

subtype File, as Str, where { -f $_ }, message {"'$_' should be a file"};

# CONSTRAINTS

my %fake_to_real_ops = (
    'eq' => '=',
    'ne' => '!=',
    'lt' => '<',
    'gt' => '>',
    'ge' => '>=',
    'le' => '<=',
);

enum ConstraintCode, [ 'A' .. 'ZZ' ];

enum UnaryOperator,  [ 'IS NOT NULL', 'IS NULL' ];
enum LCUnaryOperator,  [ 'is not null', 'is null' ];
coerce UnaryOperator, from LCUnaryOperator, via {uc($_)};

enum BinaryOperator, [ '=', '!=', '<', '>', '>=', '<=', 'CONTAINS', 'LIKE', 'NOT LIKE', 'DOES NOT CONTAIN'];
enum FakeBinaryOperator, ['eq', 'ne', 'lt', 'gt', 'ge', 'le', 'EQ', 'NE', 'LT', 'GT', 'GE', 'LE'];
enum LCBinaryOperator, ["contains", "like", "not like", "does not contain"];
coerce BinaryOperator, from LCBinaryOperator, via {uc($_)};
coerce BinaryOperator, from FakeBinaryOperator, via {$fake_to_real_ops{lc($_)}};

enum LoopOperator,   [ 'IS', 'IS NOT',];
enum LCLoopOperator,   [ 'is', 'is not',];
subtype NoSpaceLoopOperator, as Str, where {lc($_) eq 'isnt'};
enum XmlLoopOperators, [ '=', '!=', ];
my %xml_to_readable = (
    '=' => 'IS', 
    '!=', => 'IS NOT',
);
coerce LoopOperator, from LCLoopOperator, via {uc($_)};
coerce LoopOperator, from XmlLoopOperators, via {$xml_to_readable{$_}};
coerce LoopOperator, from NoSpaceLoopOperator, via {'IS NOT'};

enum ListOperator,   [ 'IN', 'NOT IN',];
enum LCListOperator, [ 'in', 'not in', ];
enum NotInWithUnderScores, [ 'not_in', 'NOT_IN' ];
coerce ListOperator, from NotInWithUnderScores, via {'NOT IN'};
coerce ListOperator, from LCListOperator, via {uc($_)};

subtype TernaryOperator, as Str, where {$_ eq 'LOOKUP'};
subtype LCTernaryOperator, as Str, where {$_ eq 'lookup'};
coerce TernaryOperator, from LCTernaryOperator, via {uc($_)};

enum MultiOperator, [ 'ONE OF', 'NONE OF', ];
subtype NotQuiteMulti, as Str, where {/^n?one[ _-]of$/i};
coerce MultiOperator, from NotQuiteMulti, via {s/[_-]/ /g;uc($_)};

my @range_ops = (
    'OVERLAPS', 'DOES NOT OVERLAP',
    'WITHIN', 'OUTSIDE', 
    'CONTAINS', 'DOES NOT CONTAIN'
);
enum RangeOperator, [ @range_ops ];
enum LCRangeOps, [ map lc, @range_ops ];
enum LCAndUnderscoredRangeOps, [ map {s/ /_/g; lc} @range_ops ];
coerce RangeOperator, from LCRangeOps, via { uc };
coerce RangeOperator, from LCAndUnderscoredRangeOps, via {s/_/ /g; uc };

class_type Constraint, { class => 'Webservice::InterMine::Constraint' };
subtype ConstraintList, as ArrayRef [Constraint];
class_type ConstraintFactory,
    { class => 'Webservice::InterMine::ConstraintFactory', };

# LOGIC

enum LogicOperator, [ 'and',    'or', ];
role_type LogicGroup, { role => 'Webservice::InterMine::Role::Logical' };
subtype LogicOrStr, as LogicGroup | Str;

# SORT ORDER

class_type SortOrder, { class => 'Webservice::InterMine::SortOrder' };
subtype SortOrderList, as ArrayRef[SortOrder];
enum SortDirection, [ 'asc', 'desc', ];

coerce SortDirection, from NotAllLowerCase, via { lc($_) };
coerce SortOrder, from Str,
    via { 
        require Webservice::InterMine::SortOrder;
        Webservice::InterMine::SortOrder->new( split /\s/ ) 
};
coerce SortOrderList, 
    from SortOrder,
        via { [$_] },
    from Str,
        via {
            require Webservice::InterMine::SortOrder;
            [Webservice::InterMine::SortOrder->new(split /\s/)]
        };

# JOINS

class_type Join, { class => 'Webservice::InterMine::Join' };
enum JoinStyle, [ 'INNER', 'OUTER', ];
subtype JoinList, as ArrayRef[Join];

# PATH DESCRIPTIONS

class_type PathDescription,
    { class => 'Webservice::InterMine::PathDescription' };
subtype PathDescriptionList, as ArrayRef [PathDescription];

# HTTP COMMUNICATION

class_type Uri, { class => 'URI' };
class_type NetHTTP, { class => 'Net::HTTP', };
class_type NetHTTPS, { class => 'Net::HTTPS', };
subtype HTTPCode, as Str, where { /^\d{3}$/ };

coerce Uri, from Str, via {
    require URI;
    my $prefix = (m!^(?:ht|f)tp!) ? '' : 'http://';
    URI->new( $prefix . $_ );
};

# SERVICE

class_type Service, { class => 'Webservice::InterMine::Service' };

coerce Service, from Str, via {
    require Webservice::InterMine::Service;
    Webservice::InterMine::Service->new( root => $_ );
};

subtype ServiceVersion, as Int, where {$_ > 0}, 
    message {'I could not get the version number for this service - please check the url and make sure the service is available. I expected a number, but got ' . $_};

coerce ServiceVersion, from Str, via {s/\s*//g;$_};

subtype ServiceRootUri, as Uri, where {$_->path =~ m|/service$| && $_->scheme},
    message { "Uri does not look like a service url: got $_" };
subtype ServiceRoot, as Str, where {m|^https?://.*/service$|};
subtype SlashedPath, as Str, where {m|/|};
subtype NotServiceRoot, as Str;

coerce ServiceRootUri, from Uri, via {
    if ($_->path !~ m|/service$|) {
        $_->path($_->path . '/service');
    }
    unless ($_->scheme) {
        $_->scheme("http");
    }
    return $_;
};
coerce ServiceRootUri, from ServiceRoot, via {
    URI->new($_);
};
coerce ServiceRootUri, from SlashedPath, via {
    my $prefix = (m!^(?:ht|f)tp!) ? '' : 'http://';
    my $suffix = (m|/service$|) ? '' : '/service';
    URI->new($prefix . $_ . $suffix);
};
coerce ServiceRootUri, from NotServiceRoot, via {
    require Webservice::InterMine::ServiceResolver;
    return URI->new(Webservice::InterMine::ServiceResolver->new->resolve($_));
};

# QUERIES

subtype QueryName, as Str, where { /^[\w\.,\s-]*$/ }, message {
    ( defined $_ )
        ? "'$_' includes some characters we do not accept"
        : "Name is undefined";
};
subtype IllegalQueryName, as Str, where { /[^\w\.,\s-]/ };
enum QueryType, [ 'template', 'saved-query', ];
class_type QueryHandler, { class => 'Webservice::InterMine::Query::Handler', };
class_type Query,        { class => 'Webservice::InterMine::Query::Core', };
role_type Listable, {role => 'Webservice::InterMine::Role::Listable'};
subtype ListOfListables, as ArrayRef[Listable];
coerce QueryName, from IllegalQueryName, 
    via { 
        s/[^a-zA-Z0-9_,. -]/_/g; 
        return $_; 
    };


# TEMPLATES

class_type Template,    { class => 'Webservice::InterMine::Query::Template', };
class_type TemplateFactory,
    { class => 'Webservice::InterMine::TemplateFactory', };
subtype TemplateHash, as HashRef [Template];
coerce TemplateFactory, from ArrayRef, via {
    require Webservice::InterMine::TemplateFactory;
    Webservice::InterMine::TemplateFactory->new($_);
};

# LISTS

class_type ListFactory, { class => 'Webservice::InterMine::ListFactory', };
class_type List, {class => 'Webservice::InterMine::List'};
subtype ListName, as Str;
duck_type CanTreatAsList, ['to_list_name'];
subtype ListOfLists, as ArrayRef[List];

subtype ListOperable, as List|Listable;
subtype ListOfListOperables, as ArrayRef[ListOperable];

coerce ListFactory, from HashRef, via {
    require Webservice::InterMine::ListFactory;
    Webservice::InterMine::ListFactory->new( $_ );
};

coerce ListName, from Listable, via {
    my $service = $_->service;
    my $list = eval {$service->new_list(content => $_)};
    if (my $e = $@) {
        confess "Cannot coerce this query into a list, because:\n" . $e;
    }
    return $list->name;
};
coerce ListName, from CanTreatAsList, via {$_->to_list_name};
coerce ListName, from List, via {$_->name};

# SAVED QUERIES

subtype SavedQuery,
    as Query,
    where { $_->does('Webservice::InterMine::Query::Roles::Saved') };

class_type SavedQueryFactory,
    { class => 'Webservice::InterMine::SavedQueryFactory', };

coerce SavedQueryFactory, from Str, via {
    require Webservice::InterMine::SavedQueryFactory;
    Webservice::InterMine::SavedQueryFactory->new( string => $_ );
};

# RESULT ITERATION

role_type RowParser, {role => "Webservice::InterMine::Parser"};
enum RowFormat, ['arrayrefs', 'hashrefs', 'xml', 'tab', 'tsv', 'csv', 'jsonobjects', 'jsonrows', 'jsondatatable', 'count', 'json'];
enum JsonFormat, ['perl', 'inflate', 'instantiate'];
enum RequestFormat, ['tab', 'csv', 'count', 'jsonobjects', 'jsonrows', 'xml', 'jsondatatable', 'json'];
subtype TSVFormat, as Str, where {/^tsv$/i};

class_type ResultIterator, {class => 'Webservice::InterMine::ResultIterator'};

coerce RowFormat, from NotAllLowerCase, via { lc($_) };
coerce JsonFormat, from NotAllLowerCase, via { lc($_) };
coerce RequestFormat, from NotAllLowerCase, via { lc($_) };
coerce RequestFormat, from TSVFormat, via { 'tab' };

# DATES

class_type Date, {class => 'DateTime'};
coerce Date, from Str, via {DateTime::Format::ISO8601->parse_datetime($_)};

# LWP

class_type UserAgent, {class => 'LWP::UserAgent'};

class_type SetObject, {class => 'Set::Object'};

coerce SetObject, from ArrayRef, via {require Set::Object; return Set::Object->new(@$_)};

subtype True, as Defined, where {$_ == 1 || $_ eq "1"};
subtype False, as Defined, where {$_ == 0 || $_ eq "0"};
subtype TruthValue, as True | False;
subtype Truthy, as Object, where {overload::Method($_, 'bool')};
coerce TruthValue, from Truthy, via {$_ ? 1 : 0};

class_type DomNode, { class => 'XML::DOM::Node' };

class_type Path, { class => 'Webservice::InterMine::Path' };
coerce Str, from Path, via {$_->to_string};

1;
