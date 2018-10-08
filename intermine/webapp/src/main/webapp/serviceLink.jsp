<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<%-- Page displaying link to webservice. --%>

<style type="text/css">

.html-snippet {
    background-color:white;
    border:none;
    padding:0px;
    margin:0px;
    width:100%;
}

.term {
    background-color: #E0E0E0;
}

p.apology {
    border: 1px solid #E0E0E0;
    padding: 10px;
    margin: 10px;
}

</style>

<script type="text/javascript">

  Syntax.root = "<html:rewrite page='/js/jquery-syntax/'/>";
  jQuery(function() {jQuery('.html-snippet').syntax({
    brush: 'html',
    layout: 'list',
    replace: 'true',
    tabwidth: 4
  }, function(options, html, container) {jQuery(container).empty().append(html)})});

</script>

<!-- serviceLink.jsp -->
<div align="center" ><div class="plainbox" style="width:900px; font-size:14px; overflow: auto">

  <c:choose>
    <c:when test="${pageTitle != null}">
        <c:set var="pageTitle" value="${pageTitle}"></c:set>
    </c:when>
    <c:otherwise>
        <c:set var="pageTitle" value="Resource link"></c:set>
    </c:otherwise>
  </c:choose>

  <h1>Embedding Template Results In Your Own Page</h1>

      <div  style="margin-top: 10px;">
           <p>
           Below you can see an example embedded table displaying the results from this
           template. As soon as the page loads, results are fetched from the server and
           formatted in the browser, according to your CSS.
           </p>
      </div>
      <div id="table-example">${realCode}</div>
      <div>
          To achieve this affect, simply copy and paste the following html into your page. To adjust the
          parameters of the template, just change the <code class="term">op</code>s and <code class="term">value</code>s in the call
          to <code class="term">loadTemplate</code>.
          <br><br>
          You can find full documentation about how to embed results and style the resultant
          tables at <a href="http://www.intermine.org/wiki/JavaScriptClient">intermine.org/wiki/JavaScriptClient</a>.
      </div>

      <pre class="html-snippet">${jsCode}</pre>

  <span style="float:left;">For other options and detailed help <a href="http://intermine.org/wiki/JavaScriptClient">click here</a>.</span>
    <span style="float:right;"><a href="javascript:history.go(-1)">Go back</a></span>

</div></div>
<!-- /serviceLink.jsp -->

