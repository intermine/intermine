<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- objectTrail.jsp -->

<c:if test="${!empty trailElements}">
  <div class="body objectTrail">
    Trail: <c:forEach items="${trailElements}" var="item" varStatus="status">
      <html:link action="/objectDetails?id=${item.objectId}&trail=${item.trail}"
                 styleClass="objectTrailLink">${item.label}</html:link>
      <c:if test="${!status.last}">
        &gt;
      </c:if>
    </c:forEach>
  </div>
</c:if>

<!-- /objectTrail.jsp -->

