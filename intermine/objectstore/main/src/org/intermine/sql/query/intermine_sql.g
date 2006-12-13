// Flymine SQL grammar

header {
package org.intermine.sql.query;
}

class SqlTreeParser extends TreeParser;

options {
    exportVocab = Sql;
    k = 1;
    buildAST = true;
    defaultErrorHandler = false;
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
    NULL_CONSTRAINT;
    NOT_CONSTRAINT;
    AND_CONSTRAINT_SET;
    OR_CONSTRAINT_SET;
    SUBQUERY_CONSTRAINT;
    INLIST_CONSTRAINT;
}

start_rule: sql ;

sql: sql_statement ( sql_statement )*;

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

where_clause:
        #( WHERE_CLAUSE ( abstract_constraint)+ ) ;

group_clause: #( GROUP_CLAUSE ( abstract_value )+ ) ;

having_clause: #( HAVING_CLAUSE ( abstract_constraint)+ ) ;

order_clause: #( ORDER_CLAUSE ( abstract_value )+ ) ;

limit_clause: #( LIMIT_CLAUSE INTEGER ( INTEGER )? ) ;

select_value: #( SELECT_VALUE abstract_value ( field_alias )? );

abstract_table: table | subquery ;

abstract_value: unsafe_function | safe_function | typecast | constant | field ;

field_alias: #( FIELD_ALIAS (ALIAS | IDENTIFIER) ) ;

table: #( TABLE table_name ( table_alias )? ) ;

subquery: #( SUBQUERY sql table_alias ) ;

table_name: #( TABLE_NAME IDENTIFIER ) ;

table_alias: #( TABLE_ALIAS IDENTIFIER ) ;

constant: #( CONSTANT ( QUOTED_STRING | INTEGER | FLOAT | "true" | "false" | "null" ) ) ;

typecast: #( TYPECAST abstract_value ("boolean" | "real" | "double" | "smallint" | "integer" | "bigint" | "numeric" | "text")+ );

field: #( FIELD (table_alias)? field_name );

safe_function: #( SAFE_FUNCTION (
                "count"
                | "max" abstract_value
                | "min" abstract_value
                | "sum" abstract_value
                | "avg" abstract_value
                | "lower" abstract_value
                | "upper" abstract_value
                | "strpos" abstract_value abstract_value
                | "substr" abstract_value abstract_value (abstract_value)?
                | "coalesce" abstract_value abstract_value
                | "stddev" abstract_value ) ) ;

unsafe_function: #( UNSAFE_FUNCTION abstract_value
            ( ( PLUS | PERCENT | ASTERISK | DIVIDE | POWER | MINUS ) abstract_value )+ ) ;

field_name: #( FIELD_NAME IDENTIFIER );

abstract_constraint: constraint | not_constraint | and_constraint_set | or_constraint_set
        | subquery_constraint | inlist_constraint | null_constraint;

constraint:
        // (aleft != aleft) becomes NOT (aleft = aright)
        ! ( #( CONSTRAINT abstract_value NOT_EQ ))=> 
            #( CONSTRAINT aleft:abstract_value NOT_EQ aright:abstract_value )
            { #constraint = #(#[NOT_CONSTRAINT, "NOT_CONSTRAINT"],
                #(#[CONSTRAINT], #aleft, #[EQ, "="], #aright) ); }
        // (bleft >= bright) becomes NOT (bleft < bright)
        | ! ( #( CONSTRAINT abstract_value GE ))=> 
            #( CONSTRAINT bleft:abstract_value GE bright:abstract_value )
            { #constraint = #(#[NOT_CONSTRAINT, "NOT_CONSTRAINT"],
                #(#[CONSTRAINT], #bleft, #[LT, "<"], #bright) ); }
        // (cleft <= cright) becomes NOT (cright < cleft)
        | ! ( #( CONSTRAINT abstract_value LE ))=> 
            #( CONSTRAINT cleft:abstract_value LE cright:abstract_value )
            { #constraint = #(#[NOT_CONSTRAINT, "NOT_CONSTRAINT"],
                #(#[CONSTRAINT], #cright, #[LT, "<"], #cleft) ); }
        // (dleft > dright) becomes (dright < dleft)
        | ! ( #( CONSTRAINT abstract_value GT ))=> 
            #( CONSTRAINT dleft:abstract_value GT dright:abstract_value )
            { #constraint = #(#[CONSTRAINT], #dright, #[LT, "<"], #dleft); }
        | #( CONSTRAINT abstract_value comparison_op abstract_value )
    ;

null_constraint:
        ! ( #(NULL_CONSTRAINT abstract_value "not" ))=> 
            #( NULL_CONSTRAINT v:abstract_value "not" ) 
            { #null_constraint = #(#[NOT_CONSTRAINT, "NOT_CONSTRAINT"], #(#[NULL_CONSTRAINT], #v) ); }
        | #( NULL_CONSTRAINT abstract_value )
    ;

not_constraint:
        // NOT (NOT a) becomes a
        ! ( #( NOT_CONSTRAINT NOT_CONSTRAINT ))=> 
            #( NOT_CONSTRAINT #( NOT_CONSTRAINT a:n_abstract_constraint ) )
            { #not_constraint = #a; }
        // NOT (b OR c..OR..) becomes NOT b AND NOT (c..OR..)
        | ! ( #( NOT_CONSTRAINT #( OR_CONSTRAINT_SET n_abstract_constraint n_abstract_constraint )))=>
            #( NOT_CONSTRAINT #(OR_CONSTRAINT_SET b:n_abstract_constraint
                    c:n_abstract_constraint_list ) )
            { #not_constraint = #(#[AND_CONSTRAINT_SET, "AND_CONSTRAINT_SET"],
                    #(#[NOT_CONSTRAINT], #b), #(#[NOT_CONSTRAINT], #(#[OR_CONSTRAINT_SET], #c))); }
        // NOT (e AND f..AND..) becomes NOT e OR NOT (f..AND..)
        | ! ( #( NOT_CONSTRAINT #( AND_CONSTRAINT_SET n_abstract_constraint n_abstract_constraint )))=>
            #( NOT_CONSTRAINT #(AND_CONSTRAINT_SET e:n_abstract_constraint
                    f:n_abstract_constraint_list ) )
            { #not_constraint = #(#[OR_CONSTRAINT_SET, "OR_CONSTRAINT_SET"],
                    #(#[NOT_CONSTRAINT], #e), #(#[NOT_CONSTRAINT], #(#[AND_CONSTRAINT_SET], #f))); }
        | #( NOT_CONSTRAINT abstract_constraint ) ;

or_constraint_set:
        // (a..OR..) OR b..OR.. becomes a..OR.. OR b..OR..
        ! ( #( OR_CONSTRAINT_SET OR_CONSTRAINT_SET n_abstract_constraint ))=>
            #( ta:OR_CONSTRAINT_SET #( OR_CONSTRAINT_SET a:n_abstract_constraint_list )
                b:n_abstract_constraint_list )
            { #or_constraint_set = #(#ta, #a, #b); }

        // d..OR.. OR (e..OR..) OR f..OR.. becomes d..OR.. OR e..OR.. OR f..OR..
        | ! ( #( OR_CONSTRAINT_SET n_abstract_constraint_list_notor OR_CONSTRAINT_SET
                    n_abstract_constraint ))=>
            #( tc:OR_CONSTRAINT_SET d:n_abstract_constraint_list_notor #( OR_CONSTRAINT_SET
                    e:n_abstract_constraint_list ) f:n_abstract_constraint_list )
            { #or_constraint_set = #(#tc, #d, #e, #f); }

        // g..OR.. OR (h..OR..) becomes g..OR.. OR h..OR..
        | ! ( #( OR_CONSTRAINT_SET n_abstract_constraint_list_notor OR_CONSTRAINT_SET ))=>
            #( td:OR_CONSTRAINT_SET g:n_abstract_constraint_list_notor #( OR_CONSTRAINT_SET
                    h:n_abstract_constraint_list ))
            { #or_constraint_set = #(#td, #g, #h); }

        // (i AND j..AND..) OR k..OR.. becomes (i OR k..OR..) AND ((j..AND..) OR k..OR..)
        | ! ( #( OR_CONSTRAINT_SET #( AND_CONSTRAINT_SET n_abstract_constraint n_abstract_constraint_list ) n_abstract_constraint ))=>
            #( te:OR_CONSTRAINT_SET #( tf:AND_CONSTRAINT_SET i:n_abstract_constraint
                    j:n_abstract_constraint_list ) k:n_abstract_constraint_list )
            { AST te2_AST = astFactory.create(te);
                AST k2_AST = astFactory.dupList(k_AST);
                AST tf2_AST = astFactory.create(tf);
                #or_constraint_set = #(#tf, #(#te, #i, #k), #(te2_AST, #(tf2_AST, #j), k2_AST));
        }

        // l..OR.. OR (m AND n..AND..) OR o..OR.. becomes
        //                      (l..OR.. OR m OR o..OR..) AND (l..OR.. OR (n..AND..) OR o..OR..)
        | ! ( #( OR_CONSTRAINT_SET n_abstract_constraint_list_notand #( AND_CONSTRAINT_SET
                        n_abstract_constraint n_abstract_constraint )
                    n_abstract_constraint_list ))=>
            #( tg:OR_CONSTRAINT_SET l:n_abstract_constraint_list_notand #( th:AND_CONSTRAINT_SET
                    m:n_abstract_constraint n:n_abstract_constraint_list ) o:n_abstract_constraint_list )
            { AST tg2_AST = astFactory.create(tg);
                AST l2_AST = astFactory.dupList(l_AST);
                AST o2_AST = astFactory.dupList(o_AST);
                AST th2_AST = astFactory.create(th);
                #or_constraint_set = #(#th, #(#tg, #l, #m, #o), #(tg2_AST, l2_AST, #( th2_AST, #n), o2_AST)); }

        // p..OR.. OR (q AND r..AND..) becomes (p..OR.. OR q) AND (p..OR.. OR (r..AND..))
        | ! ( #( OR_CONSTRAINT_SET n_abstract_constraint_list_notand #( AND_CONSTRAINT_SET n_abstract_constraint n_abstract_constraint )))=>
            #( ti:OR_CONSTRAINT_SET p:n_abstract_constraint_list_notand #( tj:AND_CONSTRAINT_SET
                    q:n_abstract_constraint r:n_abstract_constraint_list ) )
            { AST ti2_AST = astFactory.create(ti);
                AST p2_AST = astFactory.dupList(p_AST);
                AST tj2_AST = astFactory.create(tj);
                #or_constraint_set = #(#tj, #(#ti, #p, #q), #(ti2_AST, p2_AST, #(tj2_AST, #r))); }

        | ( #( OR_CONSTRAINT_SET n_abstract_constraint n_abstract_constraint ))=>
            #( OR_CONSTRAINT_SET ( abstract_constraint )+ )

        // (OR z) becomes z
        | ! #( OR_CONSTRAINT_SET z:n_abstract_constraint )
            { #or_constraint_set = #z; } ;

and_constraint_set:
        // (a..AND..) AND b..AND.. becomes a..AND.. b..AND..
        ! ( #( AND_CONSTRAINT_SET AND_CONSTRAINT_SET n_abstract_constraint ))=>
            #( ta:AND_CONSTRAINT_SET #( AND_CONSTRAINT_SET a:n_abstract_constraint_list )
                b:n_abstract_constraint_list )
            { #and_constraint_set = #(#ta, #a, #b); }

        // d..AND.. AND (e..AND..) AND f..AND.. becomes d..AND.. AND e..AND.. AND f..AND..
        | ! ( #( AND_CONSTRAINT_SET n_abstract_constraint_list_notand AND_CONSTRAINT_SET
                    n_abstract_constraint ))=>
            #( tc:AND_CONSTRAINT_SET d:n_abstract_constraint_list_notand #( AND_CONSTRAINT_SET
                    e:n_abstract_constraint_list ) f:n_abstract_constraint_list )
            { #and_constraint_set = #(#tc, #d, #e, #f); }

        // g..AND.. AND (h..AND..) becomes g..AND.. h..AND..
        | ! ( #( AND_CONSTRAINT_SET n_abstract_constraint_list_notand AND_CONSTRAINT_SET ))=>
            #( td:AND_CONSTRAINT_SET g:n_abstract_constraint_list_notand #( AND_CONSTRAINT_SET
                    h:n_abstract_constraint_list ))
            { #and_constraint_set = #(#td, #g, #h); }

        | ( #( AND_CONSTRAINT_SET n_abstract_constraint n_abstract_constraint ))=>
            #( te:AND_CONSTRAINT_SET (abstract_constraint )+ )
            { }

        // (AND z) becomes z
        | ! #( tf:AND_CONSTRAINT_SET z:n_abstract_constraint )
            { #and_constraint_set = #z; } ;

subquery_constraint: #( SUBQUERY_CONSTRAINT abstract_value sql ) ;

inlist_constraint: #( INLIST_CONSTRAINT abstract_value ( constant )+ );

comparison_op: EQ | LT | GT | NOT_EQ | LE | GE | GORNULL | "like";

abstract_constraint_list: ( abstract_constraint )+ ;

abstract_constraint_list_notand: ( constraint | not_constraint | or_constraint_set
            | subquery_constraint | inlist_constraint )+ ;

abstract_constraint_list_notor: ( constraint | not_constraint | and_constraint_set
            | subquery_constraint | inlist_constraint )+ ;


n_abstract_constraint: n_constraint | n_not_constraint
        | n_and_constraint_set | n_or_constraint_set
        | subquery_constraint | inlist_constraint | n_null_constraint;

n_constraint: #( CONSTRAINT abstract_value comparison_op abstract_value ) ;

n_null_constraint: #( NULL_CONSTRAINT abstract_value ("not")? );

n_not_constraint: #( NOT_CONSTRAINT n_abstract_constraint ) ;

n_and_constraint_set: #( AND_CONSTRAINT_SET (n_abstract_constraint)+ ) ;

n_or_constraint_set: #( OR_CONSTRAINT_SET (n_abstract_constraint)+ ) ;

n_abstract_constraint_list: ( n_abstract_constraint )+ ;

n_abstract_constraint_list_notand: ( n_constraint
            | n_not_constraint | n_or_constraint_set
            | subquery_constraint | inlist_constraint | n_null_constraint )+ ;

n_abstract_constraint_list_notor: ( n_constraint
            | n_not_constraint | n_and_constraint_set
            | subquery_constraint | inlist_constraint | n_null_constraint )+ ;



class SqlParser extends Parser;

options {
    exportVocab = Sql;
    k = 6;
    buildAST = true;
    defaultErrorHandler = false;
}

start_rule: sql (SEMI!)?;

sql: sql_statement ( "union"! sql_statement )*;

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
        "limit"! INTEGER ( "offset"! INTEGER )?
        { #limit_clause = #([LIMIT_CLAUSE, "LIMIT_CLAUSE"], #limit_clause); }
    ;

select_value:
        ( (unsafe_function)=> unsafe_function "as"! field_alias
            | (typecast)=> typecast "as"! field_alias
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
        (unsafe_function)=> unsafe_function | (typecast)=> typecast | constant | field | safe_function | paren_value
    ;

safe_abstract_value:
        (typecast)=> typecast | constant | field | safe_function | paren_value
    ;

paren_value: OPEN_PAREN! abstract_value CLOSE_PAREN! ;

field_alias:
        (ALIAS | IDENTIFIER)
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
        OPEN_PAREN! sql CLOSE_PAREN! ( "as"! )? table_alias
        { #subquery = #([SUBQUERY, "SUBQUERY"], #subquery); }
    ;

constant:
//TODO: properly
        ( QUOTED_STRING | INTEGER | FLOAT | "true" | "false" | "null" )
        { #constant = #([CONSTANT, "CONSTANT"], #constant); }
    ;

field:
        ( table_alias DOT! )? field_name
        { #field = #([FIELD, "FIELD"], #field); }
    ;

typecast:
        (constant | field | safe_function | paren_value) ( COLONTYPE! ("boolean" | "real" | "double" "precision"! | "smallint" | "integer" | "bigint" | "numeric" | "text") )+
        {#typecast = #([TYPECAST, "TYPECAST"], #typecast); }
    ;
    
safe_function:
        (
            "count" OPEN_PAREN! ASTERISK! CLOSE_PAREN!
            | "max" OPEN_PAREN! abstract_value CLOSE_PAREN!
            | "min" OPEN_PAREN! abstract_value CLOSE_PAREN!
            | "sum" OPEN_PAREN! abstract_value CLOSE_PAREN!
            | "avg" OPEN_PAREN! abstract_value CLOSE_PAREN!
            | "lower" OPEN_PAREN! abstract_value CLOSE_PAREN!
            | "upper" OPEN_PAREN! abstract_value CLOSE_PAREN!
            | "strpos" OPEN_PAREN! abstract_value COMMA! abstract_value CLOSE_PAREN!
            | "substr" OPEN_PAREN! abstract_value COMMA! abstract_value (COMMA! abstract_value)? CLOSE_PAREN!
            | "coalesce" OPEN_PAREN! abstract_value COMMA! abstract_value CLOSE_PAREN!
            | "stddev" OPEN_PAREN! abstract_value CLOSE_PAREN!
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

abstract_constraint: (gornull_constraint)=> gornull_constraint | (constraint_set)=> constraint_set | safe_abstract_constraint ;

safe_abstract_constraint: (paren_constraint)=> paren_constraint
            | (subquery_constraint)=> subquery_constraint
            | (inlist_constraint)=> inlist_constraint
            | constraint
            | not_constraint
    ;

constraint: (abstract_value comparison_op)=> abstract_value comparison_op abstract_value
        { #constraint = #([CONSTRAINT, "CONSTRAINT"], #constraint); }
            | abstract_value null_comparison
        { #constraint = #([NULL_CONSTRAINT, "NULL_CONSTRAINT"], #constraint); }
    ;

gornull_constraint: a:IDENTIFIER DOT! b:IDENTIFIER GT! c:constant "or"! d:IDENTIFIER DOT! e:IDENTIFIER "is"! "null"!
//        { String message = "gornull constraint: " + a_AST + "." + b_AST + " > " + c_AST + " OR " + d_AST + "." + e_AST + " IS NULL"; if (!(a_AST.getText().equals(d_AST.getText()) && b_AST.getText().equals(e_AST.getText()))) { System.out.println("Not a " + message); throw new RecognitionException("Not a " + message); } else { System.out.println("Found a " + message); }; }?
        { a_AST.getText().equals(d_AST.getText()) && b_AST.getText().equals(e_AST.getText()) }?
        { #gornull_constraint = #([CONSTRAINT, "CONSTRAINT"], #([FIELD, "FIELD"], #([TABLE_ALIAS, "TABLE_ALIAS"], #a), #([FIELD_NAME, "FIELD_NAME"], #b)), #([GORNULL, "n>"]), #c); }
    ;

not_constraint: "not"! safe_abstract_constraint
        { #not_constraint = #([NOT_CONSTRAINT, "NOT_CONSTRAINT"], #not_constraint); }
    ;

paren_constraint: OPEN_PAREN! abstract_constraint CLOSE_PAREN! ;

constraint_set: (and_constraint_set)=> and_constraint_set | or_constraint_set;

or_constraint_set: 
        safe_abstract_constraint ("or"! safe_abstract_constraint)+
        { #or_constraint_set = #([OR_CONSTRAINT_SET, "OR_CONSTRAINT_SET"], #or_constraint_set); }
    ;

and_constraint_set:
        safe_abstract_constraint ("and"! safe_abstract_constraint)+
        { #and_constraint_set = #([AND_CONSTRAINT_SET, "AND_CONSTRAINT_SET"], #and_constraint_set); }
    ;

subquery_constraint: (abstract_value "in" )=> abstract_value "in"! OPEN_PAREN! sql CLOSE_PAREN!
        { #subquery_constraint = #([SUBQUERY_CONSTRAINT, "SUBQUERY_CONSTRAINT"],
                #subquery_constraint); }
        | abstract_value "not"! "in"! OPEN_PAREN! sql CLOSE_PAREN!
        { #subquery_constraint = #([NOT_CONSTRAINT, "NOT_CONSTRAINT"], #([SUBQUERY_CONSTRAINT, "SUBQUERY_CONSTRAINT"], #subquery_constraint)); }
    ;

inlist_constraint: (abstract_value "in" )=> abstract_value "in"! OPEN_PAREN! constant ( COMMA! constant )* CLOSE_PAREN!
        { #inlist_constraint = #([INLIST_CONSTRAINT, "INLIST_CONSTRAINT"], inlist_constraint); }
        | abstract_value "not"! "in"! OPEN_PAREN! constant ( COMMA! constant )* CLOSE_PAREN!
        { #inlist_constraint = #([NOT_CONSTRAINT, "NOT_CONSTRAINT"], #([INLIST_CONSTRAINT, "INLIST_CONSTRAINT"], inlist_constraint)); }
    ;

comparison_op: EQ | LT | GT | NOT_EQ | LE | GE | GORNULL | "like";

null_comparison: "is"! "null"! | "is"! "not" "null"!;




class SqlLexer extends Lexer;

options {
    exportVocab = Sql;
    testLiterals = false;
    k = 2;
    caseSensitive = false;
    caseSensitiveLiterals = false;
    charVocabulary = '\3'..'\177';
    defaultErrorHandler = false;
}

IDENTIFIER options { testLiterals=true; } :
        'a'..'z' ( 'a'..'z' | '0'..'9' | '_' | '$' | '#' )*
    ;

ALIAS options { testLiterals=true; } :
        '"' 'a'..'z' ( 'a'..'z' | '0'..'9' | '_' | '$' | '#' )* '"'
    ;

QUOTED_STRING:
        '\'' ( ~'\'' )* ( '\'' '\'' ( ~'\'' )* )* '\''
    ;

SEMI: ';';
DOT: '.';
COMMA: ',';
ASTERISK: '*';
AT_SIGN: '@';
OPEN_PAREN: '(';
CLOSE_PAREN: ')';
PLUS: '+';
MINUS: ( '-' ( '0'..'9' )+ '.' '0'..'9' )=> '-' ( '0'..'9' )+ '.' ( '0'..'9' )+ ( 'e' ( '-' | '+' )? ( '0'..'9' )+ )? ("::real")? {_ttype = FLOAT; }
        | ( '-' '0'..'9' )=> '-' ( '0'..'9' )+ {_ttype = INTEGER; }
        | '-';
DIVIDE: '/';
PERCENT: '%';
VERTBAR: '|';
COLONTYPE: "::";

EQ: '=';
NOT_EQ:
        '<' { _ttype = LT; } ( ( '>' { _ttype = NOT_EQ; } ) | ( '=' { _ttype = LE; } ) )?
        | "!=" | "^="
    ;
GT: '>' ( '=' { _ttype = GE; } )? ;
GORNULL: "n>";

FLOAT: (( '0'..'9' )+ '.' '0'..'9' )=> ( '0'..'9' )+ '.' ( '0'..'9' )+ ( 'e' ( '-' | '+' )? ( '0'..'9' )+ )? ("::real")? 
        | ( '0'..'9' )+ {_ttype = INTEGER; } ;

WS: ( ' ' | '\t' | '\r' '\n' { newline(); } | '\n' { newline(); } | '\r' { newline(); } )
        {$setType(Token.SKIP);} // Ignore this token
    ;


