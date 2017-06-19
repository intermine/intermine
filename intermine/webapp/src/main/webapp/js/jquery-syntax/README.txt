*** jQuery.Syntax [release-2.2] ***

jQuery.Syntax is a light-weight client-side syntax highlighter, which dynamically loads external dependencies (js & css) when required. It uses jQuery to make it cross-browser compatible and to simplify integration.

For downloads, documentation, compatibility, please see :
	http://www.oriontransfer.co.nz/software/jquery-syntax/

There are several plugins available (source code - for stable releases see the main project page above):
	[DokuWiki] -> http://github.com/ioquatix/jquery-syntax-dokuwiki
	[WordPress] -> http://github.com/ioquatix/jquery-syntax-wordpress

For licensing details, please see the included LICENSE.txt.

*** License ***

The "jQuery.Syntax" project is licensed under the GNU AGPLv3.
Copyright 2010 Samuel Williams. All rights reserved.

For more information, please see http://www.oriontransfer.co.nz/software/jquery-syntax

This program is free software: you can redistribute it and/or modify it under the terms
of the GNU Affero General Public License as published by the Free Software Foundation,
either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with this
program. If not, see <http://www.gnu.org/licenses/>.

"Additional permissions" (as per Section 7 of the AGPLv3)
	1. Any HTML file which merely makes function calls to this code, and for that purpose 
	includes it by reference shall be deemed a separate work for copyright law purposes.
	
	2. You may distribute non-source (e.g., minimized or compacted) forms of that code 
	without the copy of the GNU AGPL normally required by section 4, provided you include
	this license notice and a URL through which recipients can access the Corresponding
	Source.

If you modify this code, you must include these "Additional permissions" in your version 
of the code.

*** Change Log ***

release-2.2.2
 - Major Internet Exploder bugfix.

release-2.2
 - Added support for Lua, C#
 - Added new fixed and list layouts.
 - Improvements to whitespace handling.
 - Several bug fixes and enhancements.

release-1.9.1
 - Added several new languages.
 - Minor bug fixes and enhancements.

release-1.8
 - Improved the simple function usability.
 - Improved shorthand notation so it is now more flexible.
   - You can now specify a combination of classes, in any order.

release-1.7
 - Added support for YAML.
 - Fixed a bug in the bisection algorithm.
 - Added support for highlighting inline code tags.
 - Added shorthand notation <code class="syntax html"> (vs <code class="syntax brush-html">).
   - However, this only works in this exact case where class="syntax {language}"
 - Improved simple function to improve consistency.

release-1.6
 - Fixed a compatibility issue with jQuery 1.4.2.

release-1.5
 - Added support for [Visual] Basic, SQL and Lisp.
 - Enabled a small optimization in the bisection algorithm.
 - Fixed a bug with alias names (they weren't working).
 - Minor updates to several other brushes.
