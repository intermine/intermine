<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- mymine.jsp -->

  <div style="border-top: 9px solid white">
  
  <c:set var="loggedin" value="${!empty PROFILE_MANAGER && !empty PROFILE.username}"/>

  <c:if test="${loggedin}">
    <c:set var="loggedOutStyle" value=""/>
  </c:if>
  <c:if test="${!loggedin}">
    <c:set var="loggedOutStyle" value="loggedOutTab"/>
  </c:if>

  <table cellpadding="5" cellspacing="0" border="0" class="histTabTable" width="100%">
    <tr>
      <td width="1%" class="topLeft" nowrap>
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
      </td>
      <td width="1%" class="tab ${(MYMINE_PAGE=='bags'||MYMINE_PAGE == null)?'selected':'not-selected'}"
        nowrap>
        <c:choose>
          <c:when test="${MYMINE_PAGE=='bags'||MYMINE_PAGE == null}">
            <fmt:message key="mymine.bags.tab.title"/>
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=bags">
              <fmt:message key="mymine.bags.tab.title"/>
            </html:link>
          </c:otherwise>
        </c:choose>
        <im:manualLink section="manualMyMineTabs.shtml#manualBagsMyMine"/>
      </td>

      <td width="1%" class="tab ${MYMINE_PAGE=='saved'?'selected':'not-selected'}" nowrap="true">
        <c:choose>
          <c:when test="${!loggedin}">
            <span onclick="alert('You need to log in to save queries'); return false;">
              <fmt:message key="mymine.savedqueryies.tab.title"/>
            </span>
          </c:when>
          <c:when test="${MYMINE_PAGE=='saved' || !loggedin}">
            <fmt:message key="mymine.savedqueryies.tab.title"/>
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=saved">
              <fmt:message key="mymine.savedqueryies.tab.title"/>
            </html:link>
          </c:otherwise>
        </c:choose>
        <im:manualLink section="manualMyMineTabs.shtml#manualSavedQueries"/>
       </td>

      <td width="1%" class="tab ${MYMINE_PAGE=='templates'?'selected':'not-selected'}" nowrap="true">

        <c:choose>
          <c:when test="${!loggedin}">
            <span onclick="alert('You need to log in to save templates'); return false;">
              <fmt:message key="mymine.savedtemplates.tab.title"/>
            </span>
          </c:when>
          <c:when test="${MYMINE_PAGE=='templates' || !loggedin}">
            <fmt:message key="mymine.savedtemplates.tab.title"/>
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=templates">
              <fmt:message key="mymine.savedtemplates.tab.title"/>
            </html:link>
          </c:otherwise>
        </c:choose>
       <im:manualLink section="manualMyMineTabs.shtml#manualSavedTemplates"/>
       </td>

      <td width="1%" class="tab ${MYMINE_PAGE=='favourites'?'selected':'not-selected'}" nowrap="true">
        <c:choose>
          <c:when test="${!loggedin}">
            <span onclick="alert('You need to log in to save favourites'); return false;">
              <fmt:message key="mymine.favourites.tab.title"/>
              <img src="images/star_active.gif" title="Favourites">
            </span>
          </c:when>
          <c:when test="${MYMINE_PAGE=='favourites' || !loggedin}">
            <fmt:message key="mymine.favourites.tab.title"/>&nbsp;<img src="images/star_active.gif" title="Favourites">
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=favourites">
              <fmt:message key="mymine.favourites.tab.title"/>&nbsp;<img src="images/star_active.gif" title="Favourites">
            </html:link>
          </c:otherwise>
        </c:choose>
       <im:manualLink section="manualMyMineTabs.shtml#manualFavTemplates"/>
       </td>

      <c:if test="${loggedin}">
        <td width="1%" class="tab ${MYMINE_PAGE=='password'?'selected':'not-selected'}" 
            nowrap="true">
          <c:choose>
            <c:when test="${MYMINE_PAGE=='password'}">
              <fmt:message key="mymine.password.tab.title"/>
            </c:when>
            <c:otherwise>
              <html:link action="/mymine?page=password">
                <fmt:message key="mymine.password.tab.title"/>
              </html:link>
            </c:otherwise>
          </c:choose>
        </td>
      </c:if>

      <td width="99%" class="tab-space" align="right" >
        &nbsp;
      </td>
    </tr>
  </table>

  <c:choose>
    <c:when test="${MYMINE_PAGE=='bags'||MYMINE_PAGE==null}">
      <tiles:insert name="bag.jsp"/>
    </c:when>
    <c:when test="${MYMINE_PAGE=='saved'}">
      <tiles:insert name="historyQueryView.jsp">
        <tiles:put name="type" value="saved"/>
      </tiles:insert>
    </c:when>
    <c:when test="${MYMINE_PAGE=='templates'}">
      <tiles:insert name="historyTemplateView.jsp">
        <tiles:put name="type" value="template"/>
      </tiles:insert>
    </c:when>
    <c:when test="${MYMINE_PAGE=='favourites'}">
      <tiles:insert name="favourites.tile" />
    </c:when>
    <c:when test="${MYMINE_PAGE=='password'}">
      <tiles:insert name="changePassword.jsp" />
    </c:when>
  </c:choose>

  <c:if test="${empty PROFILE.username}">
    <div class="body">
      <html:link action="/login"><fmt:message key="menu.login"/>...</html:link>&nbsp;&nbsp;
      <fmt:message key="mymine.login.help"/>
    </div>
  </c:if>
  
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


<!-- /mymine.jsp -->
