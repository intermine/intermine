<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- export.jsp -->
<div>
  <%-- disabling excel export as whole file must be built in-memory --%>
  <%--div>
    <html:link action="/exportAction?type=excel">
      <fmt:message key="export.excel"/>
    </html:link--%>
  </div--%>
  <ul>
    <li>
      <html:link action="/exportAction?type=csv">
        <fmt:message key="export.csv"/>
      </html:link>
    </li>
    <li>
      <html:link action="/exportAction?type=tab">
        <fmt:message key="export.tabdelimited"/>
      </html:link>
    </li>

  <fmt:setBundle basename="model"/>

  <c:forEach var="entry" items="${exporters}" varStatus="status">
    <li>
      <html:link action="${entry.value.actionPath}">
        <fmt:message key="exporter.${entry.key}.description"/>
      </html:link>
    </li>
  </c:forEach>

  </ul>
</div>
<!-- /export.jsp -->
