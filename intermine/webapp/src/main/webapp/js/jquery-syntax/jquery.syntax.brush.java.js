// brush: "java" aliases: []

//	This file is part of the "jQuery.Syntax" project, and is licensed under the GNU AGPLv3.
//	Copyright 2010 Samuel Williams. All rights reserved.
//	See <jquery.syntax.js> for licensing details.

Syntax.register('java', function(brush) {
	var keywords = ["abstract", "continue", "for", "switch", "assert", "default", "goto", "synchronized", "do", "if", "break", "implements", "throw", "else", "import", "throws", "case", "enum", "return", "transient", "catch", "extends", "try", "final", "interface", "static", "class", "finally", "strictfp", "volatile", "const", "native", "super", "while"];
	
	var access = ["private", "protected", "public", "package"];
	
	var types = ["void", "byte", "short", "int", "long", "float", "double", "boolean", "char"];
	
	var operators = ["++", "--", "++", "--", "+", "-", "~", "!", "*", "/", "%", "+", "-", "<<", ">>", ">>>", "<", ">", "<=", ">=", "==", "!=", "&", "^", "|", "&&", "||", "?", "=", "+=", "-=", "*=", "/=", "%=", "&=", "^=", "|=", "<<=", ">>=", ">>>=", "instanceof", "new", "delete"];
	
	var constants = ["this", "true", "false"];
	
	brush.push(constants, {klass: 'constant'});
	brush.push(types, {klass: 'type'});
	brush.push(keywords, {klass: 'keyword'});
	brush.push(operators, {klass: 'operator'});
	brush.push(access, {klass: 'access'});
	
	// Camel Case Types
	brush.push(Syntax.lib.camelCaseType);
	
	// Comments
	brush.push(Syntax.lib.cStyleComment);
	brush.push(Syntax.lib.cppStyleComment);
	brush.push(Syntax.lib.webLink);
	
	// Numbers
	brush.push(Syntax.lib.decimalNumber);
	brush.push(Syntax.lib.hexNumber);
	
	// Strings
	brush.push(Syntax.lib.singleQuotedString);
	brush.push(Syntax.lib.doubleQuotedString);
	brush.push(Syntax.lib.stringEscape);
	
	brush.push(Syntax.lib.cStyleFunction);
	
	brush.processes['function'] = Syntax.lib.webLinkProcess('java "Developer Documentation"', true);
});

