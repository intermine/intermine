<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- examples.jsp -->
<html:xhtml/>
<fmt:setBundle basename="model"/>

<div class="body">
  <c:if test="${!empty EXAMPLE_QUERIES}">
    <c:forEach items="${EXAMPLE_QUERIES}" var="exampleQuery" varStatus="status">
      <div class="exampleQuery">
        <span>
          <fmt:message key="exampleQuery.${exampleQuery.key}.description"/>
        </span>
        <fmt:message key="exampleQuery.${exampleQuery.key}.prefix" var="prefix"/>
        <c:if test="${!empty prefix}">
          <span class="docLink">
            [<html:link href="${WEB_PROPERTIES['project.sitePrefix']}/${prefix}/${exampleQuery.key}.shtml">
            tutorial
            </html:link>]
          </span>
        </c:if>
        <span class="link">
          <html:link action="/loadQuery?method=example&amp;name=${exampleQuery.key}">
            <img class="arrow" src="images/right-arrow.gif" title="->"/>
          </html:link>
        </span>
      </div>
      <c:if test="${!status.last}">
        <hr class="seperator"/>
      </c:if>
    </c:forEach>
  </c:if>
</div>
<!-- /examples.jsp -->
