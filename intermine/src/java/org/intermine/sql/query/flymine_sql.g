// Flymine SQL grammar

header {
package org.flymine.sql.query;
}

class SqlTreeParser extends TreeParser;

options {
    exportVocab = Sql;
    k = 6;
    buildAST = true;
}

tokens {
    SQL_STATEMENT;
    SELECT_LIST;
    FROM_LIST;
    WHERE_CLAUSE;
    GROUP_CLAUSE;
    HAVING_CLAUSE;
    ORDER_CLAUSE;
    LIMIT_CLAUSE;
    SELECT_VALUE;
    TABLE_ALIAS;
    FIELD_ALIAS;
    TABLE;
    TABLE_NAME;
    SUBQUERY;
    CONSTANT;
    FIELD;
    FIELD_NAME;
    SAFE_FUNCTION;
    UNSAFE_FUNCTION;
    CONSTRAINT;
    NOT_CONSTRAINT;
    AND_CONSTRAINT_SET;
    OR_CONSTRAINT_SET;
    SUBQUERY_CONSTRAINT;
}

start_rule: sql_statement ;

sql_statement: #( SQL_STATEMENT 
            ( "explain" )?
            ( "distinct" )?
            select_list
            ( from_list
                ( where_clause )?
                ( group_clause ( having_clause )? )?
                ( order_clause )?
                ( limit_clause )? )? )
    ;

select_list: #( SELECT_LIST ( select_value )+ ) ;

from_list: #( FROM_LIST ( abstract_table )+ ) ;

where_clause: #( WHERE_CLAUSE ( abstract_constraint)+ ) ;

group_clause: #( GROUP_CLAUSE ( abstract_value )+ ) ;

having_clause: #( HAVING_CLAUSE ( abstract_constraint)+ ) ;

order_clause: #( ORDER_CLAUSE ( abstract_value )+ ) ;

limit_clause: #( LIMIT_CLAUSE INTEGER ( INTEGER )? ) ;

select_value: #( SELECT_VALUE abstract_value ( field_alias )? );

abstract_table: table | subquery ;

abstract_value: unsafe_function | safe_function | constant | field ;

field_alias: #( FIELD_ALIAS IDENTIFIER ) ;

table: #( TABLE table_name ( table_alias )? ) ;

subquery: #( SUBQUERY sql_statement table_alias ) ;

table_name: #( TABLE_NAME IDENTIFIER ) ;

table_alias: #( TABLE_ALIAS IDENTIFIER ) ;

constant: #( CONSTANT ( QUOTED_STRING | INTEGER ) ) ;

field: #( FIELD table_alias field_name );

safe_function: #( SAFE_FUNCTION (
                "count"
                | "max" abstract_value
                | "min" abstract_value
                | "sum" abstract_value
                | "avg" abstract_value ) ) ;

unsafe_function: #( UNSAFE_FUNCTION abstract_value
            ( ( PLUS | PERCENT | ASTERISK | DIVIDE | POWER | MINUS ) abstract_value )+ ) ;

field_name: #( FIELD_NAME IDENTIFIER );

abstract_constraint: constraint | not_constraint | and_constraint_set | or_constraint_set
        | subquery_constraint ;

constraint: #( CONSTRAINT abstract_value comparison_op abstract_value ) ;

not_constraint: #( NOT_CONSTRAINT #( NOT_CONSTRAINT a:abstract_constraint ) )
            { #not_constraint = #a }
        | #( NOT_CONSTRAINT abstract_constraint ) ;

or_constraint_set: #( OR_CONSTRAINT_SET ( abstract_constraint )+ ) ;

and_constraint_set: #( AND_CONSTRAINT_SET (abstract_constraint )+ ) ;

subquery_constraint: #( SUBQUERY_CONSTRAINT abstract_value sql_statement ) ;

comparison_op: EQ | LT | GT | NOT_EQ | LE | GE | "like";



class SqlParser extends Parser;

options {
    exportVocab = Sql;
    k = 6;
    buildAST = true;
}

start_rule: sql_statement (SEMI!)? EOF;

sql_statement: select_command
        { #sql_statement = #([SQL_STATEMENT, "SQL_STATEMENT"], #sql_statement); }
    ;

select_command:
        ( "explain" )? "select"! ( "all"! | "distinct" )? select_list
        ( from_list
            ( where_clause )?
            ( group_clause ( having_clause )? )?
            ( order_clause )?
            ( limit_clause )? )?
    ;

select_list:
        select_value ( COMMA! select_value )*
        { #select_list = #([SELECT_LIST, "SELECT_LIST"], #select_list); }
    ;

from_list:
        "from"! abstract_table ( COMMA! abstract_table )*
        { #from_list = #([FROM_LIST, "FROM_LIST"], #from_list); }
    ;

where_clause:
        "where"! abstract_constraint
        { #where_clause = #([WHERE_CLAUSE, "WHERE_CLAUSE"], #where_clause); }
    ;

group_clause:
        "group"! "by"! abstract_value ( COMMA! abstract_value )*
        { #group_clause = #([GROUP_CLAUSE, "GROUP_CLAUSE"], #group_clause); }
    ;

having_clause:
        "having"! abstract_constraint
        { #having_clause = #([HAVING_CLAUSE, "HAVING_CLAUSE"], #having_clause); }
    ;

order_clause:
        "order"! "by"! abstract_value ( COMMA! abstract_value )*
        { #order_clause = #([ORDER_CLAUSE, "ORDER_CLAUSE"], #order_clause); }
    ;

limit_clause:
        "limit"! INTEGER ( "offset" INTEGER )?
        { #limit_clause = #([LIMIT_CLAUSE, "LIMIT_CLAUSE"], #limit_clause); }
    ;

select_value:
        ( (unsafe_function)=> unsafe_function "as"! field_alias
            | field ( "as"! field_alias )?
            | constant "as"! field_alias
            | safe_function "as"! field_alias
            | paren_value "as"! field_alias
        )
        { #select_value = #([SELECT_VALUE, "SELECT_VALUE"], #select_value); }
    ;

abstract_table:
        table | subquery
    ;

abstract_value:
        (unsafe_function)=> unsafe_function | constant | field | safe_function | paren_value
    ;

safe_abstract_value:
        constant | field | safe_function | paren_value
    ;

paren_value: OPEN_PAREN! abstract_value CLOSE_PAREN! ;

field_alias:
        IDENTIFIER
        { #field_alias = #([FIELD_ALIAS, "FIELD_ALIAS"], #field_alias); }
    ;

table_alias:
        IDENTIFIER
        { #table_alias = #([TABLE_ALIAS, "TABLE_ALIAS"], #table_alias); }
    ;

table:
        table_name ( ( "as"! )? table_alias )?
        { #table = #([TABLE, "TABLE"], #table); }
    ;

table_name:
        IDENTIFIER
        { #table_name = #([TABLE_NAME, "TABLE_NAME"], #table_name); }
    ;

subquery:
        OPEN_PAREN! sql_statement CLOSE_PAREN! ( "as"! )? table_alias
        { #subquery = #([SUBQUERY, "SUBQUERY"], #subquery); }
    ;

constant:
//TODO: properly
        ( QUOTED_STRING | INTEGER )
        { #constant = #([CONSTANT, "CONSTANT"], #constant); }
    ;

field:
        table_alias DOT! field_name
        { #field = #([FIELD, "FIELD"], #field); }
    ;

safe_function:
        (
            "count" OPEN_PAREN! ASTERISK! CLOSE_PAREN!
            | "max" OPEN_PAREN! abstract_value CLOSE_PAREN!
            | "min" OPEN_PAREN! abstract_value CLOSE_PAREN!
            | "sum" OPEN_PAREN! abstract_value CLOSE_PAREN!
            | "avg" OPEN_PAREN! abstract_value CLOSE_PAREN!
        )
        { #safe_function = #([SAFE_FUNCTION, "SAFE_FUNCTION"], #safe_function); }
    ;

unsafe_function:
        (
            (safe_abstract_value PLUS)=> safe_abstract_value ( PLUS safe_abstract_value )+
            | (safe_abstract_value PERCENT)=> safe_abstract_value PERCENT safe_abstract_value
            | (safe_abstract_value ASTERISK)=> safe_abstract_value ( ASTERISK safe_abstract_value )+
            | (safe_abstract_value DIVIDE)=> safe_abstract_value DIVIDE safe_abstract_value
            | (safe_abstract_value POWER)=> safe_abstract_value POWER safe_abstract_value
            | (safe_abstract_value MINUS)=> safe_abstract_value MINUS safe_abstract_value
        )
        { #unsafe_function = #([UNSAFE_FUNCTION, "UNSAFE_FUNCTION"], #unsafe_function); }
    ;

field_name:
        IDENTIFIER
        { #field_name = #([FIELD_NAME, "FIELD_NAME"], #field_name); }
    ;

abstract_constraint: (constraint_set)=> constraint_set | safe_abstract_constraint ;

safe_abstract_constraint: (paren_constraint)=> paren_constraint
            | (subquery_constraint)=> subquery_constraint
            | constraint
            | not_constraint
    ;

constraint: abstract_value comparison_op abstract_value
        { #constraint = #([CONSTRAINT, "CONSTRAINT"], #constraint); }
    ;

not_constraint: "not"! safe_abstract_constraint
        { #not_constraint = #([NOT_CONSTRAINT, "NOT_CONSTRAINT"], #not_constraint); }
    ;

paren_constraint: OPEN_PAREN! abstract_constraint CLOSE_PAREN! ;

constraint_set: (or_constraint_set)=> or_constraint_set | and_constraint_set;

or_constraint_set: 
        safe_abstract_constraint ("or"! safe_abstract_constraint)+
        { #or_constraint_set = #([OR_CONSTRAINT_SET, "OR_CONSTRAINT_SET"], #or_constraint_set); }
    ;

and_constraint_set:
        safe_abstract_constraint ("and"! safe_abstract_constraint)+
        { #and_constraint_set = #([AND_CONSTRAINT_SET, "AND_CONSTRAINT_SET"], #and_constraint_set); }
    ;

subquery_constraint: abstract_value "in"! OPEN_PAREN! sql_statement CLOSE_PAREN!
        { #subquery_constraint = #([SUBQUERY_CONSTRAINT, "SUBQUERY_CONSTRAINT"],
                #subquery_constraint); }
    ;

comparison_op: EQ | LT | GT | NOT_EQ | LE | GE | "like";





class SqlLexer extends Lexer;

options {
    exportVocab = Sql;
    testLiterals = false;
    k = 2;
    caseSensitive = false;
    caseSensitiveLiterals = false;
    charVocabulary = '\3'..'\177';
}

IDENTIFIER options { testLiterals=true; } :
        'a'..'z' ( 'a'..'z' | '0'..'9' | '_' | '$' | '#' )*
    ;

QUOTED_STRING:
        '\'' ( ~'\'' )* '\''
    ;

SEMI: ';';
DOT: '.';
COMMA: ',';
ASTERISK: '*';
AT_SIGN: '@';
OPEN_PAREN: '(';
CLOSE_PAREN: ')';
PLUS: '+';
MINUS: '-';
DIVIDE: '/';
PERCENT: '%';
VERTBAR: '|';

EQ: '=';
NOT_EQ:
        '<' { _ttype = LT; } ( ( '>' { _ttype = NOT_EQ; } ) | ( '=' { _ttype = LE; } ) )?
        | "!=" | "^="
    ;
GT: '>' ( '=' { _ttype = GE; } )? ;

INTEGER: ( '0'..'9' )+ ;

WS: ( ' ' | '\t' | '\r' '\n' { newline(); } | '\n' { newline(); } | '\r' { newline(); } )
        {$setType(Token.SKIP);} // Ignore this token
    ;


