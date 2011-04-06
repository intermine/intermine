// brush: "php" aliases: []

//	This file is part of the "jQuery.Syntax" project, and is licensed under the GNU AGPLv3.
//	Copyright 2010 Samuel Williams. All rights reserved.
//	See <jquery.syntax.js> for licensing details.

Syntax.brushes.dependency('php', 'php-script');

Syntax.register('php', function(brush) {
	brush.push({
		pattern: /(<\?(php)?)((.|\n)*?)(\?>)/gm,
		matches: Syntax.extractMatches({klass: 'operator'}, null, {brush: 'php-script'}, null, {klass: 'operator'})
	})
});

