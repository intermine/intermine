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

//]]>-->
</script>

<im:boxarea titleKey="api.java.titleKey" stylename="gradientbox" minWidth="800px" htmlId="apiJava">
  <form id="apiJavaForm" action="fileDownload.do" method="post">
    <input type="hidden" value="${path}" name="path" />
    <input type="hidden" value="${fileName}" name="fileName" />
  </form>

This is the Java API page...
<br>
<a href="javascript: jQuery('#apiJavaForm').submit();">Download WS Client jar</a>

</im:boxarea>
<!-- /apiPerl.jsp -->