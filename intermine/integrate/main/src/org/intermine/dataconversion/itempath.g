//
// Grammer for path expressions
//

header {
package org.intermine.dataconversion;
}

//
// ItemPathLexer
//

class ItemPathLexer extends Lexer;

options {
  exportVocab = ItemPath;
}

tokens {
   AST_REVREF;
   AST_EXPR;
   AST_TYPE_FIELD;
   AST_FIELD_PATH;
   AST_TYPE;
   AST_CONSTRAINT;
   AST_FIELD;
   AST_SUB_PATH;
}

IDENTIFIER : ( 'a'..'z' | 'A'..'Z' | '0'..'9' | '_' )+ ;
REVREF : "<-" ;
DOT : '.' ;
OPEN_PAREN : '(' ;
CLOSE_PAREN : ')' ;
OPEN_CON : '[' ;
CLOSE_CON : ']' ;
EQUALS : '=' ;
AND : "&&" ;
QUOTED_STRING : '\'' ( ~'\'' )* '\'' ;
VARIABLE_MARKER : '$' ('0'..'9')+ ;
WS : ' ' { $setType(Token.SKIP); } ; // Ignore this token


//
// ItemPathParser
//

class ItemPathParser extends Parser;

options {
  buildAST = true;
  exportVocab = ItemPath;
  defaultErrorHandler = false;
}

expr : type field_path
                       { #expr = #([AST_EXPR, "AST_EXPR"], #expr); } ;

revref : OPEN_PAREN! expr REVREF! type_field_path CLOSE_PAREN!
                       { #revref = #([AST_REVREF, "AST_REVREF"], #revref); } ;

field_path : (DOT! field)*
                       { #field_path = #([AST_FIELD_PATH, "AST_FIELD_PATH"], #field_path); } ;

field : (IDENTIFIER OPEN_CON) => IDENTIFIER constraint_list     { #field = #([AST_FIELD, "AST_FIELD"], #field); }
        | (IDENTIFIER) => i:IDENTIFIER                     { #field = #([AST_FIELD, "AST_FIELD"], #i); } ;

type : i:IDENTIFIER    { #type = #([AST_TYPE, "AST_TYPE"], #i); }
       | revref        { #type = #([AST_TYPE, "AST_TYPE"], #type); } ;

type_field_path : IDENTIFIER DOT! IDENTIFIER
                       { #type_field_path = #([AST_TYPE_FIELD, "AST_TYPE_FIELD"], #type_field_path); } ;

constraint_list : OPEN_CON! constraint (AND! constraint)* CLOSE_CON!
                       { #constraint_list = #([AST_CONSTRAINT, "AST_CONSTRAINT"], #constraint_list); } ;

constraint : ((IDENTIFIER DOT) => sub_path | IDENTIFIER) EQUALS! (QUOTED_STRING | VARIABLE_MARKER) ;

sub_path : IDENTIFIER (DOT! IDENTIFIER)+
                       { #sub_path = #([AST_SUB_PATH, "AST_SUB_PATH"], #sub_path); } ;


