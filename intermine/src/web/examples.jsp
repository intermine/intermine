<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- examples.jsp -->
<fmt:setBundle basename="model"/>

<div class="exampleQueries">
  <c:if test="${!empty EXAMPLE_QUERIES}">
    <c:forEach items="${EXAMPLE_QUERIES}" var="exampleQuery">
      <div class="exampleQuery">
        <span class="title">
          <fmt:message key="exampleQuery.${exampleQuery.key}.description"/>
        </span>
        <fmt:message key="exampleQuery.${exampleQuery.key}.prefix" var="prefix"/>
        <c:if test="${!empty prefix}">
          <span class="docLink">
            [<html:link href="/${prefix}/${exampleQuery.key}.html">
            tutorial
            </html:link>]
          </span>
        </c:if>
        <span class="link">
          <html:link action="/loadExampleQuery?method=load&name=${exampleQuery.key}">
            <img class="arrow" src="images/right-arrow.gif" alt="->"/>
          </html:link>
        </span>
      </div>
      <br/>
    </c:forEach>
  </c:if>
</div>
<!-- /examples.jsp -->
