<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>


<h2><bean:message key="results.title"/></h2>

<table border="1px">
  <!-- Headings -->
  <tr>
  </tr>
  <c:forEach var="resultrow" items="${results}" varStatus="status">
    <tr>
      <c:forEach var="object" items="${resultrow}">
        <td>
          <c:choose>
            <c:when test="${object.class.name == 'java.lang.String'}">
              <c:out value="${object}" />
            </c:when>
            <c:when test="${object.class.name == 'java.lang.Long'}">
              <c:out value="${object}" />
            </c:when>
            <c:otherwise>
            <html:link action="/details" paramId="objectId" paramName="object" paramProperty="id">
                <c:out value="${object}" />
              </html:link>
            </c:otherwise>
          </c:choose>
        </td>
      </c:forEach>
    </tr>         
  </c:forEach>
</table>

<br />

<html:link action="/buildquery"><bean:message key="index.query"/></html:link>
