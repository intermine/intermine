// InterMine OQL-like grammar

header {
package org.intermine.objectstore.query.iql;
}

class IqlParser extends Parser;

options {
    exportVocab = Iql;
    k = 6;
    buildAST = true;
    defaultErrorHandler=false;
}

tokens {
    IQL_STATEMENT;
    SELECT_LIST;
    FROM_LIST;
    WHERE_CLAUSE;
    GROUP_CLAUSE;
    ORDER_CLAUSE;
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
    SUBQUERY_EXISTS_CONSTRAINT;
    CONTAINS_CONSTRAINT;
    NOTLIKE;
    BAG_CONSTRAINT;
    TYPECAST;
    FIELD_PATH_EXPRESSION;
    OBJECTSTOREBAG;
    ORDER_DESC;
}


start_rule: iql_statement;

iql_statement: select_command
        { #iql_statement = #([IQL_STATEMENT, "IQL_STATEMENT"], #iql_statement); }
    ;

select_command:
        ( "explain" )? "select"! ( "all"! | "distinct" )? select_list
        ( from_list )?
        ( where_clause )?
        ( group_clause )?
        ( order_clause )?
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

order_clause:
        "order"! "by"! orderby_value ( COMMA! orderby_value )*
        { #order_clause = #([ORDER_CLAUSE, "ORDER_CLAUSE"], #order_clause); }
    ;

select_value:
        ( (unsafe_function)=> unsafe_function "as"! field_alias
            | (typecast)=> typecast "as"! field_alias
            | (field_path_expression)=> field_path_expression "as"! field_alias
            | thing ( "as"! field_alias )?
            | constant "as"! field_alias
            | safe_function "as"! field_alias
            | paren_value "as"! field_alias
            | objectstorebag
        )
        { #select_value = #([SELECT_VALUE, "SELECT_VALUE"], #select_value); }
    ;

abstract_table:
        table | subquery | multitable | query_class_bag | query_class_bag_multi
    ;

orderby_value:
        (abstract_value "desc")=> abstract_value "desc"!
        { #orderby_value = #([ORDER_DESC, "ORDER_DESC"], #orderby_value); }
        | abstract_value
    ;

abstract_value:
        (unsafe_function)=> unsafe_function | (typecast)=> typecast | constant | thing | safe_function | paren_value
    ;

safe_abstract_value:
        (typecast)=> typecast | constant | thing | safe_function | paren_value
    ;

typecast: (constant | thing | safe_function | paren_value) COLONTYPE! IDENTIFIER
        { #typecast = #([TYPECAST, "TYPECAST"], #typecast); }
    ;

paren_value: OPEN_PAREN! abstract_value CLOSE_PAREN! ;

objectstorebag: "BAG"! OPEN_PAREN! INTEGER CLOSE_PAREN!
        { #objectstorebag = #([OBJECTSTOREBAG, "OBJECTSTOREBAG"], #objectstorebag); }
    ;

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

multitable:
        OPEN_PAREN! table_name ( COMMA! table_name )* CLOSE_PAREN! ( "as"! )? table_alias
        { #multitable = #([TABLE, "TABLE"], #multitable); }
    ;

table_name:
        ( IDENTIFIER DOT! )* IDENTIFIER
        { #table_name = #([TABLE_NAME, "TABLE_NAME"], #table_name); }
    ;

query_class_bag:
        QUESTION_MARK COLONTYPE! table_name "as"! table_alias
        { #query_class_bag = #([TABLE, "TABLE"], #query_class_bag); }
        | objectstorebag COLONTYPE! table_name "as"! table_alias
        { #query_class_bag = #([TABLE, "TABLE"], #query_class_bag); }
    ;

query_class_bag_multi:
        QUESTION_MARK COLONTYPE! OPEN_PAREN! table_name ( COMMA! table_name )* CLOSE_PAREN! "as"! table_alias
        { #query_class_bag_multi = #([TABLE, "TABLE"], #query_class_bag_multi); }
        | objectstorebag COLONTYPE! OPEN_PAREN! table_name ( COMMA! table_name )* CLOSE_PAREN! "as"! table_alias
        { #query_class_bag_multi = #([TABLE, "TABLE"], #query_class_bag_multi); }
    ;

subquery:
        OPEN_PAREN! iql_statement CLOSE_PAREN! ( "as"! )? table_alias
        { #subquery = #([SUBQUERY, "SUBQUERY"], #subquery); }
    ;

constant:
//TODO: properly
        ( QUOTED_STRING | INTEGER | FLOAT | "true" | "false" | "null" )
        { #constant = #([CONSTANT, "CONSTANT"], #constant); }
    ;

field_path_expression:
        IDENTIFIER DOT! IDENTIFIER DOT! IDENTIFIER OPEN_PAREN! "def"! constant CLOSE_PAREN!
        { #field_path_expression = #([FIELD_PATH_EXPRESSION, "FIELD_PATH_EXPRESSION"], #field_path_expression); }
    ;

thing:
        ( IDENTIFIER DOT! )* IDENTIFIER
        { #thing = #([FIELD, "FIELD"], #thing); }
    ;

safe_function:
        (
            "count" OPEN_PAREN! ASTERISK! CLOSE_PAREN!
            | "max" OPEN_PAREN! abstract_value CLOSE_PAREN!
            | "min" OPEN_PAREN! abstract_value CLOSE_PAREN!
            | "sum" OPEN_PAREN! abstract_value CLOSE_PAREN!
            | "avg" OPEN_PAREN! abstract_value CLOSE_PAREN!
            | "substr" OPEN_PAREN! abstract_value COMMA! abstract_value (COMMA! abstract_value)? CLOSE_PAREN!
            | "indexof" OPEN_PAREN! abstract_value COMMA! abstract_value CLOSE_PAREN!
            | "lower" OPEN_PAREN! abstract_value CLOSE_PAREN!
            | "upper" OPEN_PAREN! abstract_value CLOSE_PAREN!
            | "stddev" OPEN_PAREN! abstract_value CLOSE_PAREN!
        )
        { #safe_function = #([SAFE_FUNCTION, "SAFE_FUNCTION"], #safe_function); }
    ;

unsafe_function:
        (
            (safe_abstract_value PLUS)=> safe_abstract_value PLUS safe_abstract_value
            | (safe_abstract_value PERCENT)=> safe_abstract_value PERCENT safe_abstract_value
            | (safe_abstract_value ASTERISK)=> safe_abstract_value ASTERISK safe_abstract_value
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
            | (bag_constraint)=> bag_constraint
            | (subquery_constraint)=> subquery_constraint
            | (contains_constraint)=> contains_constraint
            | "true"
            | "false"
            | subquery_exists_constraint
            | constraint
            | not_constraint
    ;

constraint: (abstract_value ISNULL )=> abstract_value ISNULL
        { #constraint = #([CONSTRAINT, "CONSTRAINT"], #constraint); }
        | (abstract_value ISNOTNULL )=> abstract_value ISNOTNULL
        { #constraint = #([CONSTRAINT, "CONSTRAINT"], #constraint); }
        | (thing ( EQ | NOT_EQ ) QUESTION_MARK )=> thing ( EQ | NOT_EQ ) QUESTION_MARK
        { #constraint = #([CONSTRAINT, "CONSTRAINT"], #constraint); }
        | abstract_value comparison_op abstract_value
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

subquery_constraint: (abstract_value "in" )=> abstract_value "in"! OPEN_PAREN! iql_statement CLOSE_PAREN!
        { #subquery_constraint = #([SUBQUERY_CONSTRAINT, "SUBQUERY_CONSTRAINT"],
                #subquery_constraint); }
        | abstract_value "not"! "in"! OPEN_PAREN! iql_statement CLOSE_PAREN!
        { #subquery_constraint = #([NOT_CONSTRAINT, "NOT_CONSTRAINT"], #([SUBQUERY_CONSTRAINT, "SUBQUERY_CONSTRAINT"], #subquery_constraint)); }
    ;

subquery_exists_constraint: "exists"! OPEN_PAREN! iql_statement CLOSE_PAREN!
        { #subquery_exists_constraint = #([SUBQUERY_EXISTS_CONSTRAINT, "SUBQUERY_EXISTS_CONSTRAINT"], #subquery_exists_constraint); }
        | "does"! "not"! "exist"! OPEN_PAREN! iql_statement CLOSE_PAREN!
        { #subquery_exists_constraint = #([NOT_CONSTRAINT, "NOT_CONSTRAINT"], #([SUBQUERY_EXISTS_CONSTRAINT, "SUBQUERY_EXISTS_CONSTRAINT"], #subquery_exists_constraint)); }
    ;

bag_constraint: (abstract_value "in" objectstorebag )=> abstract_value "in"! objectstorebag
        { #bag_constraint = #([BAG_CONSTRAINT, "BAG_CONSTRAINT"], #bag_constraint); }
        | (abstract_value "not" "in" objectstorebag )=> abstract_value "not"! "in"! objectstorebag
        { #bag_constraint = #([NOT_CONSTRAINT, "NOT_CONSTRAINT"], #([BAG_CONSTRAINT, "BAG_CONSTRAINT"], #bag_constraint)); }
        | (abstract_value "in" )=> abstract_value "in"! QUESTION_MARK!
        { #bag_constraint = #([BAG_CONSTRAINT, "BAG_CONSTRAINT"], #bag_constraint); }
        | abstract_value "not"! "in"! QUESTION_MARK!
        { #bag_constraint = #([NOT_CONSTRAINT, "NOT_CONSTRAINT"], #([BAG_CONSTRAINT, "BAG_CONSTRAINT"], #bag_constraint)); }
    ;

contains_constraint: (collection_from_question_mark "contains" QUESTION_MARK)=> collection_from_question_mark "contains"! QUESTION_MARK
        { #contains_constraint = #([CONTAINS_CONSTRAINT, "CONTAINS_CONSTRAINT"], #contains_constraint); }
        | (collection_from_question_mark "does" "not" "contain" QUESTION_MARK)=> collection_from_question_mark "does"! "not"! "contain"! QUESTION_MARK
        { #contains_constraint = #([NOT_CONSTRAINT, "NOT_CONSTRAINT"], #([CONTAINS_CONSTRAINT, "CONTAINS_CONSTRAINT"], #contains_constraint)); }
        | (collection_from_question_mark "contains" )=> collection_from_question_mark "contains"! thing
        { #contains_constraint = #([CONTAINS_CONSTRAINT, "CONTAINS_CONSTRAINT"], #contains_constraint); }
        | (collection_from_question_mark "does" "not" "contain" )=> collection_from_question_mark "does"! "not"! "contain"! thing
        { #contains_constraint = #([NOT_CONSTRAINT, "NOT_CONSTRAINT"], #([CONTAINS_CONSTRAINT, "CONTAINS_CONSTRAINT"], #contains_constraint)); }
        | (thing "contains" QUESTION_MARK)=> thing "contains"! QUESTION_MARK
        { #contains_constraint = #([CONTAINS_CONSTRAINT, "CONTAINS_CONSTRAINT"],
                #contains_constraint); }
        | (thing "contains" )=> thing "contains"! thing
        { #contains_constraint = #([CONTAINS_CONSTRAINT, "CONTAINS_CONSTRAINT"],
                #contains_constraint); }
        | (thing "does" "not" "contain" QUESTION_MARK)=> thing "does"! "not"! "contain"! QUESTION_MARK
        { #contains_constraint = #([NOT_CONSTRAINT, "NOT_CONSTRAINT"], #([CONTAINS_CONSTRAINT, "CONTAINS_CONSTRAINT"], #contains_constraint)); }
        | thing "does"! "not"! "contain"! thing
        { #contains_constraint = #([NOT_CONSTRAINT, "NOT_CONSTRAINT"], #([CONTAINS_CONSTRAINT, "CONTAINS_CONSTRAINT"], #contains_constraint)); }
    ;

collection_from_question_mark: QUESTION_MARK DOT! IDENTIFIER
        { #collection_from_question_mark = #([FIELD, "FIELD"], #collection_from_question_mark); }
    ;

comparison_op: EQ | LT | GT | NOT_EQ | LE | GE | "like" | "not" "like" { #comparison_op = #[NOTLIKE, "NOTLIKE"]; };





class IqlLexer extends Lexer;

options {
    exportVocab = Iql;
    testLiterals = false;
    k = 5;
    caseSensitive = false;
    caseSensitiveLiterals = false;
    charVocabulary = '\3'..'\177';
    defaultErrorHandler=false;
}

ISNULL: "is null";

ISNOTNULL: "is not null";

IDENTIFIER options { testLiterals=true; } :
        ('a'..'z' | '"') ('"' | 'a'..'z' | '0'..'9' | '_' | '$' | '#' )*
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
MINUS: ( '-' ( '0'..'9' )+ '.' '0'..'9' )=> '-' ( '0'..'9' )+ '.' ( '0'..'9' )+ ( 'e' ( '-' | '+' )? ( '0'..'9' )+ )? {_ttype = FLOAT; }
        | ( '-' '0'..'9' )=> '-' ( '0'..'9' )+ {_ttype = INTEGER; }
        | '-';
DIVIDE: '/';
PERCENT: '%';
VERTBAR: '|';
QUESTION_MARK: '?';
COLONTYPE: "::";

EQ: '=';
NOT_EQ:
        '<' { _ttype = LT; } ( ( '>' { _ttype = NOT_EQ; } ) | ( '=' { _ttype = LE; } ) )?
        | "!=" | "^="
    ;
GT: '>' ( '=' { _ttype = GE; } )? ;

FLOAT: (( '0'..'9' )+ '.' '0'..'9' )=> ( '0'..'9' )+ '.' ( '0'..'9' )+ ( 'e' ( '-' | '+' )? ( '0'..'9' )+ )?
        | ( '0'..'9' )+ {_ttype = INTEGER; } ;

WS: ( ' ' | '\t' | '\r' '\n' { newline(); } | '\n' { newline(); } | '\r' { newline(); } )
        {$setType(Token.SKIP);} // Ignore this token
    ;


