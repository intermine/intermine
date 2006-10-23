<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- mymine.jsp -->

  <script type="text/javascript">
    <!--//<![CDATA[
      function selectColumnCheckbox(form, type) {
        var columnCheckBox = 'selected_' + type;
        var testString = columnCheckBox + '_';
        var checked = document.getElementById(columnCheckBox).checked;
        document.getElementById('delete_button').disabled = !checked;
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
        document.getElementById('delete_button').disabled = deleteDisable;
        document.getElementById('export_button').disabled = deleteDisable;
        document.getElementById('selected_' + type).checked = columnCheckBoxChecked;
        return true;
      }
      //]]>-->
  </script>

  <div style="border-top: 9px solid white">

  
  <table cellpadding="5" cellspacing="0" border="0" class="histTabTable" width="100%">
    <tr>
      <td width="1%" class="topLeft" nowrap>
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
      </td>
      <td width="1%" class="tab ${(MYMINE_PAGE=='history'||MYMINE_PAGE == null)?'selected':'not-selected'}"
        nowrap>
        <c:choose>
          <c:when test="${MYMINE_PAGE=='history'||MYMINE_PAGE == null}">
            Query History
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=history">
              Query History
            </html:link>
          </c:otherwise>
        </c:choose>
      </td>
      <td width="1%" class="tab ${MYMINE_PAGE=='saved'?'selected':'not-selected'}"
        nowrap>
        <c:choose>
          <c:when test="${MYMINE_PAGE=='saved'}">
            Saved Queries
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=saved">
              Saved Queries
            </html:link>
          </c:otherwise>
        </c:choose>
      </td>
      <td width="1%" class="tab ${MYMINE_PAGE=='templates'?'selected':'not-selected'}"
        nowrap>
        <c:choose>
          <c:when test="${MYMINE_PAGE=='templates'}">
            Saved Templates
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=templates">
              Saved Templates
            </html:link>
          </c:otherwise>
        </c:choose>
      </td>
      <td width="1%" class="tab ${MYMINE_PAGE=='favourites'?'selected':'not-selected'}"
        nowrap>
        <c:choose>
          <c:when test="${MYMINE_PAGE=='favourites'}">
            Favourite Templates&nbsp;<img src="images/star_active.gif" title="Favourites">
          </c:when>
          <c:otherwise>
            <html:link action="/mymine?page=favourites">
              Favourite Templates&nbsp;<img src="images/star_active.gif" title="Favourites">
            </html:link>
          </c:otherwise>
        </c:choose>
      </td>
      <td width="99%" class="tab-space" align="right" >
        &nbsp;
      </td>
    </tr>
  </table>

  <c:choose>
    <c:when test="${MYMINE_PAGE=='saved'}">
      <tiles:insert name="historyQueryView.jsp">
        <tiles:put name="type" value="saved"/>
      </tiles:insert>
    </c:when>
    <c:when test="${MYMINE_PAGE=='history'||MYMINE_PAGE==null}">
      <tiles:insert name="historyQueryView.jsp">
        <tiles:put name="type" value="history"/>
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
  </c:choose>

  <c:if test="${empty PROFILE.username}">
    <div class="body">
      <html:link action="/login"><fmt:message key="menu.login"/>...</html:link>
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

<!-- /mymine.jsp -->
