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

	<div style="border-top: 9px solid white">

	
	<table cellpadding="5" cellspacing="0" border="0" class="histTabTable" width="100%">
	  <tr>
	    <td width="1%" class="topLeft" nowrap>
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	    </td>
			<td width="1%" class="tab ${(HISTORY_PAGE=='history'||HISTORY_PAGE == null)?'selected':'not-selected'}"
	      nowrap>
				<c:choose>
					<c:when test="${HISTORY_PAGE=='history'||HISTORY_PAGE == null}">
						History
					</c:when>
					<c:otherwise>
						<html:link action="/history?page=history">
							History
						</html:link>
					</c:otherwise>
				</c:choose>
			</td>
			<td width="1%" class="tab ${HISTORY_PAGE=='bags'?'selected':'not-selected'}"
	      nowrap>
				<c:choose>
					<c:when test="${HISTORY_PAGE=='bags'}">
						Saved Bags
					</c:when>
					<c:otherwise>
						<html:link action="/history?page=bags">
							Saved Bags
						</html:link>
					</c:otherwise>
				</c:choose>
			</td>
			<td width="1%" class="tab ${HISTORY_PAGE=='saved'?'selected':'not-selected'}"
	      nowrap>
				<c:choose>
					<c:when test="${HISTORY_PAGE=='saved'}">
						Saved Queries
					</c:when>
					<c:otherwise>
						<html:link action="/history?page=saved">
							Saved Queries
						</html:link>
					</c:otherwise>
				</c:choose>
			</td>
			<td width="1%" class="tab ${HISTORY_PAGE=='templates'?'selected':'not-selected'}"
	      nowrap>
				<c:choose>
					<c:when test="${HISTORY_PAGE=='templates'}">
						Saved Template Queries
					</c:when>
					<c:otherwise>
						<html:link action="/history?page=templates">
							Saved Template Queries
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
		<c:when test="${HISTORY_PAGE=='bags'}">
			<tiles:get name="historyBagView.jsp"/>
		</c:when>
		<c:when test="${HISTORY_PAGE=='saved'}">
			<tiles:insert name="historyQueryView.jsp">
        <tiles:put name="type" value="saved"/>
      </tiles:insert>
		</c:when>
		<c:when test="${HISTORY_PAGE=='history'||HISTORY_PAGE==null}">
			<tiles:insert name="historyQueryView.jsp">
	      <tiles:put name="type" value="history"/>
	    </tiles:insert>
		</c:when>
		<c:when test="${HISTORY_PAGE=='templates'}">
			<tiles:get name="historyTemplateView.jsp"/>
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

<!-- /history.jsp -->
