<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- mymine.jsp -->
<c:set var="loggedin" value="${!empty PROFILE_MANAGER && !empty PROFILE.username}"/>
  
  
<div class="body">

<%-- top nav --%>
<jsp:include page="mymineMenu.jsp" flush="true">
    <jsp:param name="loggedin" value="${loggedin}"/>
    <jsp:param name="MYMINE_PAGE" value="${MYMINE_PAGE}" />
</jsp:include>



  <c:choose>
  <%-- bags --%>
    <c:when test="${MYMINE_PAGE=='lists'||MYMINE_PAGE==null}">
    
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
    <tiles:put name="scope" value="user"/>
    <tiles:put name="makeCheckBoxes" value="true"/>
    <tiles:put name="showDescriptions" value="true"/>
  </tiles:insert>

  <br/>

  <fmt:message key="history.savedbags.newbag"/>
  <html:text property="newBagName" size="12"/><br/>
  <html:submit property="union">
    <fmt:message key="history.union"/>
  </html:submit>
  <html:submit property="intersect">
    <fmt:message key="history.intersect"/>
  </html:submit>
  <html:submit property="subtract">
    <fmt:message key="history.subtract"/>
  </html:submit>
</html:form>

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



<!-- /mymine.jsp -->
