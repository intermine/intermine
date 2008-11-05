<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- dateDisplayer.jsp -->
<tiles:importAttribute name="expr" ignore="false"/>
<html:xhtml/>
<im:eval evalExpression="interMineObject.${expr}" evalVariable="outVal"/>
<im:dateDisplay date="${outVal}" type="longDate"/>
<!-- /dateDisplayer.jsp -->