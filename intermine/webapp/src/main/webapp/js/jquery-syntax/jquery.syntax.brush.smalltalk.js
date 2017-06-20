// brush: "smalltalk" aliases: []

//	This file is part of the "jQuery.Syntax" project, and is licensed under the GNU AGPLv3.
//	Copyright 2010 Samuel Williams. All rights reserved.
//	See <jquery.syntax.js> for licensing details.

Syntax.register('smalltalk', function(brush) {
	var operators = ["[", "]", "|", ":=", "."];
	
	var values = ["self", "super", "true", "false", "nil"];
	
	brush.push(values, {klass: 'constant'});
	brush.push(operators, {klass: 'operator'});
	
	// Objective-C style functions
	brush.push({pattern: /\w+:/g, klass: 'function'});
	
	// Camelcase Types
	brush.push(Syntax.lib.camelCaseType);
	
	// Strings
	brush.push(Syntax.lib.singleQuotedString);
	brush.push(Syntax.lib.doubleQuotedString);
	brush.push(Syntax.lib.stringEscape);
	
	// Numbers
	brush.push(Syntax.lib.decimalNumber);
	brush.push(Syntax.lib.hexNumber);
});
