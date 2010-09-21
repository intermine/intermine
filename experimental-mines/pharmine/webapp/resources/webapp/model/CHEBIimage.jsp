<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- imageDisplayer.jsp -->
<c:if test="${!empty object.cHEBIAnnotations}">
  <c:forEach items="${object.cHEBIAnnotations}" var="thisAnnot">
    <html:img src="http://www.ebi.ac.uk/chebi/displayImage.do?defaultImage=true&imageIndex=0&chebiId=${thisAnnot.identifier}"/>
  </c:forEach>
</c:if>
<!-- /imageDisplayer.jsp -->
