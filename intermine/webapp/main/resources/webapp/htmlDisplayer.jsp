<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- htmlDisplayer.jsp -->
<iframe src="<html:rewrite action='/getAttributeAsFile?object=${object.id}&field=alignment&type=html'/>" width="600" height="350" frameborder="0">
</iframe>
<!-- /htmlDisplayer.jsp -->
