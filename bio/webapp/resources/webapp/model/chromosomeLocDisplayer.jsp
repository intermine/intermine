<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- chromosomeLocDisplayer.jsp -->

<html:xhtml/>
<b><c:out value="${interMineObject.chromosome.identifier}: ${interMineObject.chromosomeLocation.start}-${interMineObject.chromosomeLocation.end}" /></b>
<!-- /chromosomeLocDisplayer.jsp -->
