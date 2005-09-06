<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- templateSettings.jsp -->

<html:xhtml/>

<im:heading id="templateSettings">
  <c:choose>
    <c:when test="${!empty EDITING_TEMPLATE}">
      <fmt:message key="templateBuilder.editingTemplate">
        <fmt:param value="${EDITING_TEMPLATE.name}"/>
      </fmt:message>
    </c:when>
    <c:otherwise>
      Building a new template query
    </c:otherwise>
  </c:choose>
</im:heading>

<im:body id="templateSettings">
  <html:form action="/buildTemplate">
    <table border="0" width="100%">
      <tr>
        <td width="1%" align="right"><fmt:message key="templateBuilder.shortName"/></td>
        <td colspan="2">
          <html:text property="shortName" size="32"/>
          <c:if test="${IS_SUPERUSER}">
            &nbsp;&nbsp;
            <fmt:message key="templateBuilder.important"/><html:checkbox property="important"/>
          </c:if>
        </td>
      </tr>
      <tr>
        <td align="right"><fmt:message key="templateBuilder.templateDescription"/></td>
        <td><html:text property="description" size="55"/></td>
        <td>&nbsp;</td>
      </tr>
      <tr>
        <td align="right"><fmt:message key="templateBuilder.keywords"/></td>
        <td><html:text property="description" size="45"/></td>
        <td width="99%">
          <html:reset/>
          <html:submit>Update</html:submit>
        </td>
      </tr>
    </table>
  </html:form>
</im:body>

<!-- /templateSettings.jsp -->
