<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- export.jsp -->
<div>
  <div>
    <html:link action="/exportAction?type=excel">
      <fmt:message key="export.excel"/>
    </html:link>
  </div>
  <div>
    <html:link action="/exportAction?type=csv">
      <fmt:message key="export.csv"/>
    </html:link>
  </div>
  <div>
    <html:link action="/exportAction?type=tab">
      <fmt:message key="export.tabdelimited"/>
    </html:link>
  </div>

  <fmt:setBundle basename="model"/>

  <c:forEach var="entry" items="${exporters}" varStatus="status">
    <c:set var="exporterMessageId" value="exporter.${entry.key}.description"/>
    <html:link action="${entry.value.actionPath}">
      <fmt:message key="${exporterMessageId}"/>
    </html:link>
  </c:forEach>
</div>
<!-- /export.jsp -->
