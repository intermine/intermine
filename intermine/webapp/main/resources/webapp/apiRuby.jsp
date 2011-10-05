<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<html:xhtml/>

<!-- apiRuby.jsp -->

<im:boxarea titleKey="api.ruby.titleKey" stylename="gradientbox" fixedWidth="90%" htmlId="apiruby">

<div>
  <p>
    The Ruby web service client library makes it easy to run queries in <c:out value="${WEB_PROPERTIES['project.title']}"/> 
    using the InterMine API directly from Ruby programs.
    You can use the Ruby library to construct any query you could run from web interface and fetch the results in a number of
    structured formats, including reified objects, and native Ruby data structures such as lists and hashes.
  </p>
</div>
<br>
<div>

<ul>
  <li>
    <div onclick="javascript:showText('prerequisite')"><h3 style="font-weight: bold;">Prerequisites</h3></div>
    <div id="prerequisite" style="padding: 5px">
      <p>
        You should install the <a href="http://rubygems.org/gems/intermine" target="_blank">
        Ruby webservice client library module</a> to get started.
        You can install it directly from:
        <a href="http://rubygems.org">RubyGems</a>. This is a public
        repository of thousands of modules. To install the InterMine Ruby
        client library make sure you have rubygems installed, and then
        type the following command into a shell:
        <br>
    <pre>&gt; gem install intermine</pre>
        <br>
      </p>
      <p>
        This module is pure Ruby with only one external dependency (json).
        The client library has been tested on Linux, Windows and Mac OS X, and 
        like all our code is open-source, licensed under the LGPL.For information
        on the API visit our <a href="http://www.intermine.org/wiki/WebService">wiki pages</a>.
      </p>
      <p>
        For more detailed documentation please see here: 
        <a href="http://www.intermine.org/docs/ruby-docs">http://www.intermine.org/docs/ruby-docs</a>
      </p>

    </div>
  </li>
  <li>
    <div onclick="javascript:showText('examples')"><h3 style="font-weight: bold;">Examples of using the downloaded script</h3></div>
    <div id="examples" style="padding: 5px">

    <p>
	    On each Template Query page and the QueryBuilder there is a link to get Ruby code to run 
	    that particular query using the web service API. Just click the link, save the generated 
	    Ruby script in a file and execute it. You can use the generated code as a starting point 
	    for your own programs.
    </p>
    <p style="padding-top:3px; padding-bottom:3px;">
	    You can run the downloaded script by running the following
	    command in a shell:
	    <br>
	    <pre>&gt; ruby path/to/downloaded/script.rb</pre>
	    <br>
	    If you get an error saying 
	    <code>LoadError: no such file to load -- "intermine/service"</code> or similar,
	    then see <a href="#prerequisite">Prerequisites</a> above.
	
	    Feel free to edit the script - these are designed to be
	    spring-boards to help you get where you want to get to. 
	    For further documentation, please read the general introduction 
	    at <a href="http://www.intermine.org/wiki/RubyClient" target="_blank">http://www.intermine.org/wiki/RubyClient</a>
        and the detailed API documentation at <a href="http://www.intermine.org/docs/ruby-docs/" target="_blank">www.intermine.org/docs/ruby-docs/</a>
    </p>
    <p>
	    For a good reference to getting started in Ruby, visit
	    <a href="http://www.ruby-lang.org/" target="_blank">The official Ruby site</a>, or
	    <a href="http://pine.fm/LearnToProgram/" target="_blank">Learn to Program</a>.
    </p>

    </div>
  </li>
</ul>

</div>

</im:boxarea>
<!-- /apiRuby.jsp -->
