<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute name="message" ignore="false"/>
<tiles:importAttribute name="classDuplicates" ignore="false"/>

<fmt:message key="bagUploadConfirm.duplicatesHeader">
  <fmt:param value="${message}"/>
</fmt:message>

<c:forEach var="duplicatesEntry" items="${classDuplicates}">
  <c:set var="className" value="${duplicatesEntry.key}"/>
  <c:set var="objectMap" value="${duplicatesEntry.value}"/>

  <p>
    ${className}
  </p>

  <table>

  <c:forEach var="entry" items="${objectMap}">
    <c:set var="identifier" value="${entry.key}"/>
    <c:set var="objects" value="${entry.value}"/>

    <c:forEach var="object" items="${objects}" varStatus="status">
      <tr>
        <c:if test="${status.index == 0}">
          <td rowSpan="${fn:length(objects)}" valign="top">${identifier}</td>
        </c:if>
        <td>
          <c:set var="resultElement" value="${resultElement}" scope="request"/>
          <tiles:insert name="objectView.tile" />
        </td>
      </tr>
    </c:forEach>
  </c:forEach>
  </table>
</c:forEach>
