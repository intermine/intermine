<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>

<%-- Page that will take a request parameter of a file to transform and then --%>
<%-- transform it through the InterMine XSL --%>

<!-- docbook.jsp -->
<c:choose>
  <c:when test="${empty param.file}">
    <fmt:message key="documentation.error"/>
  </c:when>
  <c:otherwise>
    <c:import url='${param["file"]}' var="xml" />
    <c:import url="/WEB-INF/xslt/flymine.xsl" var="xslt" />
    <x:transform xml="${xml}" xslt="${xslt}" />
  </c:otherwise>
</c:choose>
<!-- /docbook.jsp -->
