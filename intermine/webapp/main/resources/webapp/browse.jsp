<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- browse.jsp -->
<script type="text/javascript" src="js/browse.js"></script>
<script type="text/javascript">
function updateExample(i) {
	if (i==1) {
	   document.getElementById('quickSearchInput').value = '<fmt:bundle basename="model"><fmt:message key="model.template.example"/></fmt:bundle>';	   
	} else if (i==2) {
	   document.getElementById('quickSearchInput').value = '<fmt:bundle basename="model"><fmt:message key="model.bag.example"/></fmt:bundle>';
	} else {
	   document.getElementById('quickSearchInput').value = '<fmt:bundle basename="model"><fmt:message key="model.quickSearch.example"/></fmt:bundle>';
	}
	document.getElementById('quickSearchInput').className = 'quicksearch';
}
</script>
<tiles:importAttribute name="menuItem" ignore="true"/>
<html:form action="/browseAction" style="display:inline;">
  <fmt:message key="header.search.pre"/>
  <select name="quickSearchType" onchange="updateExample(selectedIndex);">
	<option value="ids" selected>Identifiers</option>
	<option value="tpls" <c:if test="${quickSearchType=='tpls'}">selected</c:if>>Templates&nbsp;&nbsp;</option>
	<option value="bgs" <c:if test="${quickSearchType=='bgs'}">selected</c:if>>Lists</option>
  </select>
<fmt:message key="header.search.mid"/>
<input class="quicksearch"  type="text" id="quickSearchInput" name="value" size="20" value="<fmt:bundle basename="model"><fmt:message key="model.quickSearch.example"/></fmt:bundle>" onFocus="clearElement(this);" />  
<html:submit><fmt:message key="header.search.button"/></html:submit>

</html:form>
<!-- /browse.jsp -->