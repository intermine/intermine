// Flymine SQL grammar

header {
package org.flymine.sql.query;
}

class SqlParser extends Parser;

options {
    exportVocab = Sql;
    k = 4;
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
}

start_rule: sql_statement (SEMI)? EOF;

sql_statement: select_command
        { #sql_statement = #([SQL_STATEMENT, "sql_statement"], #sql_statement); }
    ;

select_command:
        ( "explain" )? "select" ( "all" | "distinct" )? select_list
        from_list
        ( where_clause )?
        ( group_clause ( having_clause )? )?
        ( order_clause )?
        ( limit_clause )?
    ;

select_list:
        select_value ( COMMA select_value )*
        { #select_list = #([SELECT_LIST, "select_list"], #select_list); }
    ;

from_list:
        "from" abstract_table ( COMMA abstract_table )*
        { #from_list = #([FROM_LIST, "from_list"], #from_list); }
    ;

where_clause:
        "where" condition
        { #where_clause = #([WHERE_CLAUSE, "where_clause"], #where_clause); }
    ;

group_clause:
        "group" "by" abstract_value ( COMMA abstract_value )*
        { #group_clause = #([GROUP_CLAUSE, "group_clause"], #group_clause); }
    ;

having_clause:
        "having" condition
        { #having_clause = #([HAVING_CLAUSE, "having_clause"], #having_clause); }
    ;

order_clause:
        "order" "by" abstract_value ( COMMA abstract_value )*
        { #order_clause = #([ORDER_CLAUSE, "order_clause"], #order_clause); }
    ;

limit_clause:
        "limit" INTEGER ( "offset" INTEGER )?
        { #limit_clause = #([LIMIT_CLAUSE, "limit_clause"], #limit_clause); }
    ;

select_value:
        abstract_value ( "as" field_alias )?
        { #select_value = #([SELECT_VALUE, "select_value"], #select_value); }
    ;

abstract_table:
        ( table | subquery )
    ;

condition:
TODO
    ;

abstract_value:
        ( constant | field | function )
    ;

field_alias:
        IDENTIFIER
        { #field_alias = #([FIELD_ALIAS, "field_alias"], #field_alias); }
    ;

table_alias:
        IDENTIFIER
        { #table_alias = #([TABLE_ALIAS, "table_alias"], #table_alias); }
    ;

table:
        table_name ( ( "as" )? table_alias )?
        { #table = #([TABLE, "table"], #table); }
    ;

table_name:
        IDENTIFIER
        { #table_name = #([TABLE_NAME, "table_name"], #table_name); }
    ;

subquery:
        OPEN_PAREN sql_statement CLOSE_PAREN ( "as" )? table_alias
        { #subquery = #([SUBQUERY, "subquery"], #subquery); }
    ;

constant:
//TODO: properly
        ( IDENTIFIER | QUOTED_STRING | INTEGER )
        { #constant = #([CONSTANT, "constant"], #constant); }
    ;

field:
        table_alias DOT field_name
        { #field = #([FIELD, "field"], #field); }
    ;

function: "fliblkajhdfkashfasdfkjhgble";
//TODO: properly

field_name:
        IDENTIFIER
        { #field_name = #([FIELD_NAME, "field_name"], #field_name); }
    ;

comparison_op: EQ | LT | GT | NOT_EQ | LE | GE ;





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

INTEGER: '0'..'9' ( '0'..'9' )* ;

WS: ( ' ' | '\t' | '\r' '\n' { newline(); } | '\n' { newline(); } | '\r' { newline(); } )
        {$setType(Token.SKIP);} // Ignore this token
    ;


