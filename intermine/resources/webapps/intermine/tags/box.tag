<%@ tag body-content="scriptless"  %>
<%@ attribute name="title" required="false" %>
<%@ attribute name="titleKey" required="false" %>
<%@ attribute name="helpUrl" required="false" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<table class="box" cellspacing="0" cellpadding="6" border="0" width="100%" align="center">
  <tr>
    <th class="title" align="left">
      <c:choose>
        <c:when test="${!empty title}">
          <c:out value="${title}"/>
        </c:when>
        <c:when test="${!empty titleKey}">
          <fmt:message key="${titleKey}"/>
        </c:when>
      </c:choose>
    </th>
    <th class="help" align="right" nowrap="nowrap">
      <c:if test="${!empty helpUrl}">
        [<html:link href="${helpUrl}">
          <fmt:message key="begin.link.help"/>
        </html:link>]
      </c:if>
    </th>
  </tr>
  <tr>
    <td valign="top" align="left" colspan="2">
      <jsp:doBody/>
    </td>
  </tr>
</table>

