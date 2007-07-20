<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- browse.jsp -->
<script type="text/javascript" src="js/browse.js"></script>
<tiles:importAttribute name="menuItem" ignore="true"/>
<html:form action="/browseAction" style="display:inline;">
  <fmt:message key="header.search.pre"/>
  <select name="quickSearchType">
	<option value="ids">Identifiers</option>
	<option value="tpls">Templates</option>
	<option value="bgs">Lists</option>
  </select>  
<fmt:message key="header.search.mid"/>  
<input style="color:#666; font-style:italic;" type="text" name="value" size="20" value="<fmt:bundle basename="model"><fmt:message key="model.quickSearch.example"/></fmt:bundle>" onFocus="clearElement(this);">  
<html:submit><fmt:message key="header.search.button"/></html:submit>
  
</html:form>
<!-- /browse.jsp -->