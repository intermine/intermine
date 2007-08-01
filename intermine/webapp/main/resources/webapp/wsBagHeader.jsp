<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute name="wsListId"/>
<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>

<!-- wsBagHeader.jsp -->
<c:if test="${!empty makeCheckBoxes}">
  <th>
    <input type="checkbox" id="selected_${wsListId}_bag"
           onclick="selectColumnCheckbox(this.form, 'bag', '${wsListId}')">
  </th>
</c:if>
<c:choose>
  <c:when test="${IS_SUPERUSER}">
    <th align="left" colspan="3" nowrap="true">
  </c:when>
  <c:otherwise>
    <th align="left" colspan="2" nowrap="true">
  </c:otherwise>
</c:choose>
<fmt:message key="query.savedbags.namecolumnheader"/>
</th>
<th nowrap="true">
  <fmt:message key="query.savedbags.typecolumnheader"/>
</th>
<c:if test="${showDescriptions}">
  <th nowrap="true">
    <fmt:message key="query.savedbags.descriptioncolumnheader"/>
  </th>
</c:if>
<th nowrap="true">
  <fmt:message key="query.savedbags.datecreatedcolumnheader"/>
</th>
<th align="right" nowrap="true">
  <fmt:message key="query.savedbags.countcolumnheader"/>
</th>

<!-- /wsBagHeader.jsp -->
