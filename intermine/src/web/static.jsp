<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jstl/xml" %>

<%-- Page that will take a request parameter of a file to transform and then include it --%>

<c:choose>
  <c:when test="${empty param.file}">
    <bean:message key="documentation.error"/>
  </c:when>
  <c:otherwise>
    <c:import url="${param.file}"/>
  </c:otherwise>

</c:choose>

