<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- webSearchableList.jsp -->

<tiles:importAttribute name="type"/>
<tiles:importAttribute name="scope"/>

<html:xhtml/>
<c:forEach items="${filteredWebSearchables}" var="webSearchableEntry">
  <div class="webSearchableList">
    <html:link action="/gotows?type=${type}&amp;scope=${scope}&amp;name=${webSearchableEntry.key}">
      ${webSearchableEntry.value.title}
    </html:link>
  </div>
</c:forEach>

<!-- /webSearchableList.jsp -->
