
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- dataSetIcons -->

<div class="dataSetIcons">
  <table>
    <tr valign="top">
      <td class="dsIconsElement">
        <c:forEach var="entry" items="${DATASETS}" varStatus="status">
          <c:set var="set" value="${entry.value}"/>
          <html:link action="/dataSet?name=${set.name}">
            <img src="${set.iconImage}" class="dsIconImage"/>
          </html:link>
          <div class="dsIconLabel">
      	    <html:link action="/dataSet?name=${set.name}">
              ${set.name}
            </html:link>
          </div>
          <div class="dsIconDetail">
            ${set.subTitle}
          </div>

          <c:if test="${fn:length(DATASETS) != status.count}">
            <td/>
            <c:if test="${status.count % 5 == 0}">
              <tr/>
              <tr valign="top">
            </c:if>
            <td class="dsIconsElement">
          </c:if>
        </c:forEach>
      </td>
    </tr>
  </table>
</div>

<!-- /dataSetIcons -->
