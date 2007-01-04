<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute name="message" ignore="false"/>
<tiles:importAttribute name="resultElementMap" ignore="false"/>

<fmt:message key="bagUploadConfirm.duplicatesHeader">
  <fmt:param value="${message}"/>
</fmt:message>

<table class="collection">
  <c:forEach var="resultElementEntry" items="${resultElementMap}">
    <c:set var="identifier" value="${resultElementEntry.key}"/>
    <c:set var="resultElementList" value="${resultElementEntry.value}"/>

    <c:forEach var="resultElement" items="${resultElementList}" varStatus="status">
        <c:set var="rowClass">
          <c:choose>
            <c:when test="${status.count % 2 == 1}">odd</c:when>
            <c:otherwise>even</c:otherwise>
          </c:choose>
        </c:set>

      <tr class="${rowClass}"/>
        <c:if test="${status.index == 0}">
          <td border="1" rowSpan="${fn:length(resultElementList)}" valign="top">${identifier}</td>
        </c:if>

        <td>
          <c:set var="resultElement" value="${resultElement}" scope="request"/>
          <tiles:insert name="objectView.tile" />
        </td>
        <td>
          <html:multibox property="selectedObjects"
                         styleId="selectedObject_${status.index}">
            ${resultElement.id}
          </html:multibox>
        </td>
      </tr>
    </c:forEach>
  </c:forEach>
</table>
