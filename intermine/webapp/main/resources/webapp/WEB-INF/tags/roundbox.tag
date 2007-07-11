<%@ tag body-content="scriptless"  %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="stylename" required="true" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%
String stylename = (String) jspContext.getAttribute("stylename");
if(new java.io.File(application.getRealPath("model")+"/"+stylename+"_L.gif").exists()) {
 	request.setAttribute("useImages","true");
}
%>

<c:if test="${!empty title}">
	<c:if test="${useImages != 'true'}"><c:set var="tableStyle" value="${tableStyle};border: 1px solid #888;" /></c:if>
	<table class="roundcornerbox" cellpadding="0" cellspacing="0">
	<c:choose>
	<c:when test="${useImages}">
	<tr>
	  <td width="20"><img src="model/${stylename}_L.gif" width="20" height="41" alt="Roundcorner L"></td>
	  <td width="100%" style="background-image: url('model/${stylename}_M.gif');"><h1><c:out value="${title}"/></h1></td>
	  <td width="20"><img src="model/${stylename}_R.gif" width="20" height="41" alt="Roundcorner R"></td>
	</tr>
	</c:when>
	<c:otherwise>
	<tr>
	  <td width="100%" style="background-color: #EEE;padding-left:20px;height:40px;"><h1 style="color:black;"><c:out value="${title}"/></h1></td>
	</tr>
	</c:otherwise>
	</c:choose>
	<tr>
	<c:if test="${useImages}"><c:set var="bodyStyle" value="boxcontent${stylename}"/></c:if>
	<td style="padding: 5px 15px 5px 15px;" class="${bodyStyle}" colspan="3"><jsp:doBody/></td>
	</tr>
	<c:if test="${useImages}">
	<tr valign="bottom">
	<td width="20"><img src="model/${stylename}_BL.gif" width="20" height="20" alt="Roundcorner BL"></td><td class="boxbottom${stylename}">&nbsp;</td><td width="20"><img src="model/${stylename}_BR.gif" width="20" height="20" alt="Roundcorner BR"></td>
    </tr>
    </c:if>
	</table>
</c:if>