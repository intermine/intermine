<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- browse.jsp -->
<script type="text/javascript" src="js/browse.js"></script>
<script type="text/javascript">
function updateExample(value) {
	if(value=='ids') {
	   document.getElementById('quickSearchInput').value = '<fmt:bundle basename="model"><fmt:message key="model.quickSearch.example"/></fmt:bundle>';
	} else if (value=='tpls') {
	   document.getElementById('quickSearchInput').value = '<fmt:bundle basename="model"><fmt:message key="model.template.example"/></fmt:bundle>';
	} else if (value='bgs') {
	   document.getElementById('quickSearchInput').value = '<fmt:bundle basename="model"><fmt:message key="model.bag.example"/></fmt:bundle>';
	}
}
</script>
<tiles:importAttribute name="menuItem" ignore="true"/>
<html:form action="/browseAction" style="display:inline;">
  <fmt:message key="header.search.pre"/>
  <select name="quickSearchType" onchange="updateExample(this.value);" >
	<option value="ids" <c:if test="${quickSearchType=='ids'}">selected</c:if>>Identifiers</option>
	<option value="tpls" <c:if test="${quickSearchType=='tpls'}">selected</c:if>>Templates</option>
	<option value="bgs" <c:if test="${quickSearchType=='bgs'}">selected</c:if>>Lists</option>
  </select>
<fmt:message key="header.search.mid"/>
<input style="color:#666; font-style:italic;" type="text" id="quickSearchInput" name="value" size="20" value="<fmt:bundle basename="model"><fmt:message key="model.quickSearch.example"/></fmt:bundle>" onFocus="clearElement(this);">  
<html:submit><fmt:message key="header.search.button"/></html:submit>
<script type="text/javascript">
  updateExample('${quickSearchType}');
</script>
  
</html:form>
<!-- /browse.jsp -->