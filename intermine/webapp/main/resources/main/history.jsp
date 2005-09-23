<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- history.jsp -->

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
    //]]>-->
</script>

<div class="body">
  <fmt:message key="history.intro"/>
</div>
<br/>

    <tiles:get name="historyBagView"/>
    <c:if test="${!empty PROFILE.username}">
      <tiles:insert name="historyQueryView">
        <tiles:put name="type" value="saved"/>
      </tiles:insert>
    </c:if>
    <tiles:insert name="historyQueryView">
      <tiles:put name="type" value="history"/>
    </tiles:insert>
    
<tiles:get name="historyTemplateView.jsp"/>
  
<!-- /history.jsp -->
