<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- bagDisplayers.jsp -->

<html:xhtml/>
<tiles:importAttribute name="bag"/>
<c:forEach items="${bag.classDescriptors}" var="cld">
	<c:if test="${fn:length(WEBCONFIG.types[cld.name].bagDisplayers) > 0}">
		<div id="linkOuts" class="listtoolbox" align="left">
		<h3>Link outs</h3>
		<p>  
		    <c:forEach items="${WEBCONFIG.types[cld.name].bagDisplayers}" var="displayer">
		      <c:set var="bag" value="${bag}" scope="request"/>
		      <tiles:insert beanName="displayer" beanProperty="src"/><br/>
		    </c:forEach>
		</p>
		</div>
	</c:if>
</c:forEach>

<!-- /bagDisplayers.jsp -->

