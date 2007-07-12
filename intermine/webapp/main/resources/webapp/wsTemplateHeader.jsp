<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>
<tiles:importAttribute name="wsColumnCheckBoxId" ignore="true"/>
<tiles:importAttribute name="showNames" ignore="true"/>
<tiles:importAttribute name="showTitles" ignore="true"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>

<!-- wsTemplateHeader.jsp -->

<c:if test="${!empty makeCheckBoxes}">
  <th>
    <input type="checkbox" id="selected_${scope}_template"
           onclick="selectColumnCheckbox(this.form, 'template', '${scope}')"/>
  </th>
</c:if>
<c:if test="${showNames}">
  <th align="left" nowrap>
    <fmt:message key="history.namecolumnheader"/>
  </th>
</c:if>
<c:if test="${showTitles}">
  <th align="left" nowrap>
    <fmt:message key="history.titleheader"/>
  </th>
</c:if>
<c:if test="${showDescriptions}">
  <th align="left" nowrap>
    <fmt:message key="history.descriptionheader"/>
  </th>
  <th align="left" nowrap>
    <fmt:message key="history.commentheader"/>
  </th>
</c:if>
<th align="center" nowrap>
  <fmt:message key="history.actionscolumnheader"/>
</th>

<!-- /wsTemplateHeader.jsp -->
