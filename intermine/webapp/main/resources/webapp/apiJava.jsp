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
     jQuery("p").hide();
  });

  function showText(pid) {
    jQuery("#" + pid).slideToggle("slow");
  }

//]]>-->
</script>

<im:boxarea titleKey="api.java.titleKey" stylename="gradientbox" minWidth="800px" htmlId="apiJava">
  <form id="apiJavaForm" action="fileDownload.do" method="post">
    <input type="hidden" value="${path}" name="path" />
    <input type="hidden" value="${fileName}" name="fileName" />
  </form>

This is the Java API page...
<br>
<br>
<div>
  <div>FAQ:</div>
  <ol>
    <li>
      <div onclick="javascript:showText('prerequisite')"><span class="fakelink">What is the prerequisite to use web service API in Java?</span></div>
      <p id="prerequisite">Blablabla...<a href="javascript: jQuery('#apiJavaForm').submit();">Download WS Client jar</a></p>
    </li>
    <li>
      <div onclick="javascript:showText('codegen')"><span class="fakelink">How to use web service code generation?</span></div>
      <p id="codegen">Blablabla...</p>
    </li>
    <li>
      <div onclick="javascript:showText('editsrc')"><span class="fakelink">How to edit the source code to make sure it works?</span></div>
      <p id="editsrc">Blablabla...</p>
    </li>
  </ol>
</div>

</im:boxarea>
<!-- /apiPerl.jsp -->