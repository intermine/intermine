<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- history.jsp -->

<tiles:insert name="historyQueryView.jsp">
  <tiles:put name="type" value="history"/>
</tiles:insert>

<script type="text/javascript">
  <!--//<![CDATA[
    var deleteButton = document.getElementById('delete_button');
    var removeButton = document.getElementById('remove_button'); 
    function selectColumnCheckbox(form, type) {
      var columnCheckBox = 'selected_' + type;
      var testString = columnCheckBox + '_';
      var checked = document.getElementById(columnCheckBox).checked;
      if (deleteButton != null) {
        deleteButton.disabled = !checked;
      }
      if (removeButton != null) {
        removeButton.disabled = !checked;
      }
      document.getElementById('export_button').disabled = !checked;
      with(form) {
        for(var i=0;i < elements.length;i++) {
          var thiselm = elements[i];
          if(thiselm.id.indexOf(testString) != -1)
            thiselm.checked = checked;
        }
      }
    }
    function setDeleteDisabledness(form, type) {
      var checkBoxPrefix = 'selected_' + type + '_';
      var deleteDisable = true;
      var columnCheckBoxChecked = true;
      with(form) {
        for(var i=0;i < elements.length;i++) {
          var thiselm = elements[i];
          if (thiselm.id.indexOf(checkBoxPrefix) != -1) {
            if (thiselm.checked) {
              deleteDisable = false;
            } else {
              columnCheckBoxChecked = false;
            }               
          }
        }
      }
      if (deleteButton != null) {
        deleteButton.disabled = deleteDisable;
      }
      if (removeButton != null) {
        removeButton.disabled = deleteDisable;
      }
      document.getElementById('export_button').disabled = deleteDisable;
      document.getElementById('selected_' + type).checked = columnCheckBoxChecked;
      return true;
    }
    //]]>-->
</script>

<!-- /history.jsp -->