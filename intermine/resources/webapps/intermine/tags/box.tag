<%@ tag body-content="scriptless"  %>
<%@ attribute name="title" required="false" %>
<%@ attribute name="titleKey" required="false" %>
<%@ attribute name="helpUrl" required="false" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:if test="${!empty titleKey}">
  <fmt:message key="${titleKey}" var="title"/>
</c:if>

<c:if test="${!empty title}">
  <table class="box" cellspacing="0" cellpadding="6" border="0" width="100%" align="center">
    <tr>
      <th class="title" align="left">
        ${title}
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
</c:if>

<%-- or just process body if no title --%>
<c:if test="${empty title}">
  <jsp:doBody/>
</c:if>