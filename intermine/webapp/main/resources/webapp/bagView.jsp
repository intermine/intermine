<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<html:xhtml/>

<!-- bagView.jsp -->
<script type="text/javascript">
<!--//<![CDATA[
  function selectColumnCheckbox(form, type, scope) {
    var checkBoxId = 'selected_' + scope + '_' + type;
    var checked = document.getElementById(checkBoxId).checked;
    with(form) {
      for(i=0;i < elements.length;i++) {
        thiselm = elements[i];
        var testString = checkBoxId + '_';
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

<html:form action="/modifyBag">
  <tiles:insert name="wsBagTable.tile">
    <tiles:put name="wsListId" value="all_bag"/>
    <tiles:put name="scope" value="all"/>
    <tiles:put name="makeCheckBoxes" value="true"/>
    <tiles:put name="showDescriptions" value="true"/>
    <tiles:put name="showTags" value="true"/>
  </tiles:insert>

</html:form>
<!-- /bagView.jsp -->
