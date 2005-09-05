<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<style>

pre.wiki, pre.literal-block {
 background: #f7f7f7;
 border: 1px solid #d7d7d7;
 padding: .25em;
 overflow: auto;
}

</style>

<!-- error.jsp -->
<html:xhtml/>

<im:box titleKey="error.title">

<div class="body"><b><fmt:message key="error.stacktrace"/></b></div>

<div class="body">
<pre class="wiki">
  <c:out value="${stacktrace}"/>
 </pre>
</div>

</im:box>

<!-- /error.jsp =-->