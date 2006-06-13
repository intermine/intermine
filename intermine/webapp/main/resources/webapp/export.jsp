<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- export.jsp -->
<html:xhtml/>

<div class="heading">
  <fmt:message key="export.export"/><im:helplink key="results.help.export"/>
</div>
<div class="body">
  <ul>
    <li>
      <html:link action="/exportAction?table=${param.table}&amp;type=csv">
        <fmt:message key="export.csv"/>
      </html:link>
    </li>
    <li>
      <html:link action="/exportAction?table=${param.table}&amp;type=tab">
        <fmt:message key="export.tabdelimited"/>
      </html:link>
    </li>
    <c:if test="${resultsTable.size <= WEB_PROPERTIES['max.excel.export.size']}">
      <li>
        <html:link action="/exportAction?table=${param.table}&amp;type=excel">
          <fmt:message key="export.excel">
            <fmt:param value="${WEB_PROPERTIES['max.excel.export.size']}"/>
          </fmt:message>
        </html:link>
      </li>
    </c:if>

    <fmt:setBundle basename="model"/>

    <c:forEach var="entry" items="${exporters}" varStatus="status">
      <li>
        <html:link action="${entry.value.actionPath}&amp;table=${param.table}">
          <fmt:message key="exporter.${entry.key}.description"/>
        </html:link>
      </li>
    </c:forEach>

  </ul>
</div>
<!-- /export.jsp -->
