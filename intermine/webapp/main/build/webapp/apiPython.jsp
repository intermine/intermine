<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<html:xhtml/>

<!-- apiPython.jsp -->

<im:boxarea titleKey="api.python.titleKey" stylename="gradientbox" fixedWidth="90%" htmlId="apipython">

<div>
  <p>
    The Python web service API makes it easy to run queries in <c:out value="${WEB_PROPERTIES['project.title']}"/> directly from Python programs.
    You can use the Python API to construct any query you could run from web interface and fetch the results in a number of
    structured formats, including native Python data structures, and TSV and CSV strings.
  </p>
</div>
<br>
<div>

<ul>
  <li>
    <div onclick="javascript:showText('prerequisite')"><h3 style="font-weight: bold;">Prerequisites</h3></div>
    <div id="prerequisite" style="padding: 5px">
      <p>
        You should install the <a href="http://pypi.python.org/pypi/intermine" target="_blank">
        Python webservice client library module</a> to get started.
        You can install it directly from:
        <a href="http://pypi.python.org">PyPi</a> (the Python Package Index). This is a public
        repository of thousands of modules. To install the InterMine python
        client library type the following command into a shell:
        <br>
    <pre>&gt; sudo easy_install intermine</pre>
        <br>
      </p>
      <p>
        This module is pure Python with no external dependencies, so long as your Python is 2.6 or newer.
        If you have 2.5 (which will be the case if you run Mac OS 10.5 - ie. Leopard),
        you will need to run the following command (we also recommend upgrading&nbsp;-&nbsp;see
        <a href="http://www.python.org/download/mac/">here</a>):
        <br>
        <pre>&gt; sudo easy_install simplejson</pre>
        <br>
        The client library has been tested on Python 2.5, 2.6 &amp; 2.7, on Linux, Windows and
    Mac OS X, and like all our code is open-source, licensed under the LGPL.For information
  on the API visit our <a href="http://www.intermine.org/wiki/WebService">wiki pages</a>.
      </p>
      <p>
        For other installation options, and a general guide to usage,
        please see here: <a href="http://www.intermine.org/wiki/PythonClient">http://www.intermine.org/wiki/PythonClient</a>
      </p>

    </div>
  </li>
  <li>
    <div onclick="javascript:showText('examples')"><h3 style="font-weight: bold;">Examples of using the downloaded script</h3></div>
    <div id="examples" style="padding: 5px">

    <p>On each Template Query page and the QueryBuilder there is a link to get Python code to run that particular
    query using the web service API. Just click the link, save the generated Python script in a file and execute it.
    You can use the generated code as a starting point for your own programs.</p>
    <p style="padding-top:3px; padding-bottom:3px;">
    You can run the downloaded script by running the following
    command in a shell:
    <br>
    <pre>&gt; python path/to/downloaded/script.py</pre>
    <br>
    If you get an error saying <code>ImportError: no module named intermine.webservice</code> or similar,
    then see 'Prerequisites' above.

    Feel free to edit the script - these are designed to be
    spring-boards to help you get where you want to. For further documentation, please read the
    general introduction at <a href="http://www.intermine.org/wiki/PythonClient" target="_blank">http://www.intermine.org/wiki/PythonClient</a>
    and the detailed API documentation at <a href="http://www.flymine.org/download/docs/python-docs/" target="_blank">www.flymine.org/download/docs/python-docs/</a>
    </p>
    <p>
    For a good reference to getting started in Python, visit
    <a href="http://wiki.python.org/moin/BeginnersGuide" target="_blank">the Python Beginners Guide</a>.
    </p>

    </div>
  </li>
</ul>

</div>

</im:boxarea>
<!-- /apiPython.jsp -->
