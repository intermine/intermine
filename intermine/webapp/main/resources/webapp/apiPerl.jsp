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
Web Service Perl API makes it easy for users to query data from a Perl program.
Users can send query request from the program and get the results as tab-separated values.
</div>
<br>
<div>


<ul>
  <li>
    <div onclick="javascript:showText('prerequisite')"><span class="fakelink">Prerequisites</span></div>
    <div id="prerequisite"  style="padding: 5px">

    A user must install the <a href="http://search.cpan.org/%7Eintermine/Webservice-InterMine/lib/Webservice/InterMine.pm">
    Webservice::InterMine </a> module first.

    </div>
  </li>
  <li>
    <div onclick="javascript:showText('codegen')"><span class="fakelink">Start to use</span></div>
    <div id="codegen"  style="padding: 5px">
      <span>Please check the 
      <a href="http://search.cpan.org/~intermine/Webservice-InterMine-0.9405/lib/Webservice/InterMine/Cookbook.pod">
      Webservice::InterMine cookbook</a> for a set of short tutorial 'recipes' 
      which aim to demonstrate particular features of the Perl API. 
      <br>Each recipe presents some code, followed by a section which explains and discusses the features used.
</span>
    </div>
  </li>
</ul>
</div>




</im:boxarea>
<!-- /apiPerl.jsp -->