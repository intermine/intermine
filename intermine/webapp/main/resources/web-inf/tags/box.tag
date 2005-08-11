<%@ tag body-content="scriptless"  %>
<%@ attribute name="title" required="false" %>
<%@ attribute name="titleKey" required="false" %>
<%@ attribute name="helpUrl" required="false" %>
<%@ attribute name="topRightTile" required="false" %>
<%@ attribute name="topLeftTile" required="false" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:if test="${!empty titleKey}">
  <fmt:message key="${titleKey}" var="title"/>
</c:if>

<c:if test="${!empty title || !empty topRightTile || !empty topLeftTile}">
<div style="width:100%">
  <table class="box" width="100%" cellspacing="0" cellpadding="0" border="0" align="center">
    <tr>
      <th class="title" align="left">
        <c:if test="${!empty topLeftTile}">
          <tiles:insert name="${topLeftTile}"/>
        </c:if>
        ${title}
      </th>
      <th class="help" align="right" nowrap="nowrap">
        <c:choose>
          <c:when test="${!empty helpUrl}">
            [<html:link href="${helpUrl}">
              <fmt:message key="begin.link.help"/>
            </html:link>]
          </c:when>
          <c:when test="${!empty topRightTile}">
            <tiles:insert name="${topRightTile}"/>
          </c:when>
          <c:otherwise>
          	&nbsp;
          </c:otherwise>
        </c:choose>
      </th>
    </tr>
    <tr>
      <td valign="top" align="left" colspan="2" class="boxbody">
        <jsp:doBody/>
      </td>
    </tr>
  </table>
</div>
</c:if>

<%-- or just process body if no title --%>
<c:if test="${empty title && empty topRightTile && empty topLeftTile}">
  <jsp:doBody/>
</c:if>
