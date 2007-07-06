<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- browse.jsp -->
<%--<tiles:importAttribute name="prompt" ignore="false"/>
<tiles:importAttribute name="templateName" ignore="false"/>
<tiles:importAttribute name="browseOperator" ignore="false"/>
--%>

<tiles:importAttribute name="menuItem" ignore="true"/>

<%--<c:choose>
  <c:when test="${empty menuItem}">
<html:form action="/browseAction" styleClass="browseForm">
    ${WEB_PROPERTIES['begin.browse.prompt']}:&nbsp;
  <html:hidden property="attributeOps(1)" value="6"/>
  <html:text property="attributeValues(1)"/>
  <input type="hidden" name="templateType" value="global"/>
  <input type="hidden" name="templateName" value="${WEB_PROPERTIES['begin.browse.template']}"/>
  <input type="hidden" name="skipBuilder" value="1"/>
  <input type="hidden" name="noSaveQuery" value="1"/>
  <html:submit><fmt:message key="begin.input.submit"/></html:submit>
</html:form>
</c:when>
  <c:otherwise>--%>
<html:form action="/browseAction" style="display:inline;">
  <fmt:message key="header.search.pre"/>
  <select name="quickSearchType">
	<option value="ids">Identifiers</option>
	<option value="tpls">Templates</option>
	<option value="bgs">Bags</option>
  </select>
  <fmt:message key="header.search.mid"/>
  <html:text property="value" size="40"/>
  <html:submit><fmt:message key="header.search.button"/></html:submit>
</html:form>
<%--  </c:otherwise>	 
</c:choose>--%>

<!-- /browse.jsp -->
