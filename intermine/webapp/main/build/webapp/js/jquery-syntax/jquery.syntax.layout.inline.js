//	This file is part of the "jQuery.Syntax" project, and is licensed under the GNU AGPLv3.
//	Copyright 2010 Samuel Williams. All rights reserved.
//	See <jquery.syntax.js> for licensing details.

Syntax.layouts.inline = function(options, code, container) {
	var inline = jQuery('<code class="syntax highlighted"></code>');
	
	inline.append(code.children());
	
	return inline;
};
