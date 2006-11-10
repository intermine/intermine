<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- chromosomeLocDisplayer.jsp -->

<html:xhtml/>
<b><c:out value="${object.object.chromosome.identifier}:${object.object.chromosomeLocation.start}-${object.object.chromosomeLocation.end}" /></b>

<!-- /chromosomeLocDisplayer.jsp -->