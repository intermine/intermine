<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- objectTrail.jsp -->

<im:body>
  <div class="objectTrail">
    <c:forEach items="${trailElements}" var="item" varStatus="status">
      <html:link action="/objectDetails?id=${item.objectId}&trail=${item.trail}"
                 styleClass="objectTrailLink">${item.label}</html:link>
      <c:if test="${!status.last}">
        >
      </c:if>
    </c:forEach>
  </div>
</im:body>

<!-- /objectTrail.jsp -->

