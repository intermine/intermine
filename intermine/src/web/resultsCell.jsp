<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%--/**
     * Render a results cell
     */
--%>

<tiles:importAttribute scope="request"/>

<c:choose>
  <c:when test="${clds != null}">
  <%-- Go through all the items in the WebConfig for this object --%>
  <table><tr>
    <td align="left">
      <html:link action="/changeResults?method=details&rowIndex=${rowIndex}&columnIndex=${columnIndex}">
        <font class="resultsCellTitle">
          <c:forEach var="cld" items="${clds}">
            <c:out value="${cld.unqualifiedName}"/>
          </c:forEach>
        </font>
      </html:link>
    </td>
  </tr>
  <tr>
    <c:forEach var="cld" items="${clds}">
      <c:if test="${empty webconfig.types[cld.name].shortDisplayers}">
        <td><tiles:insert name="/pkFields.jsp" /></td>
      </c:if>
      <c:forEach items="${webconfig.types[cld.name].shortDisplayers}" var="displayer">
        <td><tiles:insert beanName="displayer" beanProperty="src"/></td>
      </c:forEach>
    </c:forEach>
  </tr></table>

  </c:when>
  <c:otherwise>
    <font class="resultsCellValue">
      <c:out value="${object}"/>
    </font>
  </c:otherwise>
</c:choose>

