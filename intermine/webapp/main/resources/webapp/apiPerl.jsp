<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<html:xhtml/>

<!-- apiPerl.jsp -->
<script type="text/javascript">
<!--//<![CDATA[
jQuery(document).ready(function() {
    // jQuery("p").hide();
 });

 function showText(pid) {
   jQuery("#" + pid).slideToggle("slow");
 }

//]]>-->
</script>

<im:boxarea titleKey="api.perl.titleKey" stylename="gradientbox" minWidth="800px" htmlId="apiPerl">


<div>
The Perl web service API makes it easy to run queries in <c:out value="${WEB_PROPERTIES['project.title']}"/> directly from Perl programs.
You can use the Perl API to construct any query you could run from web interface and fetch the results as tab-separated values.
</div>
<br>
<div>


<ul>
  <li>
    <div onclick="javascript:showText('prerequisite')"><span class="fakelink">Prerequisites</span></div>
    <div id="prerequisite"  style="padding: 5px">

    You should install the <a href="http://search.cpan.org/%7Eintermine/Webservice-InterMine/lib/Webservice/InterMine.pm">
    Webservice::InterMine </a> module to get started.

    </div>
  </li>
  <li>
    <div onclick="javascript:showText('codegen')"><span class="fakelink">Start to use</span></div>
    <div id="codegen"  style="padding: 5px">
      <span>Please check the
      <a href="http://search.cpan.org/~intermine/Webservice-InterMine-0.9405/lib/Webservice/InterMine/Cookbook.pod">
      Webservice::InterMine cookbook</a> for a set of short tutorial 'recipes'
      which demonstrate particular features of the Perl API.
      <br>Each recipe presents some code, followed by a section which explains and discusses the features used.
</span>
    </div>
  </li>
  <li>
    <div onclick="javascript:showText('examples')"><span class="fakelink">Examples</span></div>
    <div id="examples"  style="padding: 5px">

    On each Template Query page and the QueryBuilder there is a link to get Perl code to run that particular
    query using the web service API. Just click the link, save the generated Perl in a file and execute it.
    You can use the generated code as a starting point for your own programs.

    </div>
  </li>
</ul>
</div>




</im:boxarea>
<!-- /apiPerl.jsp -->