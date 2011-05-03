// brush: "lisp" aliases: ['scheme', 'clojure']

//	This file is part of the "jQuery.Syntax" project, and is licensed under the GNU AGPLv3.
//	Copyright 2010 Samuel Williams. All rights reserved.
//	See <jquery.syntax.js> for licensing details.

Syntax.lib.lispStyleComment = {pattern: /(;+) .*$/gm, klass: 'comment', allow: ['href']};

// This syntax is intentionally very sparse. This is because it is a general syntax for Lisp like languages.
// It might be a good idea to make specific dialects (e.g. common lisp, scheme, clojure, etc)
Syntax.register('lisp', function(brush) {
	brush.push(['(', ')'], {klass: 'operator'});
	
	brush.push(Syntax.lib.lispStyleComment);
	
	// Hex, Octal and Binary numbers :)
	brush.push({
		pattern: /0x[0-9a-fA-F]+/g,
		klass: 'constant'
	});
	
	brush.push(Syntax.lib.decimalNumber);
	brush.push(Syntax.lib.webLink);
	
	brush.push({
		pattern: /\(([^\s\(\)]+)/gi,
		matches: Syntax.extractMatches({klass: 'function'})
	});
	
	brush.push({
		pattern: /\(([^\s\(\)]+)/gi,
		matches: Syntax.extractMatches({klass: 'function'})
	});
	
	brush.push({
		pattern: /#[a-z]+/gi,
		klass: 'constant'
	})
	
	// Strings
	brush.push(Syntax.lib.doubleQuotedString);
	brush.push(Syntax.lib.stringEscape);
});

