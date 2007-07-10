<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>
<tiles:importAttribute name="showNames" ignore="true"/>
<tiles:importAttribute name="showTitles" ignore="true"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>

<!-- wsTemplateHeader.jsp -->
<script type="text/javascript">
<!--//<![CDATA[
  function selectColumnCheckbox(form, type) {
    var columnCheckBox = 'selected_' + type;
    var checked = document.getElementById(columnCheckBox).checked;
    with(form) {
      for(i=0;i < elements.length;i++) {
        thiselm = elements[i];
        var testString = columnCheckBox + '_';
        if(thiselm.id.indexOf(testString) != -1)
          thiselm.checked = checked;
      }
    }
  }
  function noenter() {
    return !(window.event && window.event.keyCode == 13);
  }
//]]>-->
</script>

<c:if test="${!empty makeCheckBoxes}">
  <th>
    <input type="checkbox" id="selected_template"
           onclick="selectColumnCheckbox(this.form, 'template')"/>
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
