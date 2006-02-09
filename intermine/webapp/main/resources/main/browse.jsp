<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- browse.jsp -->
<tiles:importAttribute name="prompt" ignore="false"/>
<tiles:importAttribute name="templateName" ignore="false"/>
<tiles:importAttribute name="browseOperator" ignore="false"/>

<html:form action="/browseAction">
  ${prompt}:&nbsp;
  <html:hidden property="attributeOps(1)" value="${browseOperator}"/>
  <html:text property="attributeValues(1)"/>
  <input type="hidden" name="templateType" value="global"/>
  <input type="hidden" name="templateName" value="${templateName}"/>
  <input type="hidden" name="skipBuilder" value="1"/>
  <input type="hidden" name="noSaveQuery" value="1"/>
  <html:submit><fmt:message key="begin.input.submit"/></html:submit>
</html:form>
<!-- /browse.jsp -->
