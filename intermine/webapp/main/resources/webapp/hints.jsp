<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>


<tiles:importAttribute name="pageName" />

<!-- hints.jsp -->
<c:if test="${!empty hint}">
	<div id="hints" class="information"><a href="#"
		onclick="javascript:jQuery('#hints').hide('slow');return false">Hide</a>
	<p><c:out value="${hint}" /></p>
	</div>
	<script type="text/javascript">
		if (jQuery('div.topBar:visible').exists()) {
			jQuery('#hints.information').hide();
		}
	</script>
<!--	
	<div id="hintsDiv">
	  <div class="topBar hints"> <%-- IE table width bug --%>
	    <table width="100%" cellspacing="0" border="0" cellpadding="0">
	    <tr>
	      <td valign="bottom" width="30px"><img border="0" align="middle" src="images/tick.png" height="20px" width="20px" title="hint"/></td>
	      <td valign="middle">
	        <c:out value="${hint}"/>
	        <%--<c:out value="PAGE: ${pageName}"/>--%>
	      </td>
	      <td align="right" valign="top">
	          <a href="#" onclick="javascript:jQuery('#hintsDiv').hide('slow');return false">
	            Hide
	          </a>
	      </td>
	    </tr>
	    </table>
	  </div>
	  <br/>
	</div>
-->	
</c:if>

<!-- /hints.jsp -->
