<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- mymine.jsp -->
<link rel="stylesheet" type="text/css" href="css/mymine.css"/>

<c:set var="loggedin" value="${!empty PROFILE_MANAGER && !empty PROFILE.username}"/>
<jsp:include page="mymineMenu.jsp" flush="true">
    <jsp:param name="loggedin" value="${loggedin}"/>
    <jsp:param name="MYMINE_PAGE" value="${MYMINE_PAGE}" />
</jsp:include>
    
<div class="body">


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

 
<c:choose>
  <%-- bags --%>
  <c:when test="${MYMINE_PAGE=='lists'||MYMINE_PAGE==null}">
    <tiles:insert name="historyBagView.jsp">
	  <tiles:put name="type" value="bag"/>
	</tiles:insert>
  </c:when>
    
  <%-- favourite bags --%>
  <c:when test="${MYMINE_PAGE=='favouriteLists'}">
    <tiles:insert name="historyBagView.jsp">
	  <tiles:put name="type" value="favourite"/>
	</tiles:insert>
  </c:when>
    
    
    <%-- saved queries --%>
    <c:when test="${MYMINE_PAGE=='saved'}">
      <tiles:insert name="historyQueryView.jsp">
        <tiles:put name="type" value="saved"/>
      </tiles:insert>
    </c:when>
    
    
    <%-- query history --%>
    <c:when test="${MYMINE_PAGE=='history'}">
      <tiles:insert name="historyQueryView.jsp">
        <tiles:put name="type" value="history"/>
      </tiles:insert>
    </c:when>
    
    <%-- saved templates --%>    
    <c:when test="${MYMINE_PAGE=='templates'}">
      <tiles:insert name="historyTemplateView.jsp">
        <tiles:put name="type" value="template"/>
      </tiles:insert>
    </c:when>
    
    <%-- favourites --%> 
    <c:when test="${MYMINE_PAGE=='favourites'}">
      <tiles:insert name="favourites.tile" />
    </c:when>
    
    <%-- password --%> 
    <c:when test="${MYMINE_PAGE=='password'}">
      <tiles:insert name="changePassword.jsp" />
    </c:when>
  </c:choose>


  <%-- login msg --%>
  <c:if test="${empty PROFILE.username}">
    <div class="body">
      <html:link action="/login"><fmt:message key="menu.login"/>...</html:link>&nbsp;&nbsp;
      <fmt:message key="mymine.login.help"/>
    </div>
  </c:if>

  <%-- tag actions for super user --%>
  <div class="body">
    <c:if test="${IS_SUPERUSER}">
      <span class="smallnote">
          <html:link action="/exportTags">
            <fmt:message key="history.exportTags"/>
          </html:link>
      </span><br/>
      <span class="smallnote">
          <html:link action="/tagImport">
            <fmt:message key="history.importTags"/>
          </html:link>
      </span>
    </c:if>
  </div>

</div>

<script type="text/javascript">
  <!--//<![CDATA[
   var deleteButton = document.getElementById('delete_button');
   var removeButton = document.getElementById('remove_button');
   var exportButton = document.getElementById('export_button');
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
       if (exportButton != null) {
           exportButton.disabled = !checked;
       }
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
       if (exportButton != null) {
           exportButton.disabled = deleteDisable;
       }
       document.getElementById('selected_' + type).checked = columnCheckBoxChecked;
       return true;
   }
   //]]>-->
</script>

<!-- /mymine.jsp -->
