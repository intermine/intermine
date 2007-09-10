<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- browse.jsp -->
<c:set var="ids">
  <fmt:bundle basename="model"><fmt:message key="model.template.example"/></fmt:bundle>
</c:set>
<c:set var="tpls">
  <fmt:bundle basename="model"><fmt:message key="model.bag.example"/></fmt:bundle>
</c:set>
<c:set var="bgs">
  <fmt:bundle basename="model"><fmt:message key="model.quickSearch.example"/></fmt:bundle>
</c:set>

<script type="text/javascript" src="js/browse.js"></script>
<script type="text/javascript">
function updateExample(i) {
	if (i==1) {
	   document.getElementById('quickSearchInput').value = '<c:out value="${ids}"/>';	   
	} else if (i==2) {
	   document.getElementById('quickSearchInput').value = '<c:out value="${tpls}"/>';
	} else {
	   document.getElementById('quickSearchInput').value = '<c:out value="${bgs}"/>';
	}
	document.getElementById('quickSearchInput').style.color = '#666';
	document.getElementById('quickSearchInput').style.fontStyle = 'italic';
}
function clearElement(e) {
   var value =document.getElementById('quickSearchInput').value;
   if( value == '<c:out value="${ids}"/>'
   || value == '<c:out value="${tpls}"/>'
   || value == '<c:out value="${bgs}"/>') {
	e.value = "";
	document.getElementById('quickSearchInput').style.color = '#000';
	document.getElementById('quickSearchInput').style.fontStyle = 'normal';
   }
}
</script>
<tiles:importAttribute name="menuItem" ignore="true"/>
<html:form action="/browseAction" style="display:inline;">
  <fmt:message key="header.search.pre"/>
  <select name="quickSearchType" id="quickSearchType" onchange="updateExample(selectedIndex);" style="font-size:1em;">
	<option value="ids" selected>Identifiers</option>
	<option value="tpls" <c:if test="${quickSearchType=='tpls'}">selected</c:if>>Templates&nbsp;&nbsp;</option>
	<option value="bgs" <c:if test="${quickSearchType=='bgs'}">selected</c:if>>Lists</option>
  </select>
<fmt:message key="header.search.mid"/>
<input style="width:260px;color:#666;font-style:italic;font-size:1em" type="text" id="quickSearchInput" name="value" value="<fmt:bundle basename="model"><fmt:message key="model.quickSearch.example"/></fmt:bundle>" onFocus="clearElement(this);" />  
<html:submit><fmt:message key="header.search.button"/></html:submit>

</html:form>
<script language="javascript">
  window.onload = document.getElementById('quickSearchType').selectedIndex = 0;
  window.onload = updateExample($('quickSearchType').selectedIndex);
</script>
<!-- /browse.jsp -->