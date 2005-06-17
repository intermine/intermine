
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- dataSetIcons -->

<div class="dataSetIcons">
  <c:forEach var="entry" items="${DATASETS}" varStatus="status">
  	<c:set var="set" value="${entry.value}"/>
    <div class="dsIconsElement">
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
    </div>
  </c:forEach>
</div>

<!-- /dataSetIcons -->