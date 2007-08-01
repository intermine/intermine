<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- bag.jsp -->
<html:xhtml/>

<div class="body">
    <div id="leftCol"><tiles:insert name="bagBuild.tile"/></div>
    <div id="rightCol"><c:import url="bagView.jsp"/></div>
</div>
<!-- /bag.jsp -->
