<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%--/**
     * Render a results cell
     * The following parameters must be set:
     * fields: a List of fields to render
     * icons: a List of icons to display
     */
--%>

<tiles:importAttribute scope="request"/>

<c:choose>
  <c:when test="${cld != null}">
  <%-- Go through all the items in the WebConfig for this object --%>
  <%-- For the moment, only do the primary keys --%>
  <table><tr>
    <td><tiles:insert name="/pkFields.jsp" /></td>
  </tr></table>

  </c:when>
  <c:otherwise>
    <font class="resultsCellValue">
      <c:out value="${object}"/>
    </font>
  </c:otherwise>
</c:choose>

