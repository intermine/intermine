<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>

<!-- wsBagHeader.jsp -->
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
    <input type="checkbox" id="selected_bag"
           onclick="selectColumnCheckbox(this.form, 'bag')">
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
